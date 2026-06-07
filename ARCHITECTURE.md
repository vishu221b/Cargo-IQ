# cargo-iq — Architecture

This document explains *why* the code is laid out the way it is, not only
*how*. It assumes you know Spring Boot and Java reasonably well but maybe
haven't worked in a strict hexagonal codebase before. Read it once end-to-end,
then keep it open in a tab while you extend the app.

---

## 1. The shape: Hexagonal / Ports & Adapters

```
                          ┌────────────────────────────────────────┐
                          │           ADAPTERS / INBOUND           │
   curl, browser ───────► │   REST controllers (adapter/in/web)    │
                          │                                        │
   Claude Desktop  ─────► │   MCP tools       (adapter/in/mcp)     │
   Cursor, etc.           └─────────────┬──────────────────────────┘
                                        │  (depends on)
                                        ▼
                          ┌────────────────────────────────────────┐
                          │            APPLICATION                 │
                          │   inbound ports:                       │
                          │     IngestDocumentUseCase              │
                          │     AnswerQueryUseCase                 │
                          │     LookupIncotermUseCase, ...         │
                          │                                        │
                          │   services (implementations):          │
                          │     IngestDocumentService              │
                          │     AnswerQueryService                 │
                          │     LookupIncotermService, ...         │
                          │                                        │
                          │   outbound ports (SPIs):               │
                          │     DocumentRepository                 │
                          │     VectorStorePort                    │
                          │     ChatModelPort                      │
                          │     DocumentParserPort                 │
                          │     ReferenceDataPort                  │
                          └─────────────┬──────────────────────────┘
                                        │  (depends on)
                                        ▼
                          ┌────────────────────────────────────────┐
                          │              DOMAIN                    │
                          │   Document, DocumentChunk, Incoterm,   │
                          │   HsCode, Citation, Answer, Query,     │
                          │   ShipmentMetadata, exception types    │
                          │                                        │
                          │   Pure POJOs. No Spring. No JPA.       │
                          └────────────────────────────────────────┘
                                        ▲
                                        │  (depends on)
                          ┌─────────────┴──────────────────────────┐
                          │          ADAPTERS / OUTBOUND           │
                          │   adapter/out/persistence/jpa          │ ─► Postgres
                          │   adapter/out/persistence/vector       │ ─► pgvector
                          │   adapter/out/ai                       │ ─► OpenAI / Ollama
                          │   adapter/out/parser                   │ ─► text/regex/Tika
                          │   adapter/out/reference                │ ─► hs-codes.csv
                          └────────────────────────────────────────┘
```

Read the arrows as **"depends on"**. Crucially, the application layer **owns
both kinds of ports**:

- **Inbound ports** (`application/port/in/*`) are interfaces the inbound
  adapters call into — they're the application's public API.
- **Outbound ports** (`application/port/out/*`) are interfaces the application
  needs implementations of, written in the application's vocabulary. Outbound
  adapters implement them.

This is the Dependency Inversion Principle made structural. The application
never knows about Postgres, pgvector, OpenAI, Jakarta servlets, or MCP. Every
one of those is an adapter that *plugs into* the application.

---

## 2. Why hexagonal, given Spring already gives you layers?

A common objection: "Spring Boot's controller → service → repository is
already a layered architecture. Why all the ceremony?"

The objection underestimates how much Spring's "obvious" layout leaks
infrastructure. In a typical Spring app:

- The service injects a Spring Data `JpaRepository` directly — JPA's API
  shape ripples up into the service signature (`Page<>`, `Pageable`,
  `Optional<Entity>`).
- The controller passes JPA entities through to JSON serialization — now
  Hibernate proxies, lazy-loading, and the JSON shape are coupled to the
  database schema.
- Swapping persistence stores or model vendors means rewriting half the
  service tier.

`cargo-iq` separates these explicitly:

- The service depends on `DocumentRepository` — *our* interface, written in
  domain terms. The Spring Data interface is package-private to the JPA
  adapter and never escapes.
- DTOs ↔ domain ↔ entity is three explicit mappings, each one-way, each
  obvious to read.
- Swapping pgvector for Qdrant means rewriting one class — `PgVectorAdapter`
  — and changing one starter in `pom.xml`. Nothing else moves.

For a RAG app this matters especially because **the infrastructure side is
where the project will evolve fastest**: re-rankers, multi-query strategies,
prompt caching, different model providers, hybrid retrieval. You want a wall
between that churn and your business rules.

---

## 3. Package map (every package, why it exists)

```
io.cargoiq
│
├── CargoIqApplication.java          ── boring on purpose; bootstrap only
│
├── domain/                          ── INNER ring. Zero framework deps.
│   ├── model/
│   │   ├── Document.java            ── aggregate root for ingest
│   │   ├── DocumentChunk.java       ── value: a text slice for embedding
│   │   ├── DocumentType.java        ── enum: BILL_OF_LADING, etc.
│   │   ├── Incoterm.java            ── enum: 11 INCOTERMS 2020 rules
│   │   ├── HsCode.java              ── value: HS tariff code
│   │   ├── ShipmentMetadata.java    ── value: extracted structured fields
│   │   ├── Citation.java            ── value: retrieval hit, for grounding
│   │   ├── Answer.java              ── value: RAG output (text + citations)
│   │   └── Query.java               ── value: question + retrieval filters
│   └── exception/
│       ├── DomainException.java
│       ├── DocumentNotFoundException.java
│       ├── IncotermNotFoundException.java
│       └── HsCodeNotFoundException.java
│
├── application/                     ── MIDDLE ring. Use cases.
│   ├── port/in/                     ── use case contracts (driving side)
│   │   ├── IngestDocumentUseCase.java
│   │   ├── AnswerQueryUseCase.java
│   │   ├── LookupIncotermUseCase.java
│   │   ├── LookupHsCodeUseCase.java
│   │   └── ListDocumentsUseCase.java
│   ├── port/out/                    ── SPIs (driven side)
│   │   ├── DocumentRepository.java
│   │   ├── VectorStorePort.java
│   │   ├── ChatModelPort.java
│   │   ├── DocumentParserPort.java
│   │   └── ReferenceDataPort.java
│   └── service/                     ── implementations (annotated @Service)
│       ├── IngestDocumentService.java
│       ├── AnswerQueryService.java
│       ├── LookupIncotermService.java
│       ├── LookupHsCodeService.java
│       └── ListDocumentsService.java
│
├── adapter/
│   ├── in/                          ── DRIVING adapters
│   │   ├── web/                     ── REST controllers
│   │   │   ├── DocumentController.java
│   │   │   ├── QueryController.java
│   │   │   ├── dto/                 ── request/response shapes; explicit maps
│   │   │   └── advice/              ── @RestControllerAdvice — domain ex → HTTP
│   │   └── mcp/                     ── MCP server's @Tool-annotated beans
│   │       ├── tools/               ── search, lookup_incoterm, ...
│   │       ├── prompts/             ── stub (see roadmap)
│   │       └── resources/           ── stub (see roadmap)
│   │
│   └── out/                         ── DRIVEN adapters
│       ├── persistence/
│       │   ├── jpa/
│       │   │   ├── DocumentEntity.java
│       │   │   ├── DocumentJpaRepository.java  (Spring Data — internal)
│       │   │   └── DocumentRepositoryAdapter.java  (implements port)
│       │   └── vector/
│       │       └── PgVectorAdapter.java        ── wraps Spring AI VectorStore
│       ├── ai/
│       │   └── ChatModelRouter.java   ── owns the RAG system prompt
│       ├── parser/
│       │   └── TextDocumentParser.java         ── chunking + regex extraction
│       └── reference/
│           └── HsCodeReferenceData.java        ── loads hs-codes.csv at boot
│
└── config/                          ── @Configuration only — wiring + cross-cutting
    ├── AiConfig.java                ── (mostly empty; starters do the work)
    ├── McpConfig.java               ── registers tool callbacks
    ├── ObservabilityConfig.java     ── Micrometer common tags
    ├── SecurityConfig.java          ── intentional stub; OAuth2 plan in javadoc
    └── OpenApiConfig.java           ── Swagger metadata
```

### Naming conventions in this codebase

- `*UseCase` → an inbound port (interface)
- `*Service` → a use case implementation
- `*Port` → an outbound port (interface)
- `*Adapter` → a concrete implementation of an outbound port
- `*Controller` → an inbound HTTP adapter
- `*Tool` → an inbound MCP adapter
- `*Entity` → a JPA persistence model (never escapes the JPA package)
- `*Repository` (in `application/port/out/`) → the **port**, our contract
- `*JpaRepository` (in `adapter/out/persistence/jpa/`) → Spring Data, framework
- `*RepositoryAdapter` → bridges the two

Stick to these. They map cleanly to grep-friendly searches and make code
review pattern-matching trivial.

---

## 4. The two headline flows

### 4a. Ingest

```
POST /api/v1/documents
  ↓
DocumentController                  (adapter/in/web)
  ↓  (calls inbound port)
IngestDocumentUseCase
  ↓  (impl)
IngestDocumentService               (application/service)
  ├──► DocumentParserPort           ─► TextDocumentParser  (adapter/out/parser)
  │     → ParseResult (chunks + ShipmentMetadata)
  │
  ├──► DocumentRepository           ─► DocumentRepositoryAdapter → JPA
  │     → persist Document aggregate
  │
  └──► VectorStorePort              ─► PgVectorAdapter → Spring AI VectorStore
         → embed chunks + write to vector_store
  ↓
returns Document
  ↓
DocumentController maps to DocumentResponse
  ↓
201 Created
```

The same use case is callable from the MCP side too (`IngestDocumentTool`),
without any duplication.

### 4b. Query (RAG)

```
POST /api/v1/query                              (or MCP search_cargo_documents)
  ↓
QueryController                                 (or SearchDocumentsTool)
  ↓
AnswerQueryUseCase
  ↓
AnswerQueryService                              (application/service)
  ├──► VectorStorePort                          ─► PgVectorAdapter
  │     similaritySearch(queryText, topK, filters)
  │     → List<Citation>
  │
  └──► ChatModelPort                            ─► ChatModelRouter
         generateGrounded(query, citations)
         → grounded answer text
  ↓
returns Answer(text, citations)
  ↓
QueryResponse(answer, grounded, citations[])
  ↓
200 OK
```

`ChatModelPort` owns the system prompt that enforces grounding. That prompt
is *business logic*, not infrastructure — paraphrasing how you ground the
LLM changes what kind of product this is. Keeping it inside an adapter that
implements an application-owned port means:

1. We can unit-test `AnswerQueryService` with a fake `ChatModelPort` that
   just returns a canned string (see `AnswerQueryServiceTest`).
2. We can swap models (OpenAI → Anthropic → Ollama) by changing a starter,
   without touching the prompt strategy.
3. If we later split the prompt into a separately-versioned asset (e.g. a
   prompts table), the change is localised to the adapter.

---

## 5. RAG, concretely

A few decisions worth understanding:

### Chunk size: 800 chars, 120 overlap

These live as constants in `TextDocumentParser`. Trade-finance docs have
short labelled lines (`Vessel: ...`, `Incoterm: ...`) — too-large chunks
dilute the embedding; too-small chunks fragment numeric facts. 800/120 was
chosen as a starting point that fits well into 256-token embedding budgets
with overlap covering label/value splits. Tune against an eval set.

### Embeddings: text-embedding-3-small (1536 dim)

OpenAI's default, cheap (`$0.02 / 1M tokens`), fast, and the dimensionality
matches Spring AI's pgvector default schema. To swap:

```yaml
spring.ai.openai.embedding.options.model: text-embedding-3-large  # 3072 dim
spring.ai.vectorstore.pgvector.dimensions: 3072
```

You must rebuild the vector index when changing dimensions — they have to
match what the model emits or pgvector throws.

### Index: HNSW, cosine distance

`spring.ai.vectorstore.pgvector.index-type: HNSW` selects pgvector's
Hierarchical Navigable Small Worlds index. Faster than IVFFlat at small-to-
medium scale, with no training step. Cosine distance matches the geometry
OpenAI embeddings are tuned for.

### Citation contract: every claim, traceable

`AnswerQueryService` always returns the retrieved citations alongside the
answer text, and the `Answer.isGrounded()` boolean reflects whether *any*
citations came back. The adapter system prompt instructs the model to cite
`[#N]` and to refuse if context is insufficient. Combined with surfacing
ungrounded answers explicitly to the caller, this is the minimum bar for a
trustworthy RAG product.

### What's deliberately not in v1 (and where to slot it in)

- **Re-ranking.** A cross-encoder re-ranker between retrieval and generation
  is the single highest-ROI extension. Add a `RerankerPort` and call it
  inside `AnswerQueryService.answer()` after `similaritySearch`. Implement
  it with Cohere's hosted rerank API or a local bge-reranker via Ollama.
- **Multi-query rewriting.** Have the chat model produce 3 query variants,
  retrieve for each, dedupe by chunk ID, then synthesize. Goes inside
  `AnswerQueryService` — the adapter contracts don't need to change.
- **Hybrid search.** Add a Postgres FTS index on `vector_store.content` and
  combine BM25 + vector hits with reciprocal rank fusion. Means extending
  `VectorStorePort` with an FTS method, not changing it.
- **Conversational memory.** Introduce a `ChatMemoryPort`, persist turns,
  prepend recent history to the prompt. Spring AI 1.1 ships chat-memory
  primitives that fit cleanly behind it.

---

## 6. The MCP server, embedded

The MCP server is **another inbound adapter** — exactly the same structural
role as the REST controllers, just speaking a different protocol on a
different endpoint. There is no separate "MCP project"; the same Spring
Boot app exposes both.

### What's an MCP tool, internally?

A bean with `@Tool`-annotated methods. The Spring AI starter discovers them
via `ToolCallbackProvider` beans (wired in `McpConfig`), exposes their
signatures over JSON-RPC, and dispatches calls to them.

A tool method body is one line — delegate to a use case. That's it. Look at
`SearchDocumentsTool`: it accepts MCP-friendly parameters, builds a `Query`,
and calls `AnswerQueryUseCase`. The MCP layer's job is *adaptation*, not
business logic — same as a controller.

### Why expose tools an LLM is going to invoke, not just the REST API?

Three real reasons:

1. **The descriptions matter.** The `@Tool(description = "...")` text is
   what the LLM sees when deciding whether to call the tool. Writing those
   descriptions well is its own discipline — clear, action-verb-led,
   disambiguated from sibling tools. A good MCP server is one with a clear
   tool inventory.
2. **Determinism vs hallucination.** `lookup_incoterm` is deterministic
   reference data. `search_cargo_documents` is RAG. By exposing them as
   *separate* tools rather than collapsing both under "ask anything", the
   LLM client is biased toward the deterministic tool when the question is
   one it answers — fewer hallucinations.
3. **Composition.** An agent loop (Claude with multi-step tool use) can
   chain `search_hs_codes("smartphone")` → `lookup_hs_code("851712")` →
   `search_cargo_documents("any shipments of 851712 last quarter?")`. The
   REST API can't do that natively; MCP gives it for free.

### Streamable HTTP vs SSE

`application.yml` sets `spring.ai.mcp.server.protocol: STREAMABLE`. This is
the current MCP transport spec (replacing the older two-endpoint SSE setup)
and exposes a single endpoint at `POST /mcp`. Some clients still expect
STDIO — bridge with `mcp-remote` (see the README).

### What's stubbed, why

`prompts/TradeFinancePrompts.java` and `resources/DocumentResources.java`
are deliberate stubs. Tools are the headline MCP surface and ship first;
prompts and resources layer on top of a stable tool inventory. Each stub
file's javadoc describes the concrete features planned and the Spring AI 1.1
APIs that back them.

---

## 7. Postgres holds two tables

| Table | Owned by | Created by | Purpose |
|---|---|---|---|
| `documents` | the application | Flyway (`V1__init.sql`) | document metadata + flattened extracted fields for filtering |
| `vector_store` | Spring AI's PgVectorStore | the starter, on first run | chunk text + embedding + metadata JSON |

Two distinct tables, one Postgres. The starter's table is auto-managed (via
`spring.ai.vectorstore.pgvector.initialize-schema: true`); ours is owned by
Flyway with version-controlled migrations.

**Why not store chunk text in JPA too?** It doubles storage for nothing —
the vector store already keeps the chunk text alongside the embedding (it
has to, for retrieval). Storing it twice means every chunk read has to ask
which copy is authoritative. The chosen split:

- JPA owns the **document aggregate-level** metadata (title, type,
  vessel, ports, INCOTERM, invoice value).
- Vector store owns the **chunk-level** payload (text + embedding +
  per-chunk metadata for filter pushdown).

The Java `Document` aggregate carries chunks in memory at ingest time, then
empties them on read (`DocumentEntity#toDomain` returns `chunks = []`). If
you ever need full chunk text outside RAG, query the vector store directly.

### Cascade-deleting from both stores

`PgVectorAdapter#deleteByDocumentId` uses a Spring AI filter-expression
delete on `documentId`. The `DocumentRepositoryAdapter#deleteById` only
deletes the JPA row. **For a real "delete a document" operation, call both**
in a single use case (a `DeleteDocumentService` you'll add). The reason
that isn't in v1: nothing in the API yet exposes deletion. When you add it,
the use case body is six lines.

---

## 8. Testing strategy

Three rings of tests, mirroring the architecture rings:

### Ring 1 — Pure domain tests

```java
class IncotermTest { ... }
```

No Spring, no fakes, no setup. Run in milliseconds. The domain has so few
moving parts that the surface area for tests here is small — but valuable
when you're hand-coding enums + parsers.

### Ring 2 — Use-case tests with fake ports

```java
class AnswerQueryServiceTest {
  // hand-rolled fake VectorStorePort + ChatModelPort
}
```

No Spring, no Mockito, no Testcontainers. The fakes are anonymous classes
in the test file. This is the **mode you'll do most of your development in**
— TDD-style, sub-millisecond turnaround, exercising real business logic
against in-memory test doubles.

You can do this *because* the use case never imports a Spring AI type or a
JPA type. Every collaborator is a port the test can satisfy in five lines.

### Ring 3 — Integration smoke

```java
@SpringBootTest
@Testcontainers
class CargoIqApplicationTests {
  @Container static PostgreSQLContainer<?> POSTGRES = pgvector/pgvector:pg16;
  // ...
  @Test void contextLoads() { }
}
```

One test, one container. Catches misconfiguration the other two rings can't
— Flyway migrations parsing, bean wiring, properties resolving, etc.

### What about controller / adapter tests?

Inbound adapters that are pure one-line delegations (`DocumentController`,
the MCP tools) don't carry their own tests — there is no logic there to
verify that the use-case tests don't already cover. A `@WebMvcTest` earns its
place the moment an adapter grows real logic (parsing a multi-part upload,
HMAC-verifying a webhook). Testing trivial delegations is ceremony.

---

## 9. Configuration & profiles

```
src/main/resources/
├── application.yml          ── canonical config, dev defaults
├── application-dev.yml      ── verbose logging, all actuator endpoints
└── application-prod.yml     ── quiet logs, restricted actuator, auth expected
```

`SPRING_PROFILES_ACTIVE` selects. Docker-compose sets `dev`; CI doesn't need
a profile (Testcontainers handles the datasource).

### Properties hierarchy (most overridden first)

1. `OPENAI_API_KEY`, `POSTGRES_URL`, etc. — environment variables
2. `application-{profile}.yml`
3. `application.yml`

This is plain Spring Boot — nothing exotic. The reason it's worth calling
out: the `.env.example` file documents every env var that exists, so running
the app is never a grep through `${...}` placeholders to figure out what it
expects.

---

## 10. Observability

Spring Boot Actuator + Micrometer + Prometheus, all auto-wired by the
starters in `pom.xml`. `ObservabilityConfig` tags every metric with
`application` and `environment` so multi-deployment dashboards work without
per-environment knob-twiddling.

Spring AI 1.1 ships its own Micrometer integrations — LLM call duration,
token usage, vector store latency. They piggyback on the same registry
automatically. No extra config needed; check `/actuator/prometheus` and
look for `spring_ai_*` metrics once you're issuing real LLM calls.

### What to look at first when things slow down

| Metric | Tells you about |
|---|---|
| `spring_ai_chat_client_duration_seconds` | LLM call latency (the usual culprit) |
| `spring_ai_vector_store_duration_seconds` | pgvector retrieval latency |
| `hikaricp_connections_active` | JPA connection pool saturation |
| `jvm_threads_live` | virtual-thread leak indicators |

---

## 11. Security stance

Authentication is **stateless JWT**; authorization is **role-based** (see
`SecurityConfig`). The app is both authorization server and resource server:
`POST /api/v1/auth/login` mints an HS256-signed JWT, and every protected
request is validated as a bearer token by Spring Security's OAuth2 resource
server.

- **Roles.** `USER` reads the corpus and runs RAG queries; `ADMIN` additionally
  ingests and deletes documents (the corpus-mutating operations). Authorities
  ride in the `roles` JWT claim in `ROLE_*` form, so `hasRole('ADMIN')` resolves
  directly.
- **Open by design.** Auth endpoints, the OpenAPI docs, the actuator liveness
  probes, and the embedded MCP endpoint (`/mcp`) are permitted without a token
  so MCP Inspector and Swagger UI work out of the box. Putting `/mcp` behind the
  same bearer scheme is a one-line change in the filter chain.
- **Why the design stays at the edge.** The credential check lives in
  `AuthenticateUserService` against a `UserRepository` port, password hashing
  behind `PasswordHasherPort` (BCrypt), and token minting behind
  `TokenIssuerPort` (Nimbus JWT). The security framework never reaches into the
  domain — swapping HS256 for an RS256 key pair + JWKS, or BCrypt for Argon2, is
  an adapter change.

**Hardening before any public deployment:** override `JWT_SECRET` (≥ 32 bytes),
disable the `dev` profile's bootstrap admin, and front `/mcp` with auth.

---

## 12. Versioning & API contract

- REST endpoints live under `/api/v1` — the path is intentional. Breaking
  changes go to `/v2`.
- MCP tools are identified by `name`; renaming a tool **is** a breaking
  change for any LLM client that's learned to use it. If you must rename,
  keep the old name as an alias for a release.
- The Java package layout (`io.cargoiq.domain.model.*`) is **part of the
  artifact's API** if you ever publish it as a library. Renaming
  `DocumentChunk` to `Chunk` after a v1 cut is a major version.

---

## 13. Decisions logged separately

See `docs/adr/` for individual Architecture Decision Records:

- ADR-0001 — Hexagonal architecture
- ADR-0002 — pgvector as the vector store
- ADR-0003 — MCP server as an inbound adapter (same JVM)
- ADR-0004 — Domain choice: cargo & trade finance

ADRs are short. The format is: *Context → Decision → Consequences*. When
you change one of these decisions, append a new ADR rather than editing the
old one — the history is the point.

---

## 14. Next steps

Already in place: JWT auth + RBAC (§11), pluggable model providers
(`docs/model-providers.md`), and document delete with a two-store cascade.
Remaining, each a self-contained piece of work:

1. **Re-ranking.** `RerankerPort` + a Cohere adapter or local model, called in
   `AnswerQueryService` between retrieval and generation.
2. **PDF ingest.** `PdfDocumentParser` using the Tika dependency already in
   `pom.xml`.
3. **HS taxonomy expansion.** Swap the CSV for the full WCO export (still
   in-memory) or move to a Postgres FTS-backed adapter.
4. **MCP Prompts.** The named templates listed in the `TradeFinancePrompts`
   javadoc.
5. **MCP Resources.** Expose each `Document` at `cargo://documents/{id}`.
6. **Deploy.** A container target (Fly.io / Railway) with a small seed corpus.

Every one of these has an explicit hook in the code (a port, a stubbed
class, a TODO comment, or a javadoc upgrade plan). The scaffold is meant to
make these incremental, not architectural.
