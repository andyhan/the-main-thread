#!/usr/bin/env python3
"""Extract Mermaid blocks from Markdown and render them with Mermaid CLI."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


FENCE_RE = re.compile(r"^```(\w+)?\s*$")
MACOS_BROWSER_PATHS = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
]


def slugify(text: str) -> str:
    text = text.strip().lower()
    text = re.sub(r"[^a-z0-9]+", "-", text)
    text = re.sub(r"-+", "-", text).strip("-")
    return text or "article"


def infer_slug(markdown: str, fallback: str) -> str:
    for line in markdown.splitlines():
        if line.startswith("# "):
            return slugify(line[2:])
    return slugify(Path(fallback).stem)


def extract_mermaid_blocks(markdown: str) -> list[str]:
    blocks: list[str] = []
    in_mermaid = False
    in_other_fence = False
    current: list[str] = []

    for line in markdown.splitlines():
        match = FENCE_RE.match(line)
        if match:
            lang = (match.group(1) or "").lower()
            if in_mermaid:
                blocks.append("\n".join(current).strip() + "\n")
                current = []
                in_mermaid = False
                continue
            if in_other_fence:
                in_other_fence = False
                continue
            if lang == "mermaid":
                in_mermaid = True
                current = []
            else:
                in_other_fence = True
            continue

        if in_mermaid:
            current.append(line)

    if in_mermaid:
        raise SystemExit("Unclosed mermaid code fence")

    return blocks


def render_with_mmdc(
    mmdc: str,
    source: Path,
    target: Path,
    background: str,
    width: int | None,
    height: int | None,
) -> None:
    cmd = [mmdc, "-i", str(source), "-o", str(target), "-b", background]
    if width:
        cmd.extend(["--width", str(width)])
    if height:
        cmd.extend(["--height", str(height)])
    env = os.environ.copy()
    if "PUPPETEER_EXECUTABLE_PATH" not in env:
        for browser_path in MACOS_BROWSER_PATHS:
            if Path(browser_path).exists():
                env["PUPPETEER_EXECUTABLE_PATH"] = browser_path
                break
    subprocess.run(cmd, check=True, env=env)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Extract fenced mermaid blocks from Markdown and render PNGs with mmdc."
    )
    parser.add_argument("article", help="Markdown article path")
    parser.add_argument("--out", help="Output diagram directory")
    parser.add_argument("--slug", help="Filename slug. Defaults to H1 title")
    parser.add_argument("--extract-only", action="store_true", help="Only write .mmd files")
    parser.add_argument("--background", default="white", help="mmdc background color")
    parser.add_argument("--width", type=int, default=None, help="Optional render width")
    parser.add_argument("--height", type=int, default=None, help="Optional render height")
    parser.add_argument("--mmdc", default=None, help="Path to Mermaid CLI mmdc")
    args = parser.parse_args()

    article = Path(args.article)
    markdown = article.read_text(encoding="utf-8")
    slug = slugify(args.slug) if args.slug else infer_slug(markdown, article.name)
    out_dir = Path(args.out) if args.out else Path("publishing") / slug / "diagrams"
    out_dir.mkdir(parents=True, exist_ok=True)
    stale_manifest = out_dir / "diagrams-manifest.md"
    if stale_manifest.exists():
        stale_manifest.unlink()
        print(f"Removed stale {stale_manifest}")

    blocks = extract_mermaid_blocks(markdown)
    if not blocks:
        print("No Mermaid blocks found.")
        return 0

    sources: list[Path] = []
    for idx, block in enumerate(blocks, start=1):
        source = out_dir / f"{slug}-{idx:02d}.mmd"
        source.write_text(block, encoding="utf-8")
        sources.append(source)
        print(f"Wrote {source}")

    if args.extract_only:
        print("Extract-only mode; PNG rendering skipped.")
        return 0

    mmdc = args.mmdc or shutil.which("mmdc")
    if not mmdc:
        print(
            "Mermaid CLI 'mmdc' not found. Install @mermaid-js/mermaid-cli or pass --mmdc.",
            file=sys.stderr,
        )
        return 2

    for source in sources:
        target = source.with_suffix(".png")
        render_with_mmdc(mmdc, source, target, args.background, args.width, args.height)
        if not target.exists() or target.stat().st_size == 0:
            raise SystemExit(f"Render failed or produced empty file: {target}")
        print(f"Rendered {target}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
