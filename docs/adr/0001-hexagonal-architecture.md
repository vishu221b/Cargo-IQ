# ADR-0001 — Hexagonal architecture (Ports & Adapters)

**Status:** Accepted
**Date:** 2026-05-27

## Context

`cargo-iq` is going to evolve faster on its infrastructure surface (model
providers, vector stores, re-ranking strategies, prompt schemes) than on its
business rules (what an INCOTERM is, how a Bill of Lading relates to a Letter
of Credit, what "grounded answer" means).

A vanilla "controller → service → repository" Spring Boot layout couples the
service tier to whatever's behind it (JPA, Spring AI's `VectorStore`,
provider-specific embedding shapes). Every infrastructure swap then forces
service rewrites.

We want the opposite property: infrastructure churn localised to a single
adapter, with the application core stable.

## Decision

Adopt Hexagonal Architecture (a.k.a. Ports & Adapters):

- `io.cargoiq.domain` — pure POJOs. No Spring, no JPA, no Jackson.
- `io.cargoiq.application` — use cases and the two sets of ports they own
  (inbound = driving, outbound = driven SPIs).
- `io.cargoiq.adapter.in.*` — inbound adapters (REST, MCP).
- `io.cargoiq.adapter.out.*` — outbound adapters (JPA, pgvector, Spring AI,
  parsers, reference data).

The application layer depends only on the domain. Outbound adapters depend
on the application's ports; the application never depends on them.

## Consequences

**Positive**

- Use cases can be unit-tested in pure Java with fake ports. No Spring, no
  Testcontainers. Sub-millisecond.
- Swapping pgvector for Qdrant means rewriting one class. Same for OpenAI →
  Anthropic → Ollama.
- The package boundary is a code-review tool: a PR that imports
  `org.springframework.ai.*` into `io.cargoiq.application.*` is wrong by
  inspection.
- The MCP server is "just another inbound adapter" — no duplicated logic,
  no integration scaffolding.

**Negative**

- More files than a naive layout — explicit DTOs, explicit ports, explicit
  mappings. Worth it for the test ergonomics and the swap stories above;
  not worth it for a CRUD-over-MySQL toy. We're not building a toy.
- New contributors need to internalise "the port belongs to the
  application, not the adapter". This is the single most common confusion.
  Read `ARCHITECTURE.md` §3 before contributing.

## Alternatives considered

1. **Three-layer Spring (controller/service/repository).** Cheap to start,
   expensive to evolve. Rejected for the reasons in *Context*.
2. **Onion architecture.** Equivalent in practice; we use the
   Cockburn/hexagonal vocabulary because the "drives the app" / "is driven
   by the app" distinction is the daily mental model.
3. **DDD with explicit bounded contexts.** Premature here — this app is
   one bounded context.
