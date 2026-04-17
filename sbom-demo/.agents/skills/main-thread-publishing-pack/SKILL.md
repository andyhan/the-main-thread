---
name: main-thread-publishing-pack
description: >-
  Create publishing assets for finished The Main Thread articles after structure
  and voice are done. Use when preparing a Substack/blog publishing package,
  rendering Mermaid diagrams to copy-pasteable PNGs, writing alt text for
  diagrams, researching and generating SEO title/subtitle/url slug/tags,
  creating persona-based Substack title/subtitle variants for A/B testing,
  drafting social posts for Mastodon, X, Bluesky, and LinkedIn, or creating a
  1200x630 hero image prompt aligned to the article. Do not rewrite article
  prose unless explicitly asked.
---

# Main Thread Publishing Pack

## Scope

Use this skill after the article is finished or close to finished. It creates publishing assets next to the article. It must not insert publishing metadata, social copy, image prompts, or diagram export notes into the article body.

Default output folder:

```text
publishing/<article-slug>/
```

Default pack file:

```text
publishing/<article-slug>/publishing-pack.md
```

## Workflow

1. Read the article and infer a stable slug from the title unless the user gives one.
2. Render Mermaid diagrams with `scripts/render_mermaid_blocks.py`.
3. Write useful alt text for every rendered diagram. Describe what the diagram communicates, not every node mechanically.
4. Run the SEO research pass for Substack metadata.
5. Create Substack metadata:
   - primary SEO-compliant title and subtitle
   - two additional persona-based title/subtitle variants for Substack A/B testing
   - URL slug
   - five high-impact tags
6. Create platform-specific social copy:
   - Mastodon
   - X
   - Bluesky
   - LinkedIn
7. Create a hero image prompt for a **1200x630** image.
8. Write the publishing pack as Markdown.

## Mermaid Diagrams

Use the bundled script:

```bash
python3 .agents/skills/main-thread-publishing-pack/scripts/render_mermaid_blocks.py path/to/article.md --out publishing/<slug>/diagrams --slug <slug>
```

Run this from the project repository root (the directory that contains `.agents/`). From the skill folder you can instead run `python3 scripts/render_mermaid_blocks.py` with the same arguments.

The script extracts fenced `mermaid` blocks into `.mmd` files and renders `.png` files with Mermaid CLI (`mmdc`) when available.
On macOS, the script auto-detects common installed browser paths for Puppeteer, including Google Chrome, Chromium, and Microsoft Edge. If rendering still fails with a browser error, set `PUPPETEER_EXECUTABLE_PATH` to the browser executable before running the script.

Rules:

- Do not silently skip diagrams.
- If `mmdc` is missing, report that Mermaid CLI is required and keep the extracted `.mmd` files.
- Use stable filenames: `<slug>-01.mmd`, `<slug>-01.png`, and so on.
- Do not create a separate `diagrams-manifest.md`.
- Put every diagram's PNG path, source path, title, and alt text directly into `publishing-pack.md`.

Mermaid alt text should answer:

- What system or flow does this diagram explain?
- What are the most important steps or relationships?
- What should the reader learn from it?

Good alt text:

> Diagram showing the RAG ingest and query paths. Source documents are converted with Docling, split into text segments, embedded, and stored in pgvector; user questions use the same embedding model before nearest-vector retrieval feeds the chat model.

Bad alt text:

> Flowchart with boxes and arrows.

## Substack Metadata

Run a lightweight SEO research pass before generating title, subtitle, URL slug, and tags.

Research goals:

- Identify the primary reader intent in plain language: what would the right reader search for?
- Extract three to five candidate search phrases from the article's real topic, technologies, and problem.
- Check current official guidance when web access is available. Prefer Google Search Central for title/snippet/URL guidance and Substack Help for platform constraints.
- Check whether the chosen terms are real ecosystem language, not invented marketing phrasing.
- Keep the metadata useful for humans first. Search engines are not the reader; they are the doorman with a clipboard.

Useful baseline guidance:

- Google title guidance: use unique, descriptive, concise titles; avoid keyword stuffing and boilerplate.
- Google description guidance: use page-specific, human-readable summaries; avoid keyword lists.
- Google URL guidance: use readable words and hyphens.
- Substack URL constraint: post slugs use lowercase letters, numbers, and dashes, with no spaces.
- Substack tags create tag pages and navigation surfaces; treat them as reusable taxonomy, not disposable hashtags.

Generate:

- **Primary title**: clear, searchable, and human. Avoid clickbait and generic hype. Target senior developers and development leaders.
- **Primary subtitle**: one sentence that states the practical value for senior developers and development leaders.
- **Two persona title/subtitle variants** for Substack A/B testing:
  - **Starting developers**: more approachable, concrete, and learning-oriented without becoming childish.
  - **IT decision-makers**: outcome-oriented, practical, and risk-aware without turning into enterprise wallpaper.
- **URL slug**: lowercase, hyphenated, short, and stable.
- **Tags**: five high-impact tags. Prefer concrete technologies and topic categories.

Optimization checks:

- **Primary title**: include the main search phrase or its strongest terms naturally, preferably near the front. Mention the most important concrete technology only if it helps the reader choose the article. Do not repeat terms.
- **Primary subtitle**: summarize the specific payoff and supporting technologies. It should work as a search/social preview sentence, not as a second headline stuffed with nouns.
- **Persona variants**: keep the same article promise, but change the relevance frame:
  - Starting developers need orientation, confidence, and a clear learning path.
  - IT decision-makers need business/operational relevance, architectural confidence, and risk reduction.
  - Do not make the variants misleading, broader than the article, or stuffed with new keywords.
- **URL slug**: 2-48 characters for Substack; lowercase ASCII letters, numbers, and dashes only. Use the shortest stable phrase that still names the topic.
- **Tags**: prefer a mix of core technologies and durable topic categories. Use tags the publication can reuse across multiple posts. Avoid one-off long phrases.

Avoid titles like:

- `Unlock the Power of...`
- `A Comprehensive Guide to...`
- `Revolutionizing...`

Prefer titles that sound like The Main Thread:

- direct
- technical
- specific
- a little opinionated when useful

## Persona-Based Title Testing

Include two additional title/subtitle combinations after the primary Substack metadata.

Purpose:

- Test relevance by audience segment, not just raw click-through.
- Learn which framing works for each group while keeping the article honest.
- Keep every variant grounded in the same article topic and technical scope.

Personas:

- **Primary**: senior developers and development leaders. Use precise technical framing and architectural payoff.
- **Starting developers**: explain what they can build and learn. Prefer concrete language over abstractions.
- **IT decision-makers**: focus on system shape, operational readiness, risk, and practical adoption. Avoid procurement theatre. Nobody needs more of that.

## Social Copy

Create separate copy for each platform. Do not use the same text everywhere.

- Use the published article URL, not the source repository URL: `https://www.the-main-thread.com/p/<URL slug>`.
- Include relevant hashtags for each platform. Prefer concrete technology and topic hashtags from the article; avoid generic reach bait.
- Keep hashtags natural to the platform and proportionate to the post.
- **X**: conversation starter. Use an opinionated hook, fast takeaway, and one strong angle.
- **Bluesky**: peer exchange. Make it personal, thoughtful, curious, and builder-to-builder.
- **Mastodon**: technical trust. Be technical, sincere, low-hype, and community-respectful.
- **LinkedIn**: professional relevance. Be practical, credible, and career- or business-relevant without becoming LinkedIn cringe.

All social copy should point to the article's real technical problem, not generic "new post" filler.

## Hero Image Prompt

Create one prompt for a **1200x630** hero image.

Include:

- concept
- visual metaphor
- style
- mood
- composition
- article-specific technical cues
- what to avoid

Avoid generic cyberpunk server rooms, random glowing brains, vague AI magic, and text baked into the image unless the user asks for it.

Example shape:

```markdown
## Hero Image Prompt

Create a 1200x630 editorial hero image for an article about ...

Visual concept: ...
Style: ...
Composition: ...
Mood: ...
Technical cues: ...
Avoid: ...
```

## Publishing Pack Format

Write `publishing-pack.md` like this:

```markdown
# Publishing Pack

## Substack

**Primary title:** ...

**Primary subtitle:** ...

### Persona Title Tests

**Starting developers title:** ...

**Starting developers subtitle:** ...

**IT decision-makers title:** ...

**IT decision-makers subtitle:** ...

**URL slug:** ...

**Tags:**
- ...

## SEO Research Snapshot

**Primary reader intent:** ...

**Candidate search phrases:**
- ...

**Metadata rationale:**
- **Primary title:** ...
- **Primary subtitle:** ...
- **Starting developers variant:** ...
- **IT decision-makers variant:** ...
- **URL slug:** ...
- **Tags:** ...

**Sources checked:** ...

## Hero Image Prompt

...

## Diagrams

### Diagram 1: ...

**PNG:** `diagrams/<file>.png`

**Source:** `diagrams/<file>.mmd`

**Alt text:** ...

## Social Copy

### Mastodon
...

### X
...

### Bluesky
...

### LinkedIn
...
```

## Boundaries

- Do not modify the article unless the user explicitly asks.
- Do not add publishing notes to the article body.
- Do not invent claims not supported by the article.
- Do not generate a hero image automatically unless the user asks; produce the prompt.
- Do not claim Mermaid PNG rendering succeeded unless the files exist.
