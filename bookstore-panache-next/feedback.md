# Hibernate Panache Next — Engineering Feedback

Summary of lessons learned and implementation feedback from building the bookstore-panache-next demo (Quarkus 3.31.3). Intended for the Quarkus/Hibernate Panache Next engineering team.

**Scope:** The first sections reflect pain points and findings from the article and demo. The section **Broader API Review & Improvement Suggestions** (items 14–23) reviews the Panache Next API in general and suggests improvements beyond the tutorial scope.

---

## What Works Well

- **Unified API:** Single extension covering blocking/reactive and managed/stateless removes the old two-headed ORM vs Reactive Panache split. One entity model, multiple repository styles.
- **Type-safe queries:** `@Find` and `@HQL` with build-time validation (hibernate-processor) catch typos and invalid property names before runtime. Huge improvement over string-only JPQL.
- **Nested repository interfaces:** Keeping `Author.Repo` and `Author.ReadRepo` on the entity keeps the model and persistence contract in one place and makes injection (`Author.Repo`) clear.
- **Metamodel accessor:** `Author_.repo()` is useful in services and static-style code where injection is awkward; good alternative to injecting the repo everywhere.
- **Explicit stateless model:** Once you know the rules, “no dirty checking, explicit insert/update” is predictable and matches Jakarta Data’s mental model.

---

## API Weirdnesses & Pain Points

### 1. **Stateless reactive: two ways to run a transaction**

Stateless reactive operations can be wrapped in either:

- `SessionOperations.withStatelessTransaction(() -> …)`
- `@WithTransaction(stateless = true)` (mentioned in the article)

Having two mechanisms for the same concept is confusing. Recommendation: document one preferred approach and deprecate or clearly subordinate the other, or unify behind a single abstraction (e.g. make `@Transactional` session-type-aware in a future release).

**From the demo:** Every stateless reactive call in `BookResource` uses the lambda form; the service comment mentions the annotation alternative.

```java
// BookResource.java — every endpoint wraps in SessionOperations
@GET
public Uni<List<Book>> list() {
    return SessionOperations.withStatelessTransaction(() -> repo.findAll().list());
}

// AuthorMixedModeService.java — comment shows the other option
// Reactive stateless — use SessionOperations or @WithTransaction(stateless = true)
public Uni<List<Author>> getCatalogByCountry(String country) {
    return SessionOperations.withStatelessTransaction(() -> readRepo.catalog(country));
}
```

### 2. **`findById` on stateless reactive returns entity, not `Uni<Entity>`**

On reactive stateless repositories, `findById(id)` returns `Book` (or the entity type) inside the lambda, while other methods return `Uni<List<Book>>`, `Uni<Book>`, etc. This asymmetry:

- Breaks the “everything is Uni” expectation in reactive code.
- Forces different handling for “get one” vs “get many” in the same resource layer.

**Suggestion:** Align stateless reactive `findById` with the rest of the API and return `Uni<Book>` (or `Uni<Optional<Book>>`), so all repository calls can be composed consistently.

**From the demo:** `findById` is synchronous inside the lambda; `findPublishedSince` returns `Uni<List<Book>>`. The lambda must do null check and then call `repo.update(existing).replaceWith(existing)` to get back into `Uni`-land.

```java
// BookResource.java — list() / since(): repo returns Uni, composed directly
@GET
public Uni<List<Book>> list() {
    return SessionOperations.withStatelessTransaction(() -> repo.findAll().list());
}
@GET
@Path("/since/{year}")
public Uni<List<Book>> since(@PathParam("year") int year) {
    return SessionOperations.withStatelessTransaction(() -> repo.findPublishedSince(year));
}

// update(): findById returns Book, not Uni<Book> — different shape
@PUT
@Path("/{id}")
public Uni<Book> update(@PathParam("id") Long id, Book patch) {
    return SessionOperations.withStatelessTransaction(() -> {
        Book existing = repo.findById(id);   // synchronous; other repo methods return Uni
        if (existing == null)
            return Uni.createFrom().failure(new NotFoundException());
        existing.title = patch.title;
        existing.year = patch.year;
        return repo.update(existing).replaceWith(existing);
    });
}
```

### 3. **`persist()` returns `Uni<Void>` on reactive repositories**

Reactive `persist()` returns `Uni<Void>`, so call sites need `.replaceWith(entity)` to pass the persisted entity along (e.g. for HTTP 201 with body). The article already notes the team is considering `Uni<Entity>`. We strongly support that: returning the persisted entity (with generated ID, etc.) would simplify resources and remove boilerplate.

**From the demo:** Reactive create has to chain `.replaceWith(a)` so the caller gets the entity back.

```java
// AuthorMixedModeService.java — persist() returns Uni<Void>, so we manually re-attach the entity
@WithTransaction
public Uni<Author> createAuthorReactive(String name, String country) {
    Author a = new Author();
    a.name = name;
    a.country = country;
    return a.managedReactive().persist().replaceWith(a);  // would be nicer if persist() returned Uni<Author>
}
```

```java
// BookResource.java — insert() returns Uni<Book>, so no workaround; contrast with persist()
@POST
public Uni<Response> create(Book book) {
    return SessionOperations.withStatelessTransaction(
            () -> repo.insert(book).map(b -> Response.status(201).entity(b).build()));
}
```

### 4. **SessionOperations lives under `.runtime`**

`io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations` is part of the public contract developers must use for stateless reactive. The `.runtime` segment suggests “internal” and makes the API feel unstable. If this is the long-term entry point for stateless transactions, consider exposing it from a stable, non-runtime package (e.g. `io.quarkus.hibernate.reactive.panache.common.SessionOperations` or equivalent) and document it as the supported API.

**From the demo:** Both the Book resource and the mixed-mode service import from the runtime package.

```java
// BookResource.java, AuthorMixedModeService.java
import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
```

### 5. **Managed vs stateless transaction annotations**

- **Blocking managed:** `@Transactional` (Jakarta).
- **Reactive managed:** `@WithTransaction` (no `stateless = true`).
- **Reactive stateless:** `SessionOperations.withStatelessTransaction` or `@WithTransaction(stateless = true)`.

The rule “use `@WithTransaction` for managed reactive only; for stateless use this other thing” is easy to get wrong. A single, consistent way to express “run this in a stateless reactive transaction” (whether annotation or API) would reduce mistakes. Unifying behind a future `@Transactional` that understands session type would help.

**From the demo:** One service uses all three styles side by side.

```java
// AuthorMixedModeService.java — three different transaction styles on the same bean
// Blocking managed
@Transactional
public Author createAuthor(String name, String country) { ... }

// Reactive managed
@WithTransaction
public Uni<Author> createAuthorReactive(String name, String country) { ... }

// Reactive stateless — no annotation, explicit SessionOperations
public Uni<List<Author>> getCatalogByCountry(String country) {
    return SessionOperations.withStatelessTransaction(() -> readRepo.catalog(country));
}
```

### 6. **Stateless blocking and `@Transactional`**

For per-entity stateless blocking (e.g. `entity.statelessBlocking().insert()`), each call runs in its own transaction. Wrapping the caller in `@Transactional` is wrong and can cause subtle bugs. This is a common pitfall and deserves prominent documentation and, if possible, a clear error or warning when `@Transactional` is used in a context that triggers per-call stateless operations.

**From the demo:** Stateless blocking methods intentionally have no `@Transactional`; adding it would be a mistake.

```java
// AuthorMixedModeService.java — no @Transactional here; each insert/update is its own transaction
// Blocking stateless — explicit insert/update; each stateless operation runs in its own transaction
public void bulkImport(List<Author> authors) {
    authors.forEach(a -> a.statelessBlocking().insert());
}

public void bulkUpdateCountry(List<Author> authors, String country) {
    authors.forEach(a -> {
        a.country = country;
        a.statelessBlocking().update();
    });
}
```

Contrast with blocking managed on the same class, which must use `@Transactional`:

```java
@Transactional
public Author createAuthor(String name, String country) {
    Author a = new Author();
    a.name = name;
    a.country = country;
    repo.persist(a);
    return a;
}
```

---

## Testing

### 7. **Reactive stateless tests are verbose**

Blocking managed tests use `@TestTransaction` and roll back cleanly. Reactive stateless tests require:

- `SessionOperations.withStatelessTransaction(...)` in every test, and
- Either `.await().indefinitely()` or `@RunOnVertxContext` with `UniAsserter`.

The article notes the team is working on improving this. Suggestions:

- A test-level annotation or rule that provides a stateless reactive session and rollback (e.g. `@TestStatelessTransaction` or similar).
- Test utilities or extensions that wrap “run this Uni in a stateless transaction and assert” so tests don’t repeat the same boilerplate.

### 8. **Inconsistent test styles in the demo**

The article shows Book tests using `.await().indefinitely()`, while the actual project uses `@RunOnVertxContext` and `UniAsserter`. Aligning the docs with the recommended pattern (e.g. Vert.x context + UniAsserter) would avoid confusion.

---

## Documentation & Conventions

### 9. **Package and artifact names**

Classic Panache is `io.quarkus.hibernate.orm.panache`; Panache Next is `io.quarkus.hibernate.panache`. The “Next” is in the extension name, not the package. This is easy to get wrong when copying from old examples or docs. A short “Migration / naming” section in the Panache Next guide (package map, artifact map, one sentence per) would help.

### 10. **Annotation processor is required**

Type-safe queries require the Hibernate annotation processor (`hibernate-processor`). If it’s not on the compiler plugin path, `@Find`/`@HQL` don’t get processed and the build can fail in non-obvious ways. The setup should be highlighted in the “Getting started” section and ideally enforced or checked by the Quarkus tooling (e.g. a build analizer that warns when Panache Next is present but the processor is not).

### 11. **EAGER / JOIN FETCH in stateless**

Stateless has no lazy loading; associations must be EAGER or fetched via `JOIN FETCH`. This is mentioned in the article but is critical. A brief “Stateless mode: no lazy loading” callout in the main guide, with a one-line example of `FetchType.EAGER` and a short HQL `JOIN FETCH` example, would prevent a lot of `LazyInitializationException` debugging.

---

## Entity Model

### 12. **Two base types: `PanacheEntity` vs `WithId.AutoLong`**

- `Author` extends `PanacheEntity` (instance methods like `persist()`, managed-style).
- `Book` extends `WithId.AutoLong` (repository-only, no instance `persist()`).

Both are valid but represent different patterns. Newcomers may not know when to use which. A short “Choosing an entity base” section (e.g. “Use PanacheEntity when you want instance methods and managed style; use WithId when you want repository-only and stateless-friendly entities”) would set clear guidance.

### 13. **Naming of repository interfaces**

The demo uses `Repo` and `ReadRepo`. There’s no convention for “blocking managed” vs “reactive stateless” interface names. Naming guidelines (or even suggested suffixes like `Repo` vs `ReadRepo` / `ReactiveRepo`) in the docs would make multi-mode entities easier to read and maintain.

---

## Minor / Nice-to-Have

- **Parameter names in `@HQL`:** In the demo, `:country` and `:c` are both used. Consistency (e.g. always use full parameter names) would improve readability; the docs could recommend a style.
- **Batch stateless inserts:** `bulkImport(List<Author>)` with `authors.forEach(a -> a.statelessBlocking().insert())` runs one transaction per entity. A repository-level batch insert (e.g. `insertAll(Iterable<Author>)`) would be more efficient and would match the “stateless for bulk” use case.
- **Unify transaction model before GA:** Consolidating around `@Transactional` (or one clear alternative) for both blocking and reactive, with optional session-type hints, would simplify the mental model and reduce the “which annotation do I use?” burden.

---

## Broader API Review & Improvement Suggestions

The following points go beyond the article and demo: they address the Panache Next API in general, based on patterns seen in the codebase and comparison with classic Panache and common repository APIs.

### 14. **Repository type hierarchy is verbose**

To use reactive stateless you extend `PanacheRepository.Reactive.Stateless<Book, Long>`. The full path is long and the ID type is repeated (entity already has an ID from `WithId.AutoLong`). Blocking managed is just `PanacheRepository<Author>` (ID often inferred).

**Suggestion:** Consider shorter type aliases or a flatter hierarchy, e.g. `ReactiveStatelessRepository<Book, Long>` in a single type, or a way to infer the ID from the entity so `<Book>` suffices. Document the full matrix (blocking/reactive × managed/stateless) in one place so the four combinations are easy to pick.

```java
// Current — verbose
public interface Repo extends PanacheRepository.Reactive.Stateless<Book, Long> { ... }

// Blocking managed is shorter
public interface Repo extends PanacheRepository<Author> { ... }
```

### 15. **`findAll().list()` is a two-step for “get all”**

The demo uses `repo.findAll().list()` for “list all”. Classic Panache had `listAll()`. The two-step call is consistent with a query-builder style (findAll() returns something pageable/filterable) but forces an extra method call for the very common “get all” case.

**Suggestion:** Offer a convenience method such as `listAll()` that returns `List<Entity>` (blocking) or `Uni<List<Entity>>` (reactive), implemented as `findAll().list()`, so simple list endpoints don’t need the extra step. Alternatively, document `findAll().list()` as the canonical “get all” and ensure `findAll()` is discoverable (e.g. in IDE completion).

```java
// AuthorResource.java
return repo.findAll().list();   // two steps; listAll() would be one
```

### 16. **Return type conventions: `Optional` vs single entity**

Blocking repo uses `Optional<Author> findByName(String name)` (optional for “maybe missing”). For “get by ID”, the demo uses `findById(id)` returning the entity or null (and `deleteById` returns void). Inconsistent use of `Optional` vs null for “one or zero” results can surprise developers.

**Suggestion:** Standardise “one or zero” returns: either recommend `Optional<Entity>` for all such repository methods (including `findById`) or document clearly when to expect null vs Optional. Same for reactive: `Uni<Optional<Book>>` vs `Uni<Book>` (null when not found).

### 17. **Instance API naming and discoverability**

Panache Next exposes per-entity session-style entry points: `statelessBlocking()`, `managedReactive()`, `statelessReactive()`. They are only available on entities that extend `PanacheEntity` (e.g. `Author`), not on `WithId`-only entities (e.g. `Book`). The names are explicit but a bit long; newcomers may not know they exist.

**Suggestion:** Document the “instance API” in one place (which base classes get it, when to use which). Consider shorter or more discoverable names if the API stabilises (e.g. `blockingStateless()` vs `statelessBlocking()` — align order with “session type then I/O” or “I/O then session type”). A table “Entity base → available instance methods” would help.

### 18. **Type-safe pagination and sort**

The demo uses custom `@HQL` for “order by” and no pagination. Classic Panache had `PanacheQuery` with `page()`, `range()`, `list(Sort)`. For Panache Next, it’s unclear how to express “find by country, page 2, 20 per page, sort by name” in a type-safe way with `@Find`/`@HQL` only.

**Suggestion:** Document or add type-safe support for pagination and sort in repository interfaces, e.g. `Page<Author> findByCountry(String country, Pageable pageable)` or parameters on `@Find`/`@HQL` (e.g. `Sort` / `Page` as method parameters validated by the processor). Without it, users fall back to string HQL for every paged/sorted query.

### 19. **Metamodel class naming**

The generated metamodel is `Author_` (entity name + underscore). That’s a familiar JPA convention. For entities with non-trivial names (e.g. `Order` → `Order_`), document that the metamodel is always `EntityName_` and that it’s generated at build time so IDEs may not resolve it until after compile.

**Suggestion:** One sentence in the “Metamodel accessor” section: “The generated class is `<EntitySimpleName>_` (e.g. `Author_`, `Book_`) and is created by the Hibernate processor during compilation.”

### 20. **Count and exists**

The demo doesn’t use `count()` or `existsById()`. For REST and services, “total count for this filter” and “does this ID exist?” are common. If these are provided by the base repository, they should be called out in the guide; if not, adding `count()` and `existsById()` (and reactive equivalents) would align Panache Next with Jakarta Data and Spring Data expectations.

### 21. **Nested vs top-level repository interfaces**

The demo nests repositories inside the entity (`Author.Repo`, `Author.ReadRepo`). This works well for “one entity, one or two repos”. For larger apps, teams might want top-level repository interfaces (e.g. `AuthorRepository`, `AuthorReadRepository`) that still target `Author`. The article mentions “you can use a top-level interface if you prefer” but doesn’t show how (e.g. same `extends PanacheRepository<Author>`, injection by the interface type).

**Suggestion:** Add a short “Nested vs top-level repositories” subsection with a minimal top-level example (interface in its own file, same `@Find`/`@HQL`, inject `AuthorRepository`) so both styles are first-class in the docs.

### 22. **Consistent parameter naming in `@HQL`**

The demo uses both `:country` and `:c` in `@HQL`. Method parameters are bound by name; short names like `:c` are valid but hurt readability. Recommend a convention (e.g. “use the same name as the method parameter”) and apply it in all examples.

```java
// Author.java — mixed
@HQL("where country = :country order by name")
List<Author> findByCountry(String country);
@HQL("where country = :c order by name")
Uni<List<Author>> catalog(String c);
```

### 23. **Migration from classic Panache**

Teams migrating from classic Panache will look for a clear mapping: `Person.find("name", name)` → ?, `Person.listAll()` → ?, `PanacheEntity` (classic) → `PanacheEntity` (Next) or `WithId`? A “Migration from classic Panache” section (or appendix) with a side-by-side table (classic API → Panache Next equivalent, including package and artifact changes) would reduce friction and avoid copy-paste from old docs.

---

## Summary Table

| #   | Area              | Issue                                      | Severity | Suggestion                                              |
|-----|-------------------|--------------------------------------------|----------|---------------------------------------------------------|
| 2   | API consistency   | `findById` returns entity, not `Uni`      | High     | Return `Uni<Entity>` (or `Uni<Optional<Entity>>`)       |
| 3   | API consistency   | `persist()` returns `Uni<Void>`            | Medium   | Consider `Uni<Entity>` (already under consideration)   |
| 4   | API surface       | SessionOperations in `.runtime` package    | Medium   | Expose from stable package, document as public         |
| 1,5 | Transaction model | Two ways / wrong annotation for stateless | Medium   | Unify approach; document; consider warning/error         |
| 6   | Transaction model | Stateless blocking + `@Transactional` trap| Medium   | Document clearly; consider warning/error                |
| 7   | Testing           | Reactive stateless test boilerplate        | Medium   | Test annotation or utility for stateless tx             |
| 9–11, 19, 21–23 | Docs | Processor, EAGER, naming, metamodel, migration | Low  | Callouts; “Choosing entity base”; nested vs top-level; migration table |
| 8   | Docs              | Test style (UniAsserter vs await)          | Low      | Align article with recommended pattern                  |
| 12–13 | Entity model    | Base type and repo naming                  | Low      | “Choosing entity base”; naming guidelines               |
| 10  | Convenience       | Batch insert for stateless                 | Low      | Repository-level batch insert API                      |
| 14  | API design        | Repository type hierarchy verbose          | Low      | Shorter aliases; document 4-way matrix                 |
| 15  | API design        | `findAll().list()` two-step                | Low      | Optional `listAll()` or document as canonical           |
| 16  | API design        | Optional vs null for “one or zero”         | Low      | Standardise and document return conventions             |
| 17  | API design        | Instance API discoverability               | Low      | Document “instance API” table; consider naming         |
| 18  | API design        | Type-safe pagination/sort                  | Low      | Document or add Page/Sort in type-safe repos            |
| 20  | API design        | Count / existsById                          | Low      | Document if present; add if missing                     |
| 22  | Conventions       | @HQL parameter names                        | Low      | Recommend “match method parameter” in docs              |

---

Thanks to the team for Panache Next and for the clear tutorial. The direction (unified API, type-safe queries, managed + stateless) is strong; the feedback above is intended to reduce friction and surprises before GA.
