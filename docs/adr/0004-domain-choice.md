# ADR-0004 — Domain choice: cargo & trade finance

**Status:** Accepted
**Date:** 2026-05-27

## Context

A RAG demo needs a domain. Generic choices ("notes app", "PDF chat") are
easy to build and forgettable to look at. The domain is the single biggest
lever on whether this project moves a recruiter's needle.

Constraints for the choice:

- Must connect to authentic prior work — credible interview talking points.
- Must be niche enough to stand out in a portfolio.
- Must give the LLM/MCP server *something interesting to do* beyond
  "summarise this PDF" — a real reason for tool calls, a real reason for
  grounding to matter.
- Must fit in a weekend.

## Decision

Pick **international cargo and trade finance** as the domain:

- Documents: Bills of Lading, Sea Waybills, Commercial Invoices, Packing
  Lists, Letters of Credit, Certificates of Origin, Charter Parties.
- Reference data: INCOTERMS 2020 (the 11-rule ICC ruleset) and the HS
  (Harmonised System) tariff taxonomy.
- Use cases: ingest + semantic search over the corpus; deterministic
  lookup of INCOTERMS and HS codes; structured summarisation of one known
  document.

## Consequences

**Positive**

- **Sits at the intersection of logistics, finance, and integration** —
  three of the four high-value backend hiring tracks in Brisbane.
- **Credible career story.** Aligns with prior logistics work (Rivigo) and
  fintech consulting (Unthinkable Solutions). The interview answer to "why
  this?" writes itself.
- **The domain rewards both RAG and structured tools.** Document Q&A is RAG;
  INCOTERM rule lookup is deterministic and shouldn't go through an LLM at
  all. Having both kinds of tool in one MCP server demonstrates the right
  judgement.
- **The vocabulary is real.** B/L, LC, CIF, FOB, demurrage, consignee.
  Reading the code, a reviewer sees domain awareness, not buzzword bingo.

**Negative**

- Less general-audience than, say, a customer-support chatbot. Compensated
  by being *more* memorable to a specialist hiring manager.
- The reference data is real and finite — we can't accidentally hallucinate
  INCOTERMS 2020 having 14 rules. Eleven, with specific scopes. The hard-
  coded enum in `Incoterm.java` is on purpose.

## Alternatives considered

- **Personal notes / journaling RAG** — done to death; no differentiation.
- **Codebase Q&A** — popular, but the eval landscape is saturated with
  competing products (Cursor, Aider, etc.).
- **Regulatory / compliance Q&A** — interesting but the cybersecurity
  angle of the author's profile already covers that ground via other
  projects.
- **Pure freight tariff comparison** — narrow; loses the trade-finance
  angle that broadens the appeal to banking employers.
