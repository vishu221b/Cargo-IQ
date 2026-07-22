<div align="center">

# 🚢 cargo-iq

**Grounded RAG intelligence for international cargo, freight & trade-finance documents — exposed as both a REST API and an embedded MCP server.**

[![CI](https://github.com/vishu221b/Cargo-IQ/actions/workflows/ci.yml/badge.svg)](https://github.com/vishu221b/Cargo-IQ/actions/workflows/ci.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI 1.1](https://img.shields.io/badge/Spring_AI-1.1-6DB33F.svg)](https://spring.io/projects/spring-ai)
[![pgvector](https://img.shields.io/badge/pgvector-Postgres_16-336791.svg?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![React 18](https://img.shields.io/badge/React-18-61DAFB.svg?logo=react&logoColor=black)](https://react.dev)
[![MCP](https://img.shields.io/badge/MCP-Streamable_HTTP-7C5CFF.svg)](https://modelcontextprotocol.io)
[![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#-contributing)

[Quick start](#-quick-start) · [Architecture](./ARCHITECTURE.md) · [Web UI](#-web-ui) · [REST API](#-rest-api) · [MCP](#-mcp-tools) · [Model providers](./docs/model-providers.md)

</div>

---

`cargo-iq` ingests shipping documents — Bills of Lading, Commercial Invoices,
Letters of Credit, Charter Parties, and reference material like INCOTERMS 2020 —
and answers grounded, citation-backed questions about them. The same capabilities
are served two ways: a **REST API** (OpenAPI/Swagger UI) and an embedded **MCP
server** (Model Context Protocol) so LLM clients like Claude Desktop, Cursor, or
the MCP Inspector can invoke the tools directly.

It is built as a strict **hexagonal architecture** (Ports & Adapters) and **runs
with no API key out of the box** — a built-in mock model makes the whole pipeline
demonstrable offline, while OpenAI / Anthropic / Gemini / Ollama can be selected
per request.

> 📐 [`ARCHITECTURE.md`](./ARCHITECTURE.md) is the source of truth for *why* the
> code is shaped the way it is — start there.

## ✨ Highlights

- **RAG done properly** — retrieve → ground → answer with `[#N]` citations and an
  explicit *grounded / ungrounded* signal on every response.
- **Two front doors, one core** — REST controllers and MCP `@Tool`s are thin
  inbound adapters over the *same* use cases. Zero duplicated business logic.
- **Runs with zero setup** — a mock embedding (lexical feature-hashing) + mock
  chat answer offline; swap to a real provider per request.
- **Pluggable models** — OpenAI, Anthropic (Claude), Google Gemini, and Ollama
  (any locally-pulled model, e.g. `gemma2:9b`), chosen from the UI or the API.
- **Secure by default** — stateless JWT auth with role-based access control
  (USER / ADMIN) via Spring Security.
- **Modern web client** — React + Vite + Tailwind SPA with a light/dark theme and
  a per-request model picker.
- **Production-shaped** — Flyway migrations, Actuator/Micrometer/Prometheus,
  OpenAPI, a multi-stage non-root Dockerfile, and CI on GitHub Actions.

## 🧱 Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.4 · Spring Security · Spring Data JPA |
| AI | Spring AI 1.1 (OpenAI · Anthropic · Google GenAI · Ollama · pgvector · MCP server) |
| Vector store | pgvector in Postgres 16 (HNSW + cosine) |
| Migrations | Flyway |
| Observability | Actuator + Micrometer + Prometheus |
| API docs | springdoc-openapi (Swagger UI at `/swagger`) |
| Web client | React 18 · TypeScript · Vite · Tailwind · framer-motion |
| Tests | JUnit 5 · AssertJ · Testcontainers |
| Packaging / CI | Multi-stage Dockerfile (Temurin 21, non-root) · GitHub Actions |

## 🚀 Quick start

### 1 · Bring up the whole stack

One command brings up everything — web UI, API, pgvector, and a browser DB client:

```bash
docker compose up --build
```

| Service | URL | Notes |
| --- | --- | --- |
| **Web UI** | http://localhost:3000 | React SPA (nginx); proxies the API same-origin |
| **API + Swagger** | http://localhost:8080 · `/swagger` | REST + MCP (`POST /mcp`) |
| **Adminer** | http://localhost:8081 | DB viewer — server `postgres`, user/pass `cargoiq` |

No `.env` and **no API key** are required — the compose file pins the built-in
mock defaults, so the stack is self-contained and offline. (To use a real
provider, set `AI_CHAT_PROVIDER`/`AI_EMBEDDING_PROVIDER` + key and
`VECTOR_DIMENSIONS` on the `app` service, then `docker compose down -v && up`.)

**No API key is needed to run.** Out of the box the app uses a built-in mock
embedding + mock chat model, so the full pipeline (ingest → retrieve → grounded
answer with citations) works offline. To use a real model, pick a provider
per request in the UI — `ollama` with any pulled model (e.g. `gemma2:9b`), or a
server-configured `openai` / `anthropic` / `google-genai`. See
[`docs/model-providers.md`](./docs/model-providers.md).

The `dev` profile seeds a bootstrap admin (`admin` / `admin12345` by default —
override via `.env`).

### 2 · Log in (JWT) and seed the sample corpus

The API is secured with JWT + RBAC; ingest requires the `ADMIN` role. The seed
script logs in for you:

```bash
./sample-corpus/seed-corpus.sh          # logs in as admin, then ingests 5 docs
```

Grab a token by hand if you prefer:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | jq -r .accessToken)
```

### 3 · Ask the corpus a question

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"Which shipments were discharged at Brisbane and what INCOTERMs applied?"}' \
  | jq
```

Self-service registration (`POST /api/v1/auth/register`) creates a `USER` who can
query and read but cannot ingest or delete.

### 4 · Connect an MCP client

The MCP server is live at `POST http://localhost:8080/mcp` (Streamable HTTP). For
a quick interactive UI:

```bash
npx @modelcontextprotocol/inspector   # point it at http://localhost:8080/mcp
```

From Claude Desktop (which expects STDIO MCP servers), bridge with `mcp-remote`:

```json
{
  "mcpServers": {
    "cargo-iq": { "command": "npx", "args": ["mcp-remote", "http://localhost:8080/mcp"] }
  }
}
```

## 🖥 Web UI

A modern single-page client lives in [`frontend/`](./frontend) — React 18 +
TypeScript + Vite + Tailwind, with framer-motion micro-interactions and an
Aceternity / 21st.dev-style visual language. It drives the backend end to end:

- JWT auth with role-aware gating (ingest/delete surface only for `ADMIN`)
- a live corpus **dashboard** (`/api/v1/overview`) with animated breakdowns
- **documents** — browse / filter / delete, paste-text **or file upload**
  (PDF · DOCX · HTML · TXT), with "load more" pagination
- **ask the corpus** — a multi-turn **RAG chat**: conversational memory,
  toggleable **retrieval strategy** (hybrid · multi-query · rerank) shown per
  answer, grounded badge, scored citation cards, and a **per-request model
  picker** (mock / Ollama / configured provider)
- **reference** — INCOTERMS 2020 rule cards and HS-code lookup
- a persisted **light / dark theme** toggle, in a warm **Freight Amber** palette

The UI ships as part of `docker compose up` (served on **:3000**). For a hot-reload
dev loop instead:

```bash
cd frontend && npm install && npm run dev     # UI on :5173, proxies /api to :8080
```

See [`frontend/README.md`](./frontend/README.md) for details.

## 🔌 REST API

| Method | Path | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | public | Create a `USER` account |
| `POST` | `/api/v1/auth/login` | public | Exchange credentials for a bearer JWT |
| `GET` | `/api/v1/auth/me` | any auth | Identity + roles from the presented token |
| `POST` | `/api/v1/documents` | `ADMIN` | Ingest a document (chunks, embeds, indexes) |
| `DELETE` | `/api/v1/documents/{id}` | `ADMIN` | Delete a document (cascades JPA + vector store) |
| `GET` | `/api/v1/documents` | any auth | List documents, filterable by type |
| `GET` | `/api/v1/documents/{id}` | any auth | Fetch a single document |
| `POST` | `/api/v1/query` | any auth | RAG: grounded answer + citations (per-request model choice) |
| `GET` | `/api/v1/overview` | any auth | Corpus totals + breakdowns (dashboard) |
| `GET` | `/api/v1/incoterms/{code}` | any auth | Canonical INCOTERM 2020 rule lookup |
| `GET` | `/api/v1/hs-codes/{code}` | any auth | HS code by exact code |
| `GET` | `/api/v1/hs-codes/search?q=…` | any auth | HS code by description |
| `GET` | `/swagger` | public | Swagger UI (with bearer "Authorize") |
| `GET` | `/actuator/health` | public | Liveness / readiness |

## 🧰 MCP tools

| Tool | Use case | Description |
|---|---|---|
| `search_cargo_documents` | `AnswerQueryUseCase` | RAG over the corpus → grounded answer + citations |
| `lookup_incoterm` | `LookupIncotermUseCase` | Canonical INCOTERMS 2020 lookup (no LLM) |
| `lookup_hs_code` | `LookupHsCodeUseCase` | HS tariff code by exact code |
| `search_hs_codes` | `LookupHsCodeUseCase` | HS tariff code by free-text description |
| `summarize_shipment` | `AnswerQueryUseCase` + `ListDocumentsUseCase` | Structured summary of one document |
| `ingest_cargo_document` | `IngestDocumentUseCase` | Push a new document into the corpus |

The server also exposes MCP **prompts** — `compare_bl_to_invoice`,
`letter_of_credit_compliance_check`, `port_handover_brief` — and **resources**:
`cargo://documents` (index) and `cargo://documents/{id}` (a document's full text).

## 🧠 Model providers

The chat model is chosen **per request** and embeddings are a server-level
choice. The default is a dependency-free mock; real providers slot in behind the
same ports. Full matrix and trade-offs in
[`docs/model-providers.md`](./docs/model-providers.md).

| Provider | Chat | Embeddings | Credential |
|---|:---:|:---:|---|
| Mock (default) | ✓ | ✓ | none |
| OpenAI | ✓ | ✓ | API key |
| Anthropic (Claude) | ✓ | — | API key |
| Google Gemini | ✓ | ✓ | API key |
| Ollama | ✓ | ✓ | none (local) |

## 🧪 Testing

```bash
mvn -B verify        # unit rings + a Testcontainers pgvector smoke test
```

Three rings, mirroring the architecture:

- **Domain** — pure JUnit, no Spring (`IncotermTest`).
- **Use case** — JUnit + hand-rolled fake ports, no Spring/Mockito/Testcontainers
  (`AnswerQueryServiceTest`, `ChatModelRouterTest`, …). The primary dev loop.
- **Integration smoke** — one `@SpringBootTest` against `pgvector/pgvector:pg16`
  via Testcontainers (`CargoIqApplicationTests`).

Most tests never need a container — only the smoke test does. See
`ARCHITECTURE.md` § *Testing strategy*.

## 🗂 Project layout

```text
io.cargoiq
├── domain/                 # pure POJOs — no Spring, no JPA, no Jackson
│   ├── model/              # Document, Incoterm, HsCode, Query, ModelChoice, User, …
│   └── exception/
├── application/            # use cases + ports
│   ├── port/in/            # inbound ports (use-case contracts)
│   ├── port/out/           # outbound ports (SPIs)
│   └── service/            # use-case implementations
├── adapter/
│   ├── in/
│   │   ├── web/            # REST controllers + DTOs + @RestControllerAdvice
│   │   └── mcp/            # MCP @Tool beans (+ stubs for prompts/resources)
│   └── out/
│       ├── ai/             # ChatModelRouter + MockEmbeddingModel (Spring AI)
│       ├── security/       # BCrypt hasher + JWT issuer
│       ├── parser/         # text chunking + metadata extraction
│       ├── persistence/    # jpa/ (documents, users) + vector/ (pgvector)
│       └── reference/      # HsCodeReferenceData (CSV-backed)
└── config/                 # @Configuration classes only

frontend/                   # React + Vite + Tailwind SPA
docs/                       # model-providers guide + ADRs + diagrams
```

## 🗺 Roadmap

- [x] Stateless **JWT auth + RBAC** (USER / ADMIN) via Spring Security
- [x] **Pluggable model providers** + a zero-key mock default
- [x] **Per-request model selection** (provider + model) from the UI
- [x] **Document delete** cascading JPA + pgvector
- [x] **Corpus overview** endpoint + dashboard
- [x] **MCP Resources** — `cargo://documents` + `cargo://documents/{id}`
- [x] **MCP Prompts** — `compare_bl_to_invoice`, `letter_of_credit_compliance_check`, `port_handover_brief`
- [x] **Hybrid retrieval** — dense (pgvector) + sparse (Postgres FTS) fused with RRF
- [x] **Multi-query rewriting** + **MMR re-ranking** (both dependency-free / zero-key)
- [x] **Conversational memory** — multi-turn RAG chat threaded by `conversationId`
- [x] **File ingest** — PDF / DOCX / HTML / TXT via the Tika reader (`POST /documents/upload`)
- [x] **HS search** — token-aware ranked search over an expanded schedule
- [x] **Single `docker compose up`** — web + API + pgvector + Adminer, zero-key by default
- [ ] **Cross-encoder re-ranker** — swap the MMR reranker for a hosted rerank model
- [ ] **Deploy target** — Fly.io / Railway one-click

## 🤝 Contributing

Issues and PRs are welcome. Branch from `main`, keep changes small and tested,
and make sure `mvn -B verify` (and `cd frontend && npm run build`) is green before
opening a PR.

## 🛠 How this project is built

This is a personal project, built with AI assistance used deliberately as an
engineering tool — for scaffolding, drafting, and exploring options — while the
design decisions, domain modelling, and architecture are my own. **Every change
is manually reviewed, run, and tested before it is merged**: the layered tests
(`mvn verify`) are the gate, and nothing lands without a green build and a
read-through of the diff. The intent is a codebase I fully understand and stand
behind, not generated output taken on faith.

## 📄 License

Licensed under the **Apache License 2.0** — see [`LICENSE`](./LICENSE).
