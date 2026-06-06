# ADR-0002 — pgvector as the vector store

**Status:** Accepted
**Date:** 2026-05-27

## Context

`cargo-iq` needs a vector store for chunk embeddings. The Spring AI ecosystem
supports many — pgvector, Qdrant, Weaviate, Chroma, Pinecone, Milvus, Redis,
MongoDB Atlas, Elasticsearch, Azure Cognitive Search, and others — all behind
a common `VectorStore` interface.

The choice has two axes:
- **Operational simplicity** — clone and `docker compose up` in 60 seconds.
- **Production credibility** — the answer should be the same one a real shop
  would defend in an architecture review.

## Decision

Use **pgvector inside Postgres**.

`spring-ai-starter-vector-store-pgvector` autowires a `PgVectorStore` on top
of the same `DataSource` JPA already uses. The `pgvector/pgvector:pg16`
container image ships the extension preinstalled. Index type: HNSW. Distance
metric: cosine.

## Consequences

**Positive**

- One database to back up, monitor, restore.
- Filter pushdown works across both tables — we can join cargo-metadata
  filters (`type = 'BILL_OF_LADING'`) with vector search natively, in SQL.
- Postgres already has the operational maturity (replication, PITR,
  observability) that purpose-built vector DBs are still catching up on.
- The HNSW index is fast enough for the corpus sizes this project targets,
  and well into the "real production" range.

**Negative**

- Storage growth in Postgres is faster than for a relational-only system —
  embeddings are wide. Acceptable at this scale.
- HNSW recall is high but not perfect; for high-recall retrieval needs you
  add a re-ranker downstream (see ADR roadmap).
- If `cargo-iq` ever needs to scale to billions of vectors, a dedicated
  store (Qdrant, Pinecone) makes sense. At that point we swap one adapter.

## Alternatives considered

- **Qdrant** — excellent product, but adds a second piece of infrastructure
  and gives little on top of pgvector at this scale.
- **Pinecone** — managed and fast, but cloud-only and not free-tier-runnable
  in a way that matches our "clone and `docker compose up`" goal.
- **Chroma** — popular for prototypes, weaker on filter pushdown and
  production tooling.

## How to switch later

1. Swap the starter in `pom.xml`
   (`spring-ai-starter-vector-store-pgvector` → `…-qdrant`, etc.).
2. Update `application.yml` with the new vector store's properties.
3. Rename `PgVectorAdapter` → `QdrantAdapter`; the class body for
   `VectorStorePort` stays nearly identical because Spring AI's
   `VectorStore` interface is the same across vendors.

Nothing in `application/` or `domain/` changes.
