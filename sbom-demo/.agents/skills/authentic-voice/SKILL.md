---
name: authentic-voice
description: >-
  Apply The Main Thread voice to tutorials, articles, explanations, and technical
  marketing copy: first-person personal technical narration, direct peer-to-peer
  engineering prose, fluent German/non-native rhythm, smaller plain vocabulary,
  concrete stakes, occasional dry humor, and no generic AI polish. Use when
  drafting, rewriting, or reviewing prose for tone; when a user asks for "my
  voice", "make this sound less AI", "make it sound like The Main Thread", or
  "run a voice pass"; and when a draft needs less documentation smell, less
  repetition, or a negation-budget pass. This skill controls wording only. Pair
  with main-thread-tutorial-structure for invisible story scaffolding,
  verification, and code standards, and with article-reviewer for critique.
---

# Authentic voice

## Instructions

1. Use this skill for tone and phrasing only. Do not change technical meaning, section requirements, code, commands, or verification unless the user also asks for those changes.
2. Read [reference.md](reference.md) before revising a full draft or making a substantial voice pass. For a small paragraph edit, use the quick rules below and open the reference only when the choice is unclear.
3. Write like a senior German engineer on his own blog: personal, practical, occasionally dry, careful with claims, and more interested in consequences than polish.
4. Use first person normally for preferences, experience, and judgment. Use `we` for walkthrough steps. Use `you` for consequences in the reader's system. Do not invent war stories.
5. Prefer anti-duplication over repeating the same comfortable phrase in every section. If a failure mode, guarantee, or config explanation already has a home in the structure, point back briefly instead of restating it.
6. Avoid documentation smell: neutral filler, class-by-class sameness, announce-y bridges, and paragraphs that sound like generated API docs.
7. Avoid repeated opening smell. Do not let many articles start with `Most teams still...`, `Most developers think...`, or the same `not X but Y` contrast frame.
8. Remove authoring-process leakage: no mentions of drafts, companion files, maintained markdown, repo walkthrough changes, or "this was updated from the original" unless the article is explicitly an update note or the user asks for that context.
9. Gloss load-bearing jargon once at first real use. Keep precise technical terms when precision matters.
10. For full drafts, apply the negation budget from [reference.md](reference.md). Negative-led and antithetical sentences are fine when they are occasional or necessary for safety, CLI semantics, criticism, or literal constraints. They must not become the article's main rhythm.
11. Conflict order: explicit user request, then main-thread-tutorial-structure for story shape and verification, then article-reviewer when reviewing, then this skill for voice.

## Quick Rules

- Use simple glue words: `use`, `show`, `change`, `set`, `problem`, `keep`, `stop`, `real`, `extra`, `below`, `after that`.
- Do not cycle synonyms just to sound varied.
- Keep direct teaching questions only when the answer immediately follows.
- Add personality through first-person judgment, concrete friction, short asides, visible trade-offs, and occasional dry humor. Do not add fake stories.
- Preserve necessary warnings. Remove rhetorical negation when it is only there to sound decisive.
- Keep the prose globally readable, but do not invent grammar mistakes or caricature a non-native speaker.
- Use O'Reilly-style mechanics for cleanup only. Do not let it turn the prose into book documentation.
- The reader should never see the authoring process. Replace meta lines about drafts, companion revisions, source-control tracking, or API-alignment chores with article-native context.
- Vary article openings. Start from a concrete artifact, dry observation, personal preference, failure in motion, or reader consequence instead of always using `Most teams still...`.

## Do not

- Introduce grammar mistakes or caricatured “non-native” English on purpose.
- Replace precise technical terms with vague casual words when precision matters.

The full rules, examples, and self-check list are in [reference.md](reference.md).
