---
name: main-thread-tutorial-structure
description: >-
  Define invisible story scaffolding for The Main Thread hands-on tutorials:
  why the problem matters, what gets built, what readers need, how the code
  works, how it fails, how to configure it, how to prove it, and how to close
  the loop. Use when planning, drafting, rewriting, or reviewing a full tutorial,
  quick guide, architecture article, or code-heavy explanation that needs
  production framing, complete code, verification commands, and a contained
  narrative. Use O'Reilly mechanics for cleanup only, not as the prose voice.
  Pair with authentic-voice for personal technical narration and article-reviewer
  for structured critique. Verify version-sensitive facts with current docs or
  tools rather than memory.
---

# Main Thread tutorial structure

## Instructions

1. Use this skill for story shape, completeness, verification, and production framing. Do not let the structure leak into the prose as checklist language or documentation filler.
2. Use **authentic-voice** for all connective prose when that skill is available. This skill decides what the article must contain; authentic-voice decides how it sounds.
3. Read [reference.md](reference.md) before writing or restructuring a full tutorial. For quick checks, use the story beats below and open only the relevant reference section.
4. Apply the **Information budget**: each major risk, guarantee, or failure mode gets one primary explanation. Later sections should point back or add only new detail.
5. Vary the opening. Keep the problem front and center, but do not default to `Most teams still...`, `Most developers think...`, or the same contrast frame across articles.
6. Verify version-sensitive facts with current official docs, local repo docs, or available documentation tools. Do not rely on memory for Quarkus versions, Java versions, extension names, CLI syntax, dependency coordinates, APIs, model support, pricing, limits, or security guidance.
7. Make articles standalone. Do not include authoring-process scaffolding, companion-file explanations, internal repo maintenance notes, or changelog sections unless the user explicitly requests an update note.
8. Use O'Reilly-style mechanics for cleanup: headings, links, code formatting, list hygiene, punctuation, and similar copyediting details. Do not make the article sound like an O'Reilly chapter.
9. For The Main Thread and Substack-bound drafts, do not use Markdown tables. Use numbered lists, bullets, definition-style lines (`**Term** - detail`), or short paragraphs.
10. End with the canonical code location when a repository name is known: `https://github.com/myfear/the-main-thread/<repository-name>`.
11. After generated project setup, avoid generic dependency alignment prose. State the Quarkus version once, list extension roles, and mention separately pinned Quarkiverse or third-party versions in the extension list or an exact snippet.
12. Keep setup prose on a budget. Do not narrate what the command already says, such as obvious group/package-coordinate consistency, unless it prevents a real copy-paste trap.

## Story beats (tutorial mode)

Use these beats to keep the article contained. Headings can be conventional, but the prose must not expose the checklist.

1. **Why I care** - Production reality vs naive mental model. Start with the pain, not a table of contents.
2. **What we build** - The concrete system or slice readers will have at the end.
3. **What you need** - One paragraph plus a short prerequisite list.
4. **Build the base** - One project setup command plus extension/dependency reasons.
5. **Make it work** - Components with context, complete code, and human explanation.
6. **Configure it** - Properties and environment choices, with what breaks when they are wrong.
7. **Make it survive** - The real production risks this article creates, not an arbitrary category checklist.
8. **Prove it** - Runnable commands/tests and expected output.
9. **Close the loop** - What the reader built, how it answers the opening problem, and where the code lives.

## Non-negotiables (summary)

- **No Markdown tables** in Main Thread-bound drafts (use lists or `**bold lead** — detail` lines; see instruction 5 above and **Content standards** in [reference.md](reference.md)).
- Compilable code, all imports, no placeholders, internal consistency across the article.
- Production-appropriate config values; explicit limits and failure modes.
- O'Reilly heading/list/code conventions as copyediting mechanics only.
- No visible template language: `This section covers`, `The key insight is`, `Production hardening considerations include`, `What this guarantees`, or similar generated scaffolding.
- No authoring-process leakage. The article is a standalone piece unless the user explicitly asks for release notes or an update note.
- Use the canonical The Main Thread code URL pattern at the end when possible: `https://github.com/myfear/the-main-thread/<repository-name>`.
- No generic "align the generated POM/BOM/plugin" filler after `quarkus create app`. Show exact manual dependency edits only when they are necessary.
- No setup filler that only restates command flags. Explain traps, choices, and manual edits; let obvious flags be obvious.

Full detail, examples, checklists, output modes, and validation criteria are in [reference.md](reference.md).
