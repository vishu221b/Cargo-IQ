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
# edit .env, paste your OPENAI_API_KEY

docker compose up --build
```

The app listens on `http://localhost:8080`.

### 2. Seed the sample corpus

```bash
./sample-corpus/seed-corpus.sh
```

You should see 5 documents go in with their IDs printed.

### 3. Ask the corpus a question

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -d '{"query":"Which shipments were discharged at Brisbane and what INCOTERMs applied?"}' \
  | jq
```

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

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/documents` | Ingest a document (chunks, embeds, indexes) |
| GET | `/api/v1/documents` | List documents, filterable by type |
| GET | `/api/v1/documents/{id}` | Fetch a single document |
| POST | `/api/v1/query` | RAG: ask a grounded question with citations |
| GET | `/api/v1/incoterms/{code}` | Canonical INCOTERM 2020 rule lookup |
| GET | `/api/v1/hs-codes/{code}` | HS code by exact code |
| GET | `/api/v1/hs-codes/search?q=...` | HS code by description |
| GET | `/swagger` | Swagger UI |
| GET | `/actuator/health` | Liveness/readiness |
| GET | `/actuator/prometheus` | Metrics |

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

## Roadmap (the weekend you're about to spend on this)

Read `ARCHITECTURE.md` first — every item below has a marker in the code or
docs explaining where to slot the change in.

- [ ] **Document Resources** — expose every persisted `Document` as an MCP
      resource at `cargo://documents/{id}`.
- [ ] **MCP Prompts** — add `compare_bl_to_invoice`, `letter_of_credit_compliance_check`,
      `port_handover_brief`.
- [ ] **Re-ranking** — wire a cross-encoder re-ranker (Cohere or local
      bge-reranker) between retrieval and generation in `AnswerQueryService`.
- [ ] **PDF ingest** — add a `PdfDocumentParser` using Spring AI's Tika reader
      (the dependency is already in `pom.xml`).
- [ ] **HS taxonomy** — replace the curated CSV with the full WCO HS 2022
      export; consider a Postgres FTS-backed adapter once size justifies it.
- [ ] **Auth** — fill in `SecurityConfig` with OAuth2 resource-server config
      before deploying anywhere reachable.
- [ ] **Hosted demo** — Fly.io or Railway, deploy with a small seed corpus.
      Add the live URL to the GitHub repo description.

---

## License

Apache 2.0. See [`LICENSE`](./LICENSE) — add one before publishing.
