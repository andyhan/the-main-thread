# The Main Thread - Tutorial Structure Guide

This guide controls structure, technical completeness, verification, and tutorial flow.
Voice decisions belong to the **authentic-voice** skill. When the skill files live together, use `../authentic-voice/reference.md` for the voice rules.

## Purpose
Invisible story scaffolding for hands-on technical tutorials. It keeps The Main Thread articles contained, production-focused, and verifiable without making them sound like technical documentation.

O'Reilly style is a copyediting layer here: headings, links, lists, code formatting, numbers, punctuation, and similar mechanics. It is **not** the prose voice.

## Quick Map

- **Authority Hierarchy**: conflict order for user requests, structure, style, and platform constraints
- **Model Operating Rules**: how to handle missing context, freshness, and research mode
- **Story Beats**: required narrative beats and information budget
- **Opening Variation**: keep the problem first without repeating the same opener
- **Implementation**: component pattern, complete code rules, analysis patterns, and anti-polish guardrails
- **Configuration**: `application.properties` expectations and failure-mode explanations
- **Production Hardening**: real production concerns and stress behavior
- **Verification**: commands, tests, expected output, and probabilistic-system checks
- **Conclusion**: one tight outcome paragraph and final GitHub link
- **Standalone Articles and Code Links**: no authoring-process leakage, canonical repo URL
- **Formatting Mechanics**: headings, lists, code, typography, and links
- **Quality Checklist**: final readiness checks
- **Output Mode Rules**: tutorial, quick guide, architecture, review, and rewrite modes

---

## Authority Hierarchy

When conflicts arise, apply in this order:
1. **Explicit user request** (current conversation)
2. **authentic-voice** for prose voice and personal narration
3. **This structural guide** for story shape, completeness, and verification
4. **O'Reilly style guide** for mechanics only: formatting, typography, grammar, code style, links, and lists
5. **Writing platform constraints** (Substack, blog platform limits)

---

## Model Operating Rules

These instructions assume current ChatGPT models with strong reasoning, tool use, and long-context synthesis.

- Default to one strong draft instead of many shallow drafts
- For long tutorials, create a short working plan internally and then write
- Do not expose internal reasoning
- When facts may have changed, verify them before writing
- Prefer grounded detail over polished filler
- When source material is incomplete, make the smallest safe assumption and state it clearly
- Use tools only when they materially improve accuracy or deliver the requested artifact

## Freshness Rules

Do not rely on memory for facts that can change.

Always verify:
- Quarkus versions
- Java versions
- extension names
- CLI syntax
- dependency coordinates
- model names
- API capabilities
- pricing, limits, and availability
- product or framework behavior that may have changed

Prefer official documentation for version-sensitive claims.

## Research Mode

Use supplied material only when the task is a rewrite, structural edit, or voice edit.

Use live verification when the task includes:
- current framework features
- latest releases
- model support
- product comparisons
- security guidance
- performance claims
- operational recommendations

## Ambiguity Handling

When a required detail is missing:
- use the smallest safe assumption
- state the assumption briefly
- do not invent product behavior, API syntax, or version-specific details
- prefer placeholders only when the user explicitly asked for a template

---

## Standalone Articles and Code Links

Default article mode is standalone. The reader should not see how the article was produced, revised, copied from a draft, synchronized with a repository, or adapted from another document.

Do not include:

- companion-file explanations
- maintained Markdown notes
- repo-tracking commentary
- internal changelogs
- "the original article is unchanged"
- "this file follows the source code"
- "the walkthrough below was updated"
- "the code and explanation are aligned with current APIs"

Allowed exceptions:

- The user explicitly asks for release notes, migration notes, or an update note.
- The piece is intentionally framed as an update to a previously published article. Keep the note short and reader-facing.

All article code lives under:

```text
https://github.com/myfear/the-main-thread/<repository-name>
```

When the repository name is known, end with that canonical URL. Do not write placeholder text like "add your Git remote when publishing."

---

## Story Beats

Every hands-on tutorial needs these beats unless the explicit user request selects another mode such as quick guide, architecture mode, review mode, or rewrite mode. The beat order should be visible to the reader as a smooth path, not as exposed checklist prose.

Think:

1. Why I care
2. What we build
3. What you need
4. Build the base
5. Make it work
6. Configure it
7. Make it survive
8. Prove it
9. Close the loop

The actual headings can be simpler and conventional. The narration should come from **authentic-voice**.

### Information budget (single custody)

Duplicate prose reads artificial and wastes senior readers' time. **Each major risk, guarantee, or failure mode gets one primary explanation** in the article.

- **Pick a home**: Explain it fully in exactly one of: problem framing, the first implementation block where it matters, the configuration section (property-by-property), or production hardening—whichever teaches it best.
- **Everywhere else**: At most **one short pointer** back ("Same acquisition timeout as in configuration—fail fast instead of blocking forever.") unless you introduce **new** interaction or a **new** failure mode.
- **Implementation vs configuration**: If `application.properties` already states what breaks when a key is missing or wrong, the implementation analysis **must not** repeat that paragraph. Add only code-specific behavior the properties section did not cover.

The conclusion has its own stricter rules under **Conclusion** (section 8).

### 1. Why I care (problem framing, 3-6 paragraphs)

**Purpose**: Shift reader from naive mental model to production reality.

**Required components**:
- What developers typically think the problem is
- Why that mental model fails in real systems
- What breaks in production when it fails

**Voice**: Personal technical blog voice. First person is allowed when the author is stating a preference, experience, or criticism. Do not invent experience.

**Mechanics**:
- Use flowing paragraphs (2-4 sentences each)
- No bullet points in this section
- Prefer direct claims when they are universally true for the scenario you show
- Simple words: "breaks" not "exhibits degraded behavior"

**Question rule**:
- Do not use rhetorical hook questions in introductions
- Teaching questions are allowed inside explanations when they are answered immediately and improve clarity

### Opening variation

The opening must put the problem or production challenge front and center. It must not reuse the same rhetorical skeleton across articles.

Avoid defaulting to:

- `Most teams still...`
- `Most developers think...`
- `In production, this is not X, it is Y`
- `The problem is not..., the problem is...`

Choose one opening angle:

- **Incident**: start with the failure already happening
- **Personal judgment**: start with what I dislike, prefer, or distrust
- **Concrete artifact**: start with a file, command, request, log line, config value, or user action
- **Bad default**: start with the trap hidden in normal setup
- **Trade-off**: start with the decision the reader has to make
- **Reader consequence**: start with what breaks in their system
- **Myth correction**: correct a common mental model, but avoid the repeated `Most teams...` shape
- **Dry observation**: short, specific criticism when it clarifies the problem

For a batch of articles, vary the opening angle. If the previous article used `Most teams...`, do not use that shape again.

**Example opening** (personal judgment):
> I do not like RAG demos that start with clean Markdown. That is usually where the hard part was quietly deleted.
>
> Real enterprise PDFs are not flat text. Tables, headings, footnotes, and multi-column layout carry meaning. Strip that structure and retrieval feeds the model fragments without context. The answer may still sound confident, which is exactly the annoying part.

---

### 2. What you need (prerequisites, 1 paragraph + bullet list)

**Format**:
```
One context paragraph explaining what readers need.

- Java 21 installed
- Quarkus CLI (`quarkus create app`)
- Basic understanding of REST endpoints
- 15 minutes
```

**Mechanics**:
- Spell out numbers zero through nine, numerals for 10+ (15 minutes)
- Use bullets for this list (exception to "minimal bullets" rule)
- Sentence case for each item
- No periods after bullet items (these are fragments)

**What to include**:
- Required software with versions
- Required knowledge (be honest about assumptions)
- Estimated time
- Link to previous articles if building on them

**What to exclude**:
- Setup sprawl (no "install Java" instructions for senior devs)
- Motivational language ("exciting", "powerful")

---

### 3. Build the base (project setup)

**Format**:
```
Create the project:

```bash
quarkus create app com.example:connection-pool-demo \
  --extension=quarkus-rest,quarkus-jdbc-postgresql,quarkus-agroal
```

Extensions explained:
- `quarkus-rest`: REST endpoints
- `quarkus-jdbc-postgresql`: PostgreSQL driver and connection pooling
- `quarkus-agroal`: Connection pool management (included with JDBC)
```

**Mechanics**:
- Code in constant width: **`quarkus create app`**
- Extension names in `code font`
- Prefer commas, parentheses, or short follow-up sentences in body text
- Colon before code block (O'Reilly convention)

**The Main Thread rules**:
- One command only
- Explain why each extension exists
- Do not add container setup unless it materially affects the tutorial
- Prefer Podman when a container runtime is needed
- If Dev Services, local registries, or local model runtimes depend on containers, explain that dependency briefly
- Dev Services enabled by default (mention if relevant)

### Generated project dependency rule

After `quarkus create app`, do not add generic dependency-maintenance prose such as:

> Align the generated `pom.xml` with the Quarkus platform BOM...

If the CLI command creates the project, assume the generated Quarkus platform BOM, plugin, and platform-managed extensions are aligned.

Instead:

- State the Quarkus version once near setup: `This article uses Quarkus 3.34.3 and Java 21.`
- Explain each extension in the extension list.
- For Quarkus platform-managed extensions, do not mention versions unless version behavior matters.
- For Quarkiverse, third-party, or manually pinned extensions, mention the version in the extension list or in a short note.
- If a manual POM edit is required, show the exact XML snippet and explain why.
- Do not include a full `pom.xml` unless the POM itself teaches something or contains several non-obvious manual changes.

Bad:

> Align the generated `pom.xml` with the Quarkus platform 3.34.3 BOM and LangChain4j BOM, and pin `io.quarkiverse.docling:quarkus-docling` to 1.3.0 or a newer compatible release.

Better:

> This article uses Quarkus 3.34.3 and Java 21. The Quarkus CLI keeps the platform-managed extensions on the same stream. `quarkus-docling` comes from Quarkiverse, so we pin it separately at 1.3.0.

Better still, put the version where the reader first sees the extension:

- `quarkus-docling` (`io.quarkiverse.docling:quarkus-docling:1.3.0`): Docling REST client and Dev Services for the Docling container

### Setup explanation budget

Project setup prose should explain only choices the reader must understand or might reasonably change.

Do not narrate what the command already says.

Bad:

> The group `com.ibm` matches the package used in the sources.

Better, when the package name is a real copy-paste constraint:

> Use `com.ibm` here because the Java sources below use that package.

Better still, when no special warning is needed:

> This article uses Quarkus 3.34.3 and Java 21. Create the project:

Explain traps, choices, and manual edits. Let obvious flags be obvious.

---

### 4. Make it work (implementation)

For **every major component**, include these ingredients. Do not force the surface rhythm to repeat every time.

#### Component Pattern

**a) Context paragraph**
```
Why this component exists and what problem it isolates.
```

**b) Complete source code**
```java
// Full, compilable code
// Include ALL imports
// Include package declaration
// No placeholders like "// your code here"
```

**Code requirements**:
- Code must be internally consistent across all tutorial sections
- Imports, package names, endpoint paths, and configuration keys must match
- Do not silently switch APIs, frameworks, or naming halfway through the tutorial
- Do not invent convenience methods or framework behavior that does not exist

**c) Human analysis**

Address these themes without exposing them as checklist labels unless the topic truly needs that shape:
- **Guarantees and limits**: What it does and explicitly what it doesn't
- **Behavior under stress**: Load, contention, failure modes
- **Design decisions**: Why this approach vs. alternatives

**How to vary the structure**:

**Pattern 1: Flowing narrative** (most common)
Write naturally, weaving in the themes without labels:

> The `@Transactional` annotation wraps this method in a database transaction. If any exception escapes, Quarkus rolls back all changes automatically. But this only covers database operations—external API calls, file writes, and message publishing are **not** part of the transaction boundary. If you call Stripe's API mid-transaction and it succeeds, then your database update fails, you've charged the customer but haven't recorded it.

**Pattern 2: Direct comparison**
Contrast what works vs. what breaks:

> This validator uses a blocklist of dangerous Unicode characters. When someone submits `user<script>`, validation fails immediately with a 400 response. The request never reaches your service layer. But blocklists are incomplete by definition—new attack vectors appear constantly. **Production systems combine this with output encoding** at render time. The validator stops obvious attacks; your template engine prevents the subtle ones.

**Pattern 3: Progressive revelation**
Start simple, add complexity:

> At first glance, this looks like a standard REST endpoint. It accepts JSON, returns JSON, nothing special. But look at the return type: `Uni<Response>`. This is Quarkus's reactive type. Instead of blocking the thread while querying the database, this method returns immediately. The database work happens on a separate thread pool, and the result gets written to the HTTP response when it completes.
>
> This means under load, you're not burning request threads waiting for PostgreSQL. A single Quarkus instance can handle thousands of concurrent requests with a thread pool of 20. The cost? Debugging becomes harder—stack traces span multiple threads, and you can't use ThreadLocal state.

**Pattern 4: Production story**
Lead with the failure mode:

> I've seen this exact pattern fail in production. Everything works fine until you deploy during peak traffic. Suddenly, your connection pool exhausts, requests time out, and your monitoring dashboard lights up red. The problem? No acquisition timeout. When the pool runs dry, threads block forever waiting for a connection. Load balancers see slow responses and send more traffic to healthy instances, which exhaust their pools, cascading the failure across your entire cluster.
>
> The three-second timeout we configured prevents this. Requests fail fast with 503 instead of queueing indefinitely. Your application stays responsive, and your load balancer can route around the problem.

**Pattern 5: Point of view + supporting detail**
Lead with the main point:

> I like this pattern because it moves validation from runtime to compile time. Without the sealed interface, you could accidentally create an `InvalidEmail` and pass it around. The type system wouldn't stop you. By sealing the interface and making the invalid case package-private, only the factory method can construct instances. If you have an `Email` reference, it's guaranteed to be valid.
>
> The trade-off is verbosity. You need a factory, an interface, two implementations, and package-private constructors. For email addresses, this might be overkill. For financial amounts or security tokens, it's exactly the right level of paranoia.

**Pattern 6: Socratic** (rare, use sparingly)
Guide the reader through reasoning:

> Why use `Optional<User>` instead of returning `null`? Consider what happens at the call site. With null, every caller must remember to check. Miss one, and you get `NullPointerException` in production. With `Optional`, the type system forces the decision: call `orElseThrow()`, `orElse()`, or `ifPresent()`. You can't accidentally dereference it.
>
> But `Optional` has costs. It allocates an object for every query, adding GC pressure. For high-throughput systems, this matters. That's why Hibernate's `Session.get()` still returns null—performance over safety. We're using `Optional` because **teaching correct patterns** beats micro-optimizations.

**Mix patterns within a tutorial**:
- Use flowing narrative for most components
- Use a production story for critical failure modes when there is a real story or a realistic failure path
- Use progressive revelation for complex abstractions
- Use comparison when alternatives are obvious

**Variation contract**: If the tutorial has **three or more** major implementation components, vary the surface. Do not make every block open with the same cadence, the same bold "key insight" line, or the same three-paragraph shape. The reader should feel a human is deciding how each piece needs to be explained.

**Example structure with varied analysis**:
```markdown
We need a repository that handles connection failures gracefully.

```java
package com.example.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class UserRepository {
    
    @Inject
    DataSource dataSource;
    
    public User findById(Long id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // query implementation
        }
    }
}
```

The try-with-resources block guarantees connection cleanup, even when queries throw exceptions. Without it, a single uncaught exception leaks a connection. Leak enough connections and your pool exhausts, taking down the entire application. I've debugged this at 2am more times than I care to admit.

But connection cleanup alone isn't enough. If all 20 pool connections are busy, `getConnection()` blocks the calling thread indefinitely. No timeout means **unbounded waiting**. One slow query cascades into thread pool exhaustion. We'll add an acquisition timeout in the configuration section to fail fast instead.

Why not use Panache? Panache is excellent for straightforward CRUD, but it hides connection management. For this tutorial, we need to see exactly when connections are acquired and released. Once you understand the mechanics, Panache becomes the right abstraction.
```

**Anti-patterns to avoid**:
- ❌ Starting every analysis with "What this guarantees:"
- ❌ Numbered lists of guarantees/limits (too mechanical)
- ❌ Repeating the same structure for every component
- ❌ Saying "this is important" without showing consequences
- ❌ Generic statements like "handles edge cases" without specifics
- ❌ Class-by-class neutral filler (`X is responsible for...`) when a sharper sentence would teach more
- ❌ Letting the structure skill produce documentation prose instead of blog narration

---

#### Anti-Polish Guardrails

**Canonical phrase banlist for this repo** (other skills link here; do not paste the list into tutorials as meta-commentary).

Avoid generic polished AI phrasing.

Do not write:
- In today's fast-evolving landscape
- powerful feature
- seamless integration
- unlock the potential
- robust and scalable solution
- comprehensive guide
- enterprise-grade, unless proven in the tutorial itself
- vague judgment on configuration: "aggressive settings," "opinionated defaults," "strong" policies; say what actually happens (data dropped, index rebuilt, unsafe in production, etc.)

Prefer:
- direct statements
- concrete trade-offs
- failure modes
- operational consequences
- code-level explanation


---

### 5. Configure it (application.properties)

**Format**:
```
Configure the connection pool in `src/main/resources/application.properties`:

```properties
quarkus.datasource.jdbc.min-size=5
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.acquisition-timeout=3s
```

Each setting explained:
- `min-size=5`: Always keep 5 connections ready (avoids cold-start latency)
- `max-size=20`: Never create more than 20 connections (protects database)
- `acquisition-timeout=3s`: Fail fast if no connection available (prevents cascading failures)
```

- Explain what breaks when the setting is missing or wrong
- Prefer realistic defaults over placeholder demo values
- When a value is environment-specific, say what to tune and why
- If the implementation section already explained a failure mode tied to this key, **cross-reference** instead of copying the paragraph (see **Information budget** above)


**Mechanics**:
- Property files in `constant width`: `application.properties`
- Use code blocks with `properties` highlighting
- Explain every setting (no "configure as needed")

**Main Thread rules**:
- Production values, not demo values
- Explain the failure mode each setting prevents
- No handwaving ("tune based on load" without guidance)

---

### 6. Make it survive (production hardening)

**Purpose**: Shift from "it works" to "it survives production"

Pick the **three most real risks** created by this article. Do not satisfy an arbitrary category checklist just because the template has one.

Possible areas:
1. Latency and throughput under contention
2. Concurrency and ordering guarantees
3. Precision and correctness boundaries
4. Operational failure modes (circuit breakers, retries, timeouts)
5. Security abuse cases (injection, DoS, resource exhaustion)

For AI, search, messaging, and distributed workflows, hardening must focus on system behavior, not just happy-path correctness.

Also consider:
- fallback behavior
- retry boundaries
- timeout placement
- permission enforcement
- data isolation
- observability during failure


Tie this section back to the original problem framing. The reader should see why the happy path is not enough. This is also a good place for a short first-person preference or dry warning when it clarifies the risk.

**Format**:
```markdown
### What Happens Under Load

When all 20 connections are busy, new requests wait up to 3 seconds before failing. This is intentional. **Fast failure is better than silent queueing.**

Test it yourself:
```bash
# Simulate 50 concurrent requests
for i in {1..50}; do
  curl http://localhost:8080/users/1 &
done
wait
```

You'll see some requests fail with HTTP 503. This is correct behavior. Your application is protecting the database from overload.

### Concurrency Guarantees

Connection pools don't prevent race conditions in your SQL. If two threads run `UPDATE users SET balance = balance - 100`, you need database-level locking or optimistic concurrency control.

The pool only guarantees: each thread gets its own connection. What you do with that connection is your problem.

### Security Considerations

Connection pools don't protect against SQL injection. Always use prepared statements. The pool just ensures your injection attack doesn't open 10,000 database connections.
```

**Mechanics**:
- Headings in Title Case (H3 level)
- Short paragraphs (2-4 sentences)
- **Bold for warnings or load-bearing claims, not because the template wants a bold line**
- Code examples for every claim

**Main Thread rules**:
- Prefer direct statements for the failure modes you demonstrate; use precise hedging when behavior depends on version, traffic shape, or environment
- Production consequences, not theoretical risks

---

### 7. Verification

**Purpose**: Prove the system works as claimed

**Allowed formats**:
1. Real curl commands with expected output
2. JUnit tests with explicit assertions
3. For AI/ML: Threshold checks with explanation

**Required**:
- Actual commands/code that readers can run
- Expected output shown
- Explanation of what's being verified

**Format**:
```markdown
### Testing Connection Pool Limits

Start the application and verify pool behavior:

```bash
# Check current pool statistics
curl http://localhost:8080/q/health/ready | jq '.checks[] | select(.name == "Database connections health check")'
```

Expected output:
```json
{
  "name": "Database connections health check",
  "status": "UP",
  "data": {
    "active": 2,
    "idle": 3,
    "max": 20
  }
}
```

This confirms the pool has 5 total connections (2 active, 3 idle) and can grow to 20.

### Integration Test

```java
@QuarkusTest
class ConnectionPoolTest {
    
    @Test
    void shouldFailFastWhenPoolExhausted() {
        // Hold all 20 connections
        List<CompletableFuture<Void>> futures = IntStream.range(0, 25)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                // Each call holds a connection for 5 seconds
                given().when().get("/users/1").then().statusCode(200);
            }))
            .toList();
        
        // At least 5 requests should fail (20 max connections)
        long failures = futures.stream()
            .map(f -> {
                try {
                    f.get(10, TimeUnit.SECONDS);
                    return false;
                } catch (Exception e) {
                    return true;
                }
            })
            .filter(failed -> failed)
            .count();
        
        assertTrue(failures >= 5, "Expected pool exhaustion to cause failures");
    }
}
```

This test verifies fail-fast behavior. Without the 3-second timeout, this test would hang indefinitely.
```

**O'Reilly rules**:
- Use `code font` for commands, methods, classes
- Show actual output (don't say "you'll see a response")
- Explain assertions (why `failures >= 5`?)

**The Main Thread rules**:
- No "should work" without proof
- For AI systems: explain why deterministic output isn't possible
- Verification must match production hardening claims

---

### 8. Conclusion (1 paragraph + 1 sentence)

**Format**:
```markdown
## Conclusion

We've built a connection pool configuration that protects both your application and database. The 3-second timeout prevents cascade failures, the 20-connection limit protects PostgreSQL, and the 5-connection minimum avoids cold-start latency. Most importantly, we understand what the pool guarantees (connection isolation) and what it doesn't (query correctness).

The complete code is available on [GitHub](https://github.com/username/repo).
```

**O'Reilly rules**:
- One recap paragraph only
- Link to GitHub with descriptive anchor text
- No new ideas in conclusion

**The Main Thread rules**:
- Tie back to the **stakes** from problem framing in **one** short line (not a second full framing section)
- One closing sentence maximum (GitHub link counts as that sentence when formatted as its own line)

**Do not in the conclusion**:
- Re-list every configuration key or repeat property explanations
- Paste guarantee/limit checklists already stated in implementation or hardening
- Summarize each section in sequence ("First we… then we…")

**Do in the conclusion**:
- State what the reader built and what production risk it addresses, without re-proving it
- Link to GitHub with descriptive anchor text

---

## Formatting Mechanics (O'Reilly-Inspired Cleanup)

Use this section after the prose exists. These rules keep the article clean in Markdown, Substack, and book-ish contexts. They must not make the article sound formal, neutral, or like generated documentation.

### Headings
- **Article title (H1)**: Title Case
- **Major sections (H2)**: Title Case ("Problem Framing", "Production Hardening")
- **Subsections (H3)**: Sentence case ("What happens under load")
- Never stack headings (always have text between them)
- No code formatting in headings

### Lists
**Use bullets only for**:
- Prerequisites
- Extension lists
- CLI command options
- Brief enumerations (3-5 items)

**Use numbered lists for**:
- Sequential steps
- Ordered procedures
- "Must address N of these" lists

**Never use bullets for**:
- Explanatory content (use paragraphs)
- Code alternatives (use paragraphs + code blocks)
- Pros/cons (use flowing paragraphs with **bold** labels)

### Code
- Always include package declaration
- Always include all imports
- Use `code font` for: classes, methods, annotations, properties, commands
- Use **`bold code font`** for user input
- Use *`italic code font`* for placeholders (rare in your tutorials)

### Typography
- **Bold** for warnings, important constraints, and load-bearing claims
- *Italic* for first use of technical terms (optional, use sparingly)
- `Code font` for anything executable or configuration
- Use em dashes sparingly. Do not remove a natural sentence just to satisfy punctuation neatness.
- Serial comma always (this, that, and the other)

### Links
- Descriptive anchor text: "[Spring documentation](url)" not "[here](url)"
- Link to official docs when mentioning frameworks
- Link to your previous articles when building on them
- Always include GitHub repo at end

---

## Voice Boundary

Voice belongs primarily to **authentic-voice**. This structure skill only blocks prose that weakens the tutorial.

### Prohibited Language and Shapes
- ❌ Hype: "powerful", "amazing", "incredible"
- ❌ Vague hedging that dodges responsibility: "things may vary", "it depends" with no specifics
- ❌ Academic: "one can observe", "it has been noted"
- ❌ Rhetorical hook questions: "Have you ever wondered...?"
- ❌ Motivational: "Let's dive in!", "Exciting stuff!"
- ❌ Visible template phrases: "This section covers", "The key insight is", "Production hardening considerations include", "What this guarantees"
- ❌ Documentation smell: every class introduced the same way, too many noun phrases, bullets where a paragraph would read better

### Required Voice
- ✅ Direct where the article already proved the behavior (code, commands, config keys)
- ✅ Authoritative steps: "Configure it this way" not endless "you might try"
- ✅ Honest scope when behavior is version-, environment-, or load-dependent: "With Quarkus 3.x as written here, …" or "Under load, …" is allowed and preferred over fake certainty
- ✅ Production-focused: consequences over cheerleading
- ✅ Globally readable: Simple words, short sentences
- ✅ Personal when useful: first-person preference, dry criticism, or a short aside is allowed if it clarifies the trade-off

### Tense
- **Present tense** for code behavior: "This method returns..."
- **Future/present** for tutorial steps: "We'll create..." or "We create..."
- **Past tense** for war stories: "I debugged this at 3am..."
- **Be consistent within sections**

### Pronouns
- **"We"** for collaborative building: "We'll implement..."
- **"You"** for direct instruction: "You need Java 21 installed"
- **"Your"** for reader's systems: "Your application crashes when..."
- **"I"** for preference, experience, and judgment: "I would keep this boring"
- Never use: "one should", "developers must"

---

## Non-Negotiable Rules

### Code Standards
1. Every code block must compile
2. Every import must be shown
3. Every concept mentioned must appear in code
4. No placeholders (`// TODO`, `// implementation here`)
5. Transaction boundaries must be visible
6. Flag experimental features explicitly

### Content Standards
1. Every decision must have a reason
2. Every guarantee must state its limits
3. Failure modes must be acknowledged
4. No images unless explicitly requested
5. No Markdown tables (Main Thread / Substack and related surfaces do not render them reliably; use lists, definition-style lines, or code blocks)
6. War stories are optional (remove if asked)

### Production Focus
1. Demo values are forbidden (use production-appropriate settings)
2. "Works on my machine" is insufficient
3. Explain what breaks under load
4. Explain what happens when dependencies fail
5. Security is a requirement, not a nice-to-have

---

## Quality Checklist

Before delivering a tutorial, verify:

**Structure**
- [ ] Problem framing addresses production failure
- [ ] Prerequisites are honest about assumptions
- [ ] Every component has context → code → analysis
- [ ] Major risks have a **single** primary explanation; later sections only add new detail or short pointers (see **Information budget**)
- [ ] Three+ implementation components use **at least two** different analysis shapes (see **Variation contract** under Implementation)
- [ ] Production hardening addresses the real risks this article creates
- [ ] Verification uses real commands/tests
- [ ] Conclusion states outcome and stakes without re-listing config or body checklists

**Code**
- [ ] Every code block compiles
- [ ] All imports shown
- [ ] Package declarations included
- [ ] No placeholders
- [ ] Configuration uses production values

**Formatting mechanics**
- [ ] Headings follow capitalization rules
- [ ] Lists used only for enumerations/prerequisites
- [ ] Code font applied to all technical elements
- [ ] Links use descriptive anchors
- [ ] Numbers spelled out (zero-nine) or numerals (10+)
- [ ] Serial commas throughout
- [ ] No Markdown tables for Main Thread / Substack (use lists or `**term** — description` lines)

**Voice**
- [ ] No hype or motivational language
- [ ] No vague hedging; precise hedging OK where behavior truly varies by version, environment, or load
- [ ] No rhetorical hook questions in the opening; teaching Q&A inside explanations is fine when answered immediately
- [ ] Simple, globally readable English
- [ ] Could only be written by someone with production experience
- [ ] Does not sound like vendor docs, an O'Reilly chapter, or an exposed checklist
- [ ] First person, dry humor, or criticism appears where it improves judgment or flow, not everywhere

**Production Focus**
- [ ] Failure modes explicit
- [ ] Limits clearly stated
- [ ] Trade-offs explained
- [ ] Alternative approaches addressed
- [ ] Security considered

---

## Example Section Breakdown

Here's a complete component section showing all elements:

```markdown
## Implementing Graceful Shutdown

When you kill a Java process, active requests get terminated mid-flight. Clients see broken connections, databases see uncommitted transactions, and your logs fill with stack traces. **Graceful shutdown gives in-flight requests time to complete before the JVM exits.**

```java
package com.example.shutdown;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class GracefulShutdownManager {
    
    private static final Logger LOG = Logger.getLogger(GracefulShutdownManager.class);
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    public void requestStarted() {
        activeRequests.incrementAndGet();
    }
    
    public void requestCompleted() {
        activeRequests.decrementAndGet();
    }
    
    void onShutdown(@Observes ShutdownEvent event) {
        LOG.info("Shutdown initiated. Active requests: " + activeRequests.get());
        
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
        
        while (activeRequests.get() > 0 && System.currentTimeMillis() < deadline) {
            try {
                LOG.info("Waiting for " + activeRequests.get() + " requests to complete");
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (activeRequests.get() > 0) {
            LOG.warn("Forcing shutdown with " + activeRequests.get() + " active requests");
        } else {
            LOG.info("All requests completed. Shutting down gracefully.");
        }
    }
}
```

The JVM waits up to 30 seconds for active requests to finish. If they complete, shutdown is clean. It does **not** guarantee that every request finishes: a 60-second request still dies at the 30-second mark. The cap exists so a bad deploy cannot block shutdown forever.

Under stress, if you process 1,000 requests per second and shutdown takes 30 seconds, you can strand tens of thousands of in-flight requests. **Drain traffic at the load balancer first**, not only inside the app.

Quarkus already has graceful shutdown; this adds visible request counts. In production you would wire that to the load balancer health check so new traffic stops before you trigger shutdown.

Prefer this flowing style by default. Use an explicit guarantee/limit label only when the topic is safety-critical and a plain paragraph would hide the boundary.

### Configuration

Add to `application.properties`:

```properties
quarkus.shutdown.timeout=30s
```

This configures Quarkus's built-in shutdown timeout. Our manager works alongside it, adding request tracking.
```

---

## Final Validation

A tutorial is ready when:

1. **A senior engineer** can skim headings and get value in 2 minutes
2. **A junior engineer** can follow every step and understand why
3. **Someone at 2am** can use it to fix a production issue
4. **Someone without production experience** could not have written it

If any of these fail, the tutorial is not ready.

---

## Verification Standards

Every tutorial must make it clear how the reader knows the implementation works.

Prefer verification that checks behavior, not just syntax.

For deterministic systems, verify:
- endpoint responses
- database state
- configuration effects
- expected logs only when they are stable and useful

For AI and probabilistic systems, do not verify exact wording unless the system is deterministic.

Instead verify:
- schema shape
- required fields
- threshold behavior
- fallback behavior
- retry behavior
- latency expectations
- security boundaries
- permission enforcement
- failure handling

---

## Output Mode Rules

- Tutorial mode: full end-to-end article
- Quick guide mode: shorter structure, same technical rigor
- Architecture mode: fewer code listings, more trade-off analysis
- Review mode: concise defect lists are allowed
- Rewrite mode: preserve substance, improve structure and voice

Published tutorial mode should prefer flowing paragraphs over lists.
Review mode may use compact lists when clarity improves.
