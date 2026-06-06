# cargo-iq

> RAG + MCP for international cargo, freight, and trade-finance documents.
> Built with Spring Boot 3.4, Spring AI 1.1, pgvector, and Java 21.

`cargo-iq` ingests shipping documents — Bills of Lading, Commercial Invoices,
Letters of Credit, Charter Parties, and reference material like INCOTERMS 2020
— and lets you ask grounded, citation-backed questions about them. The same
capabilities are exposed twice:

- as a **REST API** (OpenAPI/Swagger UI on `/swagger`), and
- as an embedded **MCP server** (Model Context Protocol) on `POST /mcp`, so
  LLM clients like Claude Desktop, Cursor, or the MCP Inspector can invoke
  the same tools directly.

The codebase is laid out as a strict **hexagonal architecture** (Ports &
Adapters). See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for the deep walkthrough
— that document is the project's source of truth and the place to look first.

---

## Stack

| Layer | Choice |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 |
| AI | Spring AI 1.1 (`starter-model-openai`, `starter-vector-store-pgvector`, `starter-mcp-server-webmvc`) |
| Vector store | pgvector inside Postgres (HNSW + cosine) |
| Relational store | Postgres 16 |
| Migrations | Flyway |
| Observability | Actuator + Micrometer + Prometheus |
| API docs | springdoc-openapi (Swagger UI at `/swagger`) |
| Tests | JUnit 5 + AssertJ + Testcontainers |
| Container | Multi-stage Dockerfile (Temurin 21, non-root) |
| CI | GitHub Actions |

---

## Quick start

### 1. Bring up Postgres + the app

```bash
cp .env.example .env
# Pick a model provider:
#   - OpenAI/Anthropic/Gemini: paste the relevant API key (see docs/model-providers.md)
#   - Ollama (no key): `ollama pull llama3.1 && ollama pull nomic-embed-text`
#     then set SPRING_PROFILES_ACTIVE=dev,ollama

docker compose up --build
```

The app listens on `http://localhost:8080`. The `dev` profile seeds a bootstrap
admin (`admin` / `admin12345` by default — override via `.env`).

### 2. Log in (JWT) and seed the sample corpus

The API is secured with JWT + RBAC. Ingest requires the `ADMIN` role; the seed
script logs in for you:

```bash
./sample-corpus/seed-corpus.sh          # logs in as admin, then ingests 5 docs
```

To get a token by hand:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | jq -r .accessToken)
```

### 3. Ask the corpus a question

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"Which shipments were discharged at Brisbane and what INCOTERMs applied?"}' \
  | jq
```

Self-service registration (`POST /api/v1/auth/register`) creates a `USER` who
can query and read but cannot ingest or delete.

### 4. Connect an MCP client

The MCP server is live at `POST http://localhost:8080/mcp` (Streamable HTTP).
For a quick interactive UI:

```bash
npx @modelcontextprotocol/inspector
```

Point it at `http://localhost:8080/mcp` (transport: Streamable HTTP). You
should see five tools: `search_cargo_documents`, `lookup_incoterm`,
`lookup_hs_code`, `summarize_shipment`, `ingest_cargo_document`.

To connect from Claude Desktop, which currently expects STDIO MCP servers,
bridge with `mcp-remote`:

```json
{
  "mcpServers": {
    "cargo-iq": {
      "command": "npx",
      "args": ["mcp-remote", "http://localhost:8080/mcp"]
    }
  }
}
```

---

## Web UI

A modern single-page client lives in [`frontend/`](./frontend) — React 18 +
TypeScript + Vite + Tailwind, with framer-motion micro-interactions and an
Aceternity/21st.dev-style visual language. It exercises the backend end to end:
JWT auth with role-aware gating, a live corpus dashboard (`/api/v1/overview`),
document browse/ingest/delete, RAG queries with grounded/citation rendering, and
the INCOTERMS + HS-code reference lookups.

```bash
docker compose up --build          # API on :8080
cd frontend && npm install && npm run dev   # UI on :5173
```

Sign in with the dev bootstrap admin (`admin` / `admin12345`). See
[`frontend/README.md`](./frontend/README.md) for details.

---

## Running tests

```bash
mvn -B verify
```

Tests fall into three buckets:
- **Domain tests** — pure JUnit, no Spring (`IncotermTest`)
- **Use-case tests** — JUnit + hand-rolled fake ports, no Spring (`AnswerQueryServiceTest`)
- **Integration smoke** — full `@SpringBootTest` against pgvector via Testcontainers
  (`CargoIqApplicationTests`)

This layering is the payoff of hexagonal architecture — most of your tests
never need a container, only the smoke test does. See `ARCHITECTURE.md` §
*Testing strategy*.

---

## REST surface (highlights)

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Create a `USER` account |
| POST | `/api/v1/auth/login` | public | Exchange credentials for a bearer JWT |
| GET | `/api/v1/auth/me` | any auth | Identity + roles from the presented token |
| POST | `/api/v1/documents` | ADMIN | Ingest a document (chunks, embeds, indexes) |
| DELETE | `/api/v1/documents/{id}` | ADMIN | Delete a document (cascades JPA + vector store) |
| GET | `/api/v1/documents` | any auth | List documents, filterable by type |
| GET | `/api/v1/documents/{id}` | any auth | Fetch a single document |
| POST | `/api/v1/query` | any auth | RAG: ask a grounded question with citations |
| GET | `/api/v1/incoterms/{code}` | any auth | Canonical INCOTERM 2020 rule lookup |
| GET | `/api/v1/hs-codes/{code}` | any auth | HS code by exact code |
| GET | `/api/v1/hs-codes/search?q=...` | any auth | HS code by description |
| GET | `/swagger` | public | Swagger UI (with "Authorize" for the bearer token) |
| GET | `/actuator/health` | public | Liveness/readiness |
| GET | `/actuator/prometheus` | any auth | Metrics |

---

## MCP tools

| Tool | Use case it calls | Description |
|---|---|---|
| `search_cargo_documents` | `AnswerQueryUseCase` | RAG over ingested docs, returns grounded answer + citations |
| `lookup_incoterm` | `LookupIncotermUseCase` | Canonical INCOTERMS 2020 lookup (no LLM) |
| `lookup_hs_code` | `LookupHsCodeUseCase` | HS tariff code by exact code |
| `search_hs_codes` | `LookupHsCodeUseCase` | HS tariff code by free-text description |
| `summarize_shipment` | `AnswerQueryUseCase` + `ListDocumentsUseCase` | Structured summary of one known document |
| `ingest_cargo_document` | `IngestDocumentUseCase` | Push a new document into the corpus from the LLM side |

---

## Project layout

```
io.cargoiq
├── domain/                 # pure POJOs — no Spring, no JPA, no Jackson
│   ├── model/              # Document, DocumentChunk, Incoterm, HsCode, ...
│   └── exception/
├── application/            # use cases + ports
│   ├── port/in/            # inbound port interfaces (use case contracts)
│   ├── port/out/           # outbound port interfaces (SPIs)
│   └── service/            # use case implementations
├── adapter/
│   ├── in/
│   │   ├── web/            # REST controllers + DTOs + @RestControllerAdvice
│   │   └── mcp/            # MCP tools (and stubs for prompts + resources)
│   └── out/
│       ├── ai/             # Spring AI ChatModel adapter
│       ├── parser/         # raw-text parsing + metadata extraction
│       ├── persistence/
│       │   ├── jpa/        # DocumentEntity + Spring Data repo + adapter
│       │   └── vector/     # PgVectorAdapter wrapping Spring AI VectorStore
│       └── reference/      # HsCodeReferenceData (CSV-backed)
└── config/                 # @Configuration classes only
```

---

## Roadmap

Each item below has a marker in the code or docs explaining where it slots in;
`ARCHITECTURE.md` has the detail.

- [x] **Auth** — stateless JWT + RBAC (USER/ADMIN) via Spring Security.
- [x] **Pluggable model providers** — OpenAI, Anthropic, Gemini, Ollama
      (see `docs/model-providers.md`).
- [x] **Document delete** — cascade across the JPA aggregate and pgvector.
- [ ] **Document Resources** — expose every persisted `Document` as an MCP
      resource at `cargo://documents/{id}`.
- [ ] **MCP Prompts** — `compare_bl_to_invoice`, `letter_of_credit_compliance_check`,
      `port_handover_brief`.
- [ ] **Re-ranking** — a cross-encoder re-ranker (Cohere or local bge-reranker)
      between retrieval and generation in `AnswerQueryService`.
- [ ] **PDF ingest** — a `PdfDocumentParser` using Spring AI's Tika reader
      (the dependency is already in `pom.xml`).
- [ ] **HS taxonomy** — replace the curated CSV with the full WCO HS 2022
      export; a Postgres FTS-backed adapter once size justifies it.

---

## How this project is built

This is a personal project, built with AI assistance used deliberately as an
engineering tool — for scaffolding, drafting, and exploring options — while the
design decisions, domain modelling, and architecture are my own. **Every change
is manually reviewed, run, and tested before it is merged**; the layered unit
tests (`mvn verify`) are the gate, and no PR lands without a green build and a
read-through of the diff. The intent is a codebase I fully understand and stand
behind, not generated output taken on faith.

---

## License

Apache 2.0.
