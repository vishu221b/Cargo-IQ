# ADR-0004 — Domain choice: cargo & trade finance

**Status:** Accepted
**Date:** 2026-05-27

## Context

A RAG system needs a domain, and the domain shapes what the architecture has to
prove. Generic choices ("notes app", "PDF chat") exercise retrieval but little
else — there is no reason for deterministic tools, no reference data to ground
against, and no real structure to extract.

What the domain needs to provide:

- A reason for the LLM/MCP server to do *something beyond summarising a PDF* —
  genuine tool calls, and a genuine reason for grounding to matter.
- Both **unstructured** content (documents to retrieve over) and **structured,
  authoritative** reference data (where an LLM should *not* be guessing).
- Real, finite vocabulary that makes correctness checkable rather than vibes.

## Decision

Use **international cargo and trade finance** as the domain:

- Documents: Bills of Lading, Sea Waybills, Commercial Invoices, Packing Lists,
  Letters of Credit, Certificates of Origin, Charter Parties.
- Reference data: INCOTERMS 2020 (the 11-rule ICC ruleset) and the HS
  (Harmonised System) tariff taxonomy.
- Use cases: ingest + semantic search over the corpus; deterministic lookup of
  INCOTERMS and HS codes; structured summarisation of one known document.

## Consequences

**Positive**

- **The domain rewards both RAG and structured tools.** Document Q&A is RAG;
  INCOTERM rule lookup is deterministic and should not go through an LLM at all.
  Exposing both kinds of tool in one MCP server is the whole point — it forces a
  clean separation between "retrieve and reason" and "look up a fact".
- **Grounding is meaningful.** Trade-finance answers carry money and legal
  weight, so an ungrounded or uncited answer is a real defect, not a cosmetic
  one. That makes the citation contract (every claim traceable) load-bearing.
- **The vocabulary is real and finite.** B/L, LC, CIF, FOB, demurrage,
  consignee — and exactly eleven INCOTERMS, with specific scopes. Correctness is
  verifiable; the hard-coded enum in `Incoterm.java` is deliberate so the system
  cannot "hallucinate" a twelfth rule.

**Negative**

- Narrower general appeal than a customer-support chatbot, and the reference
  data must be kept accurate by hand. Both are acceptable: the domain's
  structure is exactly what makes the RAG-vs-deterministic split worth building.

## Alternatives considered

- **Personal notes / journaling RAG** — pure retrieval, no structured tools, no
  authoritative reference data to ground against.
- **Codebase Q&A** — interesting, but the problem space is saturated with mature
  products (Cursor, Aider, etc.) and adds little to demonstrate here.
- **Pure freight tariff comparison** — too narrow; loses the trade-finance
  documents that make the corpus worth retrieving over.
