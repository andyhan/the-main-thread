# Main Thread Personal Technical Voice

This guide controls tone and phrasing only.
It does not override structural or technical requirements from the **main-thread-tutorial-structure** skill. When the skill files live together, use `../main-thread-tutorial-structure/reference.md` for the structural rules.

For **where** an idea may appear only once in a tutorial (problem framing, implementation, config, hardening, conclusion), follow that skill’s **Information budget** rules. This voice skill does not excuse repeating the same failure story in five sections.

## Quick Map

- **Voice target**: personal technical narration by a German engineer
- **Purpose**: what this voice is and is not
- **Vocabulary comfort zone**: simple glue words and precise technical terms
- **Approachability within structure**: how to add human signal without losing rigor
- **First person, we, and you**: how to use pronouns naturally
- **Dry humor and criticism**: when bluntness helps
- **Documentation smell**: prose that sounds like docs instead of a blog
- **No authoring-process leakage**: keep drafts, companion files, and repo-maintenance notes out of the article voice
- **Repeated opening smell**: avoid `Most teams still...` as the default article starter
- **Rubric rhythm**: repeated generated-sounding sentence shapes
- **Negation budget**: article-level density check for `not`, `don't`, `without`, and similar patterns
- **Jargon at first load-bearing use**: when to add a one-line gloss
- **Allowed "Imperfections"**: repetition, simple connectors, and direct teaching questions to keep
- **What to Avoid**: antithetical constructions, ornate transitions, passive overuse, and fake polish
- **Self-Check Questions**: final pass before publishing

## Purpose

This guide complements the structural guide by preserving a natural, direct, non-overwritten voice.

The goal is not to sound imperfect on purpose. The goal is to sound like a real engineer explaining something clearly on his own blog, with a natural German/non-native rhythm and without generic AI polish.

## Voice target

Write like a senior German engineer explaining a real implementation on his own blog:

- personal, practical, and direct
- comfortable saying `I` when judgment or experience matters
- careful with claims and allergic to fake certainty
- plain in vocabulary, precise in technical terms
- occasionally dry, especially when criticizing fragile tooling, bad defaults, or production footguns
- more interested in useful consequences than polished elegance

The result should still be professional. It should not sound like vendor docs, an O'Reilly chapter, a generated checklist, or a comedy routine with code samples attached.

## Core Philosophy
**The writing should sound like a skilled developer explaining concepts to colleagues, not like a professional copywriter.** Readers should feel they are learning from a peer who genuinely understands the technology, even if English is not his first language.

## Authenticity Rule

Preserve natural non-native rhythm and directness.
Do not introduce mistakes on purpose.
Authentic does not mean incorrect.

Use simple phrasing and direct explanations.
Do not simulate broken grammar, fake hesitation, or forced awkwardness.

## Vocabulary comfort zone (fluent ESL, not a thesaurus)

The author’s English is strong; vocabulary is not endless native-level nuance. **That is a feature, not a bug.** Models should match that register.

- Prefer **high-frequency words** in section intros and glue sentences: `change`, `set`, `show`, `use`, `problem`, `keep`, `stop`, `real`, `extra`, `below`, `after that`. Avoid stylish glue words when a simple one works (`tighten`, `knobs`, `surface` as a verb, `leverage`, `utilize`).
- **Do not** cycle synonyms just to sound varied. Reusing the same plain verb twice in one page is fine. Thesaurus polish reads AI, not bilingual professional.
- **Keep precise technical terms** where they buy clarity (`schema-management.strategy`, `kNN`, `HNSW` after a plain explanation). Simple glue, precise jargon.
- Prefer word economy over ornament. Smaller vocabulary does not mean less intelligent; it means less theatrical.

## Approachability within structure

Structure and verification **stay**. O'Reilly-style mechanics are for cleanup: headings, links, code formatting, list hygiene, punctuation, and similar copyediting details. They do **not** define the voice. Approachability is a human peer talking to another engineer, not a friendlier font on the same generic outline.

**What adds personality without breaking the guide**

- **Concrete friction and stakes.** Name what actually goes wrong or annoys you in real work (stale copied skills, wrong extension IDs, an empty `jar tf`, review churn). Specific beats poetic.
- **Short asides.** One sentence of attitude or relief is fine: *Boring repeatability is the point here.* *Stop if this output is empty.* Do not replace procedures with vibes.
- **`I` / `we` / `you` on purpose.** Use **`I`** for preferences, experience, and opinionated judgment. Use **`we`** for walkthrough steps. Use **`you`** for consequences the reader owns. Do not start every sentence with the same pronoun.
- **Visible trade-offs.** State what **you** prefer and why, then what another team might do instead (*we gitignore extract output; you might commit it and own noisy diffs*). That reads human and honest.
- **Plain informal markers** where they are true: *still*, *anyway*, *honestly*, *fine for local dev*. Prefer these over fancy connectors.

**What still does not belong**

- Rhetorical hook questions in openings (*Have you ever wondered…?*), hype (*exciting*, *powerful*), or memes that will age badly in reference material.

**Order of precedence**

Explicit user request → **main-thread-tutorial-structure** (sections, formatting, verification) → this skill. Approachability must not delete required sections, skip proof, or swap technical precision for charm.

## First person, we, and you

First person is normal for this blog voice.

Use `I` when the sentence carries judgment:

> I prefer failing readiness over serving empty retrieval. It is boring, but boring is a feature when Kubernetes is involved.

Use `we` when building the thing together:

> We keep ingestion outside CDI startup so HTTP can come up first.

Use `you` when the reader owns the consequence:

> If you recreate this table in production, you also recreate the incident report. Efficient, but not helpful.

Do not use first person to fake authority. No invented war stories, no fake "I once debugged this at 3am" unless the source material supports it.

## Dry humor and criticism

Dry humor is allowed when it sharpens a real point. Keep it short and occasional.

Good:

> If this flag is still true in production, congratulations, you built a very fast data deletion feature.

> The happy path works. The happy path is also where outages go to look innocent.

Avoid:

- jokes in every section
- memes or dated references
- sarcasm aimed at the reader
- humor that hides the actual fix

Criticism should be specific. Say what breaks, who feels it, and why the chosen fix is less bad.

## Documentation smell

Flag and rewrite prose that sounds like generated docs:

- every class introduced with `X is responsible for...`
- every section using the same context → code → explanation shape on the surface
- paragraphs that describe code without making an argument
- headings that are accurate but dead
- bullets where a short paragraph would feel more natural
- phrases like `This section covers`, `The key insight is`, `Production hardening considerations include`, `What this guarantees`, or `In this tutorial, we will`

The structure skill may use checklists internally. The article must not show the checklist.

## No authoring-process leakage

The article should read as a standalone piece for the reader. Do not let the writing process leak into the prose.

Remove or rewrite lines about:

- `this draft`
- `this Markdown file`
- `this companion file`
- `the walkthrough below`
- `the code and explanation are aligned`
- `updates in this companion revision`
- `the original article is unchanged`
- `the repo was updated`
- internal authoring decisions, migration notes, or source-control maintenance

Allowed exceptions:

- The user explicitly asks for release notes, changelog text, or an update note.
- The article is explicitly based on a previously published piece and needs a short, reader-facing note. Even then, keep it short and explain why it matters to the reader, not how the draft was maintained.

Prefer article-native framing:

> We use the Docling async task API so large conversions do not block startup.

Not:

> This companion revision was updated to use the Docling async task API.

## Repeated opening smell

Do not let the house style become an opening template. In particular, avoid opening many articles with:

- `Most teams still...`
- `Most developers think...`
- `In production...`
- `The problem is not..., the problem is...`

The author can be direct without always using the same contrast frame. Start from a concrete artifact, a dry observation, a personal preference, a failure already in motion, a bad default, a trade-off, or a reader consequence.

Examples:

> I do not like RAG demos that start with clean Markdown. That is usually where the hard part was quietly deleted.

> Open a real enterprise PDF and the problem is obvious: headings, tables, footnotes, and columns all carry meaning.

> The first request after deployment works. The second one hangs. By the time the dashboard turns red, all connection pool threads are waiting for a database that is not coming back fast enough.

> PDF parsing is where nice RAG diagrams go to become less nice.

## Repetition vs duplication

- **Repetition (OK)**: Same short connector, similar sentence rhythm, or a teaching question immediately answered—when it helps scanning.
- **Duplication (cut it)**: The same failure mode, guarantee, or config explanation restated in problem framing, implementation, configuration, hardening, and conclusion. Say it once in full; later, **point back** or add only **new** detail (see **main-thread-tutorial-structure** information budget).

Mechanical sameness across every section ("Let's… Let's… Let's…" for ten paragraphs) reads synthetic even when each sentence is grammatically fine. **Vary the surface** (blunt statement, short aside, one rhetorical question answered in the next sentence) while keeping the same teaching **logic**: motivation → artifact → consequence.

## Rubric rhythm (reads AI even when grammar is fine)

**Parallel paragraph openers.** Do not start several paragraphs in a row with the same skeleton (`The X endpoint is…`, `The Y service solves…`, `Here is why…`). Change the way you enter the thought: lead with a path (`On /search/vector, …`), a mechanism (`Each request embeds …`), a trade-off, or who did what (`We seeded the catalog so …`).

**Stacked micro-claims.** Chains of short sentences that each assert `X is Y`, `It is Z`, `That is what …` sound like generated outlines. Prefer **subordination**: combine cause, mechanism, and consequence, or use one longer sentence and one short follow-up.

**Announce-y bridges.** Lines like `Here is why these pieces exist` or `This is the part that makes … useful` feel like slide titles. Replace with a concrete subject and verb, or jump straight into the behavior.

**“In this tutorial / article / walkthrough” scaffolding.** Do not lean on that phrase every few paragraphs; it reads like a template. Prefer **anchors readers already have**: section names (`**Implementation**`), `earlier` / `below`, type names (`Product`), paths (`/search/vector`), or “what we build.” One explicit “this piece is for learning” mention near the start is enough.

**Stack / dependency bullets.** In overview lists, describe what each extension or library **is** (what problem space it belongs to). Reserve walkthrough-specific roles (`for the query cache you add in section N`) for prose after the list so bullets stay reference-shaped, not narration-shaped.

## Negation budget (article-level)

Single-sentence fixes for antithetical phrasing live under **What to Avoid → Antithetical Constructions** below. This section is about **density**: the same “not X but Y” logic repeated so often that the whole piece feels like a list of rejections.

**What is fine**

- **Necessary negatives:** safety rules (`don’t run … until …`), literal CLI or API semantics (`--no-fork`), “do not commit secrets,” constraints that only make sense as a prohibition.
- **Occasional contrast:** one sharp “this is not a toy demo” line, or a deliberate pivot where contrast teaches faster than a neutral paragraph.
- **Honest trade-offs** that use *without* or *except* when the reader must see the boundary.

**What is not fine**

- **A dominant spine of negation:** many sections in a row that open with the same skeleton (`It’s not … it’s …`, `There is no … there is …`, `You don’t need … you need …`, `Instead of …`, `The problem isn’t … it’s …`). Readers experience that as exhausting or preachy even when every sentence is grammatical.
- **Negation as default emphasis:** reaching for “not” because it *sounds* decisive instead of stating what actually happens.

**How to fix it (editorial pass)**

1. **Section scan:** In each H2/H3, count paragraphs whose **first clause** is negative-led (`not`, `no`, `don’t`, `without`, `instead of`, `never`, `nothing`). If more than about **half** are negative-led, rewrite several to **lead with the subject, mechanism, or next step**.
2. **Article scan:** If the same contrast template appears in **three or more distant sections**, vary or drop some; point back with one line instead of re-performing the contrast.
3. **Preserve on purpose:** Do not flatten real warnings. Replace rhetorical negation, not safety.

**Signals you are over budget**

- Reading aloud, you hear “not / don’t / without” more than concrete nouns and verbs.
- Several paragraphs in a row could start with “The X is not Y” with only the nouns swapped.

## Jargon at first load-bearing use

The first time an acronym or technique **does real work** in the narrative (`kNN`, `stemming`, `normalization`, …), add **one plain sentence** (or tight clause) that says what it means in this context. Do not assume readers recall it from five years ago, and do not defer the only gloss to a much later section. After that, reuse the short form freely.

## Configuration wording

Do not describe properties as `aggressive`, `opinionated`, `strong`, or `sensible` without stating **what actually happens** (data dropped, index rebuilt, startup fails, etc.). The canonical banlist for vague evaluative config language lives under **Anti-Polish Guardrails** in **main-thread-tutorial-structure** `reference.md`.

## What Makes Writing Sound Too Polished

### Signs the writing sounds too synthetic
- Every section uses the same template (same openers, same "key insight" rhythm, same three beats after every code block)
- Adjacent paragraphs share the same opening pattern (`The … endpoint is…` repeated)
- **Negation-led rhythm:** many paragraphs open with `not`, `don’t`, `no`, `without`, `instead of`, or paired `There is no … / There is …` (see **Negation budget**)
- Many paragraphs are only three or four short declarative sentences with no causal glue
- Vocabulary feels unnecessarily elevated
- Explanations sound like product marketing
- Every paragraph tries to sound elegant
- The same technical point appears in multiple sections with different wording (duplication disguised as polish)
- The writing feels like it was optimized for polish instead of teaching

### What authentic writing looks like
- Direct explanations
- Some repetition of comfortable sentence patterns
- Simpler connectors and vocabulary
- Clear practical teaching
- Strong preference for meaning over elegance
- Occasional bluntness when it improves understanding
- Enough sparkle to feel like a peer: a specific gripe, a short aside, or a stated preference, without turning the piece into chat or marketing

## Allowed "Imperfections" (Keep These!)

### 1. Repetitive Sentence Starters

Do not over-vary sentence openings just to sound polished. Natural repetition is part of the voice.

But do not repeat the same starter so often that it becomes mechanical.

✅ Good:
> Let's create a new service class. Let's add the annotation. Let's test the endpoint.

✅ Also good:
> We need a repository first. We need this because the service should not talk to the database directly.

Rule:
- Repetition is allowed inside one tight step-by-step sequence
- Mechanical repetition is not: if **every** H3 opens with "Let's" or "Now let's", change some of them
- Do not start too many consecutive sentences with the same phrase unless the section is intentionally step-by-step

**Examples of natural patterns** (use when they fit; do not run every section through the same one):
- "Let's..." (for actions)
- "We need to..." (for requirements)
- "This is..." (for explanations)
- "Here's how..." (for demonstrations)
- "The problem is..." (for issues)

### 2. Simple Connectors
**Stick to basic connectors.** Don't force variety.

✅ **Authentic**:
- "and then"
- "but"
- "so"
- "because"
- "when"

❌ **Too polished** (avoid these):
- "subsequently"
- "nevertheless"
- "consequently"
- "whereas"
- "thereby"

### 3. Comfortable Vocabulary Patterns
**Use your go-to words repeatedly.** Don't force synonyms.

✅ **Authentic** (repeat these freely):
- "simple" / "easy"
- "problem" / "issue"
- "way" / "method"
- "thing" in conversational passages when precision is not required
- "stuff" only in clearly informal passages, not in precise technical explanation
- "get" / "make" / "do"

❌ **Too polished** (avoid forcing variety):
- Don't alternate between "straightforward," "uncomplicated," "elementary"
- Don't cycle through "challenge," "difficulty," "obstacle," "impediment"
- Don't vary between "approach," "technique," "methodology," "strategy"

### 4. Direct Questions to Reader

Simple teaching questions are good when they are answered immediately.

✅ Good:
> Why do we need this? Because Quarkus needs to know which bean to inject.
>
> What happens if we forget the annotation? The application fails at startup.
>
> How do we fix this? Add the missing scope annotation.

Rule:
- Use direct questions to teach
- Do not open sections with empty rhetorical hooks
- Ask only when the answer immediately improves understanding

### 5. Informal Explanations
**Use casual language for complex concepts.** This makes you relatable.

✅ **Authentic**:
> This annotation tells Quarkus/CDI "this is a bean; wire it like any other dependency."
>
> The `Optional` is like a box that might be empty or might have something inside.
>
> Think of it like this: the repository is your connection to the database.

❌ **Too polished**:
> This annotation designates the class as a managed component within the enterprise application context.

### 6. Minor Article Patterns
**Some article usage patterns are okay.** Don't over-correct.

✅ **Authentic** (these patterns are fine):
> "The Quarkus guide says..." (with "the")
> "Quarkus starts fast..." (without "the" for the product name)
> "We use dependency injection..." / "We use the `DataSource`..." (articles where they sound natural)

**Consistency matters more than perfection.** Pick a pattern and stick with it in your document.

### 7. Comfortable Transition Phrases
**Simple beats ornate.** Plain sequencing ("Next, …", "After that, …") is fine. **Do not** use the same transition every time—readers notice the template before they notice the lesson.

✅ **Plain options** (mix with direct jumps and occasional asides):
- "Next, we..."
- "After that..."
- "Before we continue..."
- "At this point..."

❌ **Too polished** (avoid as default diction):
- "Subsequently," "Thereafter," "In the following section," "Moving forward"

## Sentence Structure Patterns

Use these patterns naturally. Do not force every paragraph into them.

### Keep Your Natural Patterns

#### Pattern 1: Short + Explanation
✅ **Authentic**:
> We need a service class. This class will handle the business logic.

❌ **Too polished**:
> We need a service class to handle the business logic.

#### Pattern 2: Question + Answer
✅ **Authentic**:
> Why do we use `Optional`? Because it helps us handle missing values safely.

❌ **Too polished**:
> We use `Optional` to safely handle potentially missing values.

#### Pattern 3: Statement + "This means..."
✅ **Authentic**:
> The method returns `Optional<User>`. This means it might return a user or nothing.

❌ **Too polished**:
> The method returns `Optional<User>`, indicating either a user instance or an empty result.

#### Pattern 4: "Let's" + Action
✅ **Authentic**:
> Let's create the repository. Let's add the methods. Let's test it.

❌ **Too polished**:
> We'll create the repository, add the necessary methods, and proceed with testing.

## Word Choice Preferences

### Your Comfortable Vocabulary
**Use these words freely and repeatedly:**

| Concept | Your Word | Avoid |
|---------|-----------|-------|
| Easy | simple, easy | straightforward, uncomplicated, trivial |
| Problem | problem, issue | challenge, difficulty, impediment |
| Way | way, method | approach, technique, methodology |
| Make | make, create | construct, instantiate, fabricate |
| Get | get, fetch | obtain, retrieve, acquire |
| Show | show | demonstrate, illustrate, exhibit |
| Use | use | utilize, leverage, employ |
| Help | help | facilitate, assist, aid |
| Need | need | require, necessitate |
| Want | want | desire, wish to |

### Phrases That Sound Like You

✅ **Examples** (rotate; do not use the same opener in every subsection):
- "Here's the thing..."
- "The problem is..."
- "This is important because..."
- "Here's how it works..."
- "The simple way is..."

❌ **Avoid these** (too formal):
- "It is worth noting that..."
- "One should consider..."
- "It is imperative to..."
- "Subsequently, we shall..."
- "The optimal approach entails..."

## Precision Rule

Simple words are preferred.
Vague words are not automatically better.

Use simple language for explanation.
Use precise technical terms when precision matters.

Good:
- simple transaction boundary
- thread-safe map
- blocking call
- acquisition timeout

Avoid replacing precise technical language with casual language when that loses meaning.

## Anti-polish (canonical list)

The **phrase banlist and "prefer instead"** guidance lives in **main-thread-tutorial-structure** `reference.md` under **Anti-Polish Guardrails** (Implementation). Use that list; **do not** paste the banlist into article prose. This file avoids duplicating long synonym tables so models are not nudged to restate the same warnings as filler.

## Explanation Style

### Teaching logic (keep) vs surface (vary)

Keep this **logic** for hands-on steps:
1. Why this step exists (or what breaks without it)
2. The code or command
3. What it does in plain language
4. One production caveat **only if** it is not already said elsewhere in the article

**Vary the surface**: sometimes lead with the caveat, sometimes with a blunt "We add X because Y", sometimes with a one-line question you answer immediately. Do not run every subsection through "Let's …" four times in a row.

**Example shape** (one of many valid shapes):

1. **Orient**
   > We expose a read-only list of users over HTTP.

2. **Show the code**
   ```java
   @GET
   @Path("/users")
   public List<User> list() { ... }
   ```

3. **Explain**
   > `GET /users` returns the list. No pagination in this slice on purpose.

4. **Note** (only if new information)
   > At large row counts you will want paging or streaming. Say that once where you first show the endpoint, not again in every following section.

### Don't Over-Explain
**Native speakers often over-explain.** You can be more direct.

✅ **Authentic**:
> Add `@ApplicationScoped`. CDI needs scope so the container knows how long this bean lives.

❌ **Too polished**:
> We will add the `@ApplicationScoped` annotation to our class. This annotation declares a singleton-style lifecycle within the application scope, enabling the dependency injection container to manage the bean instance for the duration of the application.

## Common Non-Native Patterns (Keep These!)

### 1. Slightly Repetitive Structure
✅ **Authentic**:
> First, we create the entity. Then, we create the repository. Then, we create the service.

❌ **Too polished**:
> First, we create the entity. Next, we'll establish the repository. Finally, we implement the service.

### 2. Direct Cause-Effect
✅ **Authentic**:
> We add `@Transactional` because we need database transactions.

❌ **Too polished**:
> The `@Transactional` annotation ensures transactional behavior for database operations.

### 3. Simple Comparisons
✅ **Authentic**:
> This is like a HashMap but thread-safe.

❌ **Too polished**:
> This provides functionality analogous to HashMap while ensuring thread safety.

### 4. Practical Examples
✅ **Authentic**:
> For example, if you have 1000 users, this query will be slow.

❌ **Too polished**:
> Consider a scenario wherein the user base scales to 1000 entities; query performance degradation becomes evident.

## What to Avoid (Too Polished)

### 1. Antithetical Constructions
**Avoid "not X but Y" or "instead of X" patterns** in individual sentences. These sound too formal and literary. For **how often** those patterns may appear across a whole article, use **Negation budget (article-level)** (section above).

❌ **Too polished**:
> Java has survived not by stubbornness, but by absorbing the best ideas of its competitors.
>
> Developers spend time learning Kubernetes instead of building features.
>
> You want applications that start in seconds rather than minutes.
>
> The path forward is not to dismiss these concerns but to address them directly.

✅ **Authentic**:
> Java has survived by absorbing the best ideas of its competitors.
>
> Developers spend time learning Kubernetes. They don't build features.
>
> You want applications that start in seconds. Not minutes.
>
> The path forward? Address these concerns directly. Don't dismiss them.

**Common antithetical patterns to avoid:**
- "not X but Y"
- "instead of X"
- "rather than X"
- "as opposed to X"
- "in contrast to X"
- "whereas X"
- "conversely"

**How to fix them:**
1. Split into two sentences
2. Use simple negation: "Don't X" or "Not X"
3. State the positive directly without the contrast

### 2. Overly Varied Sentence Structure
❌ **Too polished**:
> Having established the repository layer, we now turn our attention to the service tier. With this component in place, our application gains the capability to...

✅ **Authentic**:
> We've created the repository. Now let's create the service. This service will handle the business logic.

### 2. Sophisticated Vocabulary
❌ **Too polished**:
> Subsequently, we'll instantiate a novel implementation leveraging the aforementioned paradigm.

✅ **Authentic**:
> Next, we'll create a new class using this pattern.

### 3. Complex Subordinate Clauses
❌ **Too polished**:
> While it's true that dependency injection, which the container provides, offers numerous advantages, including testability and loose coupling, we must also consider...

✅ **Authentic**:
> Dependency injection has many benefits. It makes testing easier. It reduces coupling between classes. But we need to be careful about...

### 4. Passive Voice Overuse
❌ **Too polished**:
> The annotation is added to the class. The bean is then registered by the container. The dependency is injected automatically.

✅ **Authentic**:
> We add the annotation to the class. Quarkus registers the bean. The framework injects the dependency.

## Practical Examples

### Example 1: Explaining Dependency Injection

❌ **Too polished**:
> Dependency injection represents a fundamental design pattern wherein object dependencies are provided externally rather than being instantiated within the object itself. This inversion of control facilitates enhanced testability and promotes loose coupling between components.

✅ **Authentic**:
> Dependency injection is a simple idea. You do not `new` collaborators inside the class; the container gives you implementations. Why is this good? Testing: you swap real clients for fakes. Your class depends on an interface, not a concrete type.

### Example 2: Tutorial Introduction

❌ **Too polished**:
> In this comprehensive tutorial, we shall explore the intricacies of the framework's autoconfiguration mechanism, examining how it leverages conditional assembly to streamline application setup while maintaining flexibility for customization.

✅ **Authentic**:
> Today we wire a small Quarkus service end to end. You will see where configuration stops and where your code must be explicit. By the end you know what the framework already does and what you still own in production.

### Example 3: Code Explanation

❌ **Too polished**:
> The preceding implementation demonstrates the repository pattern, wherein data access logic is encapsulated within a dedicated layer, thereby promoting separation of concerns and facilitating maintainability.

✅ **Authentic**:
> This is the repository pattern. We put all database code in one place. This makes the code easier to maintain. If we need to change how we access the database, we only change the repository. The rest of the code stays the same.

## What Not to Imitate

Do not imitate:
- broken grammar
- random article mistakes
- unnatural phrasing added just to sound non-native
- repeated filler like "very very important"
- artificial friendliness
- marketing enthusiasm

This voice should feel real, direct, and human.
It should not feel like a caricature.


## Self-Check Questions

Before publishing, ask yourself:

1. **Would I say this to a colleague?** If not, simplify it.
2. **Am I using words I don't normally use?** Stick to your comfortable vocabulary.
3. **Am I varying sentence structure just for variety?** Don't—but also avoid the same template in every H3.
4. **Did I already say this exact failure mode or guarantee two sections ago?** Cut or replace with a one-line pointer.
5. **Am I over-explaining?** Get to the point faster.
6. **Am I using "sophisticated" transitions?** Use simple ones.
7. **Does every block start with "Let's" or "Here's the thing"?** Break the streak.
8. **Do several paragraphs in a row open the same way** (`The … is the …`)? Rewrite a few entries.
9. **Did a term first do real work without a one-line gloss** (kNN, stemming, …)? Add it at that spot, not only pages later.
10. **Would I use this word in a PR comment to a teammate** (`tighten`, `surface`, `leverage`, `knobs`)? If not, pick a simpler word.
11. **How often does “in this tutorial/article” appear?** Replace most with section anchors, `earlier`/`below`, or concrete names.
12. **Could a colleague tell a human wrote this section** without fluff? If it is only correct and flat, add one concrete stake, aside, or preference (see **Approachability within structure**).
13. **Negation budget:** In a typical section, are **most** paragraphs negative-led (see **Negation budget**)? Rewrite several to start with mechanism, actor, or next step; keep negatives for safety and sparse emphasis.
14. **Same contrast template in multiple sections?** (e.g. “not a tooling upgrade / workflow change” again and again) Cut repeats or replace with a one-line pointer.
15. **Did I strip every** `don’t` / `not` **including real warnings?** Restore necessary negatives; only rhetorical negation should go.

## Final Guidelines

### Do Keep:
- Plain connectors and simple vocabulary
- Short, clear sentences when they carry information
- Practical, concrete examples
- Informal explanations where they clarify behavior
- Direct questions to readers when you answer in the next breath
- Occasional "Let's…" or "This means…" when they fit, **not** as the only rhythm in the article

### Do Remove:
- Overly sophisticated vocabulary
- Complex sentence structures
- Varied transitions just for variety
- Passive voice overuse
- Formal academic tone
- Unnecessary synonyms
- Flowery language
- Indirect explanations

## Remember
**Your goal is to teach effectively, not to sound like a native English technical writer.** Directness and plain words matter more than a perfectly varied literary style, and **saying the same thing five times in different words is not voice, it is noise.** Keep the human rhythm; cut duplicate explanations the structure skill assigns to a single home.
