# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`ARCHITECTURE.md` is the source of truth for *why* the code is shaped the way it is — read it before any non-trivial change. This file is the fast operational summary.

## What this is

`cargo-iq` is a Spring Boot 3.4 / Java 21 RAG service for cargo, freight, and trade-finance documents (Bills of Lading, Commercial Invoices, Letters of Credit, INCOTERMS, HS codes). The same capabilities are exposed twice: as a REST API under `/api/v1` and as an embedded MCP server at `POST /mcp` (Streamable HTTP). Retrieval is pgvector-backed. **It runs with no API key by default** — a built-in mock embedding (`MockEmbeddingModel`) + mock chat (`ChatModelRouter`) — and switches to OpenAI / Anthropic / Gemini / Ollama per-request (the query carries a `ModelChoice`) or via config.

## Commands

```bash
# Run the full stack (Postgres+pgvector + app) — no API key needed (mock default)
cp .env.example .env        # only edit to use a real provider
docker compose up --build   # app on :8080

# Build + run all tests (Testcontainers spins up pgvector for the smoke test)
mvn -B verify

# Run a single test class / method (no Docker needed for rings 1 & 2 below)
mvn -Dtest=IncotermTest test
mvn -Dtest=AnswerQueryServiceTest#answersWhenContextFound test

# Seed the sample corpus into a running app
./sample-corpus/seed-corpus.sh
```

There is no Maven wrapper; use a local `mvn`. CI (`.github/workflows/ci.yml`) runs `mvn -B -ntp verify` on JDK 21 with `TESTCONTAINERS_RYUK_DISABLED=true`, then a Docker image smoke-build.

## Architecture: strict hexagonal (Ports & Adapters)

Dependencies point inward: `adapter` → `application` → `domain`. The domain and application layers never import Spring AI, JPA, Jackson, or servlet types.

- `domain/` — pure POJOs, zero framework deps.
- `application/port/in/*` — inbound ports = use-case contracts (the app's public API).
- `application/port/out/*` — outbound ports = SPIs written in domain vocabulary.
- `application/service/*` — `@Service` use-case implementations.
- `adapter/in/web` and `adapter/in/mcp` — driving adapters (REST, MCP), both thin.
- `adapter/out/*` — driven adapters implementing outbound ports (jpa, vector, ai, parser, reference).
- `config/` — `@Configuration` only.

### Naming conventions (enforced, grep-friendly)
`*UseCase` = inbound port · `*Service` = its impl · `*Port` = outbound port · `*Adapter` = outbound impl · `*Controller` = REST adapter · `*Tool` = MCP adapter · `*Entity` = JPA model (never leaves the jpa package) · `*Repository` in `port/out/` = our port · `*JpaRepository` = Spring Data internal.

### Rules that span multiple files
- **Inbound adapters contain no business logic.** Controllers and `@Tool` methods build a domain object and delegate to a use case in one line. REST and MCP both call the *same* use cases — never duplicate logic between them.
- **The RAG system prompt is business logic, not infrastructure.** It lives inside `ChatModelRouter` (which implements `ChatModelPort`). Grounding/citation behavior is defined there, not in the service.
- **Two Postgres tables, deliberately split.** `documents` (JPA + Flyway-owned, aggregate metadata + flattened fields for filtering) and `vector_store` (Spring AI starter-owned, chunk text + embeddings). Chunk text is *not* duplicated into JPA — `DocumentEntity#toDomain` returns empty chunks on read. Deleting a document must hit *both* stores (`PgVectorAdapter#deleteByDocumentId` + the JPA delete); no delete use case exists yet.
- **Swapping infrastructure = rewrite one adapter + one starter.** Changing model vendor (OpenAI→Ollama/Anthropic) or vector store (pgvector→Qdrant) touches only an adapter class and `pom.xml`; domain/application stay untouched. This is the whole point — keep that wall intact.

### Two headline flows
- **Ingest** (`POST /api/v1/documents` or `ingest_cargo_document` tool) → `IngestDocumentService` → parse (chunks + metadata) → persist aggregate (JPA) → embed + index (vector store).
- **Query/RAG** (`POST /api/v1/query` or `search_cargo_documents` tool) → `AnswerQueryService` → `VectorStorePort.similaritySearch` → `ChatModelPort.generateGrounded` → `Answer(text, citations)`. `Answer.isGrounded()` reflects whether any citations came back.

### Auth & RBAC
Stateless JWT (HS256). `POST /api/v1/auth/login` issues a token; protected requests carry `Authorization: Bearer <jwt>`, validated by Spring Security's OAuth2 resource server. Roles `USER`/`ADMIN` (`domain.model.Role`) ride in the `roles` claim as `ROLE_*`. Corpus-mutating endpoints (`POST`/`DELETE /api/v1/documents`) require `ADMIN`; other `/api/v1/**` requires any authenticated user; auth/docs/health/`/mcp` are public. Credential check + token minting sit behind `UserRepository`/`PasswordHasherPort`/`TokenIssuerPort` — the security framework never reaches the core. Dev profile seeds a bootstrap admin (`DataInitializer`).

### Model providers
**Default is a dependency-free mock** (chat=`mock`, embedding=`none`→`MockEmbeddingModel`) so the app runs with no key. `ChatModelRouter` picks the chat model per request from the `ModelChoice` on the `Query`: `mock`, `ollama` (any pulled model, built on demand — needs only a local Ollama), or a server-configured provider. Four Spring AI starters are on the classpath; a real server default is set via `spring.ai.model.chat`/`embedding` (+ key). `MockEmbeddingModel` is `@ConditionalOnMissingBean`, so a configured embedding provider transparently replaces it. Embedding switches need re-indexing (`docs/model-providers.md`). Unavailable providers → 503 `ModelUnavailableException`.

## Testing strategy (three rings)
- **Ring 1 — domain** (`IncotermTest`): pure JUnit, no Spring, milliseconds.
- **Ring 2 — use case** (`AnswerQueryServiceTest`): JUnit + hand-rolled fake ports (anonymous classes in the test), no Spring/Mockito/Testcontainers. **This is the primary dev mode** — possible only because services import no framework types.
- **Ring 3 — integration smoke** (`CargoIqApplicationTests`): one `@SpringBootTest` + `@Testcontainers` against `pgvector/pgvector:pg16`. Catches wiring/Flyway/property errors.

Don't write `@WebMvcTest` for adapters that only delegate — add controller/tool tests only when an adapter grows real logic.

## Config & RAG tunables
- Profiles: `application.yml` (defaults) + `application-dev.yml` / `application-prod.yml`, selected by `SPRING_PROFILES_ACTIVE` (compose sets `dev`). `.env.example` documents every env var.
- Embedding dims **must** match the active embedding model (`spring.ai.vectorstore.pgvector.dimensions` / `VECTOR_DIMENSIONS`): OpenAI 3-small = 1536, Gemini/Ollama = 768. Switching embedding providers means re-indexing (`docs/model-providers.md`).
- Chunking constants (800 chars / 120 overlap) live in `TextDocumentParser`.
- MCP transport is `STREAMABLE` (single `POST /mcp`). New `@Tool` beans must be registered explicitly in `McpConfig` (no package scan — keeps the inventory diff-reviewable).
- `SecurityConfig` wires the JWT filter chain + encode/decode beans; the dev `JWT_SECRET` default must be overridden anywhere reachable.

## Roadmap hooks
Stubs and extension points are deliberate: `adapter/in/mcp/prompts/` and `adapter/in/mcp/resources/` are stubbed; re-ranking/multi-query/hybrid-search slot into `AnswerQueryService` behind new/extended ports. See ARCHITECTURE.md §5 and §14, and `docs/adr/` for the decision records.
