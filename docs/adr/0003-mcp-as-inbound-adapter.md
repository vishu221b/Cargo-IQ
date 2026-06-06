# ADR-0003 — MCP server as an inbound adapter, not a separate service

**Status:** Accepted
**Date:** 2026-05-27

## Context

The Model Context Protocol (MCP) is a way for LLM clients (Claude Desktop,
Cursor, the MCP Inspector) to discover and invoke tools, prompts, and
resources from a server. We want LLMs to be able to call our RAG and lookup
capabilities directly.

Two options:

1. **Standalone MCP server** — a separate Spring Boot project that talks to
   the main app over REST. Familiar microservice pattern.
2. **Embedded MCP server** — same JVM as the main app; MCP tools live in
   `adapter/in/mcp/` alongside REST controllers.

## Decision

Embed the MCP server in the main app.

In hexagonal terms, an MCP tool is *structurally identical* to a REST
controller: an inbound adapter that translates a protocol-specific request
into a use-case invocation. The application layer doesn't care whether the
caller arrived via HTTP+JSON or MCP+JSON-RPC.

Each MCP tool's body is "build a domain value object, call the use case,
return". Same body shape as a controller. Different annotations.

## Consequences

**Positive**

- **Zero duplication.** Every MCP tool calls the same use case its REST
  twin would. No business logic in either layer.
- **Single deployment.** One Docker image, one container, one network
  boundary, one set of metrics — the right default at this scale.
- **Single source of truth for prompts and grounding.** The system prompt
  in `SpringAiChatModelAdapter` applies whether the question came via REST
  or via MCP — there's no "MCP path" that could drift.
- **Testability is the same.** A fake `AnswerQueryUseCase` exercises both
  the controller and the tool with the same logic.

**Negative**

- The app process now serves two protocols. We accept the small startup
  cost (Spring AI's MCP server starter adds ~100ms) for the operational
  simplicity.
- If we ever need to scale the MCP and REST surfaces independently, we
  split them later. The split is mechanical because the layer below — the
  use cases — is already isolated.

## Alternatives considered

- **Standalone MCP server hitting REST.** Doubles the deployment surface
  and introduces a network hop for no architectural gain. Rejected.
- **MCP-only, no REST.** Locks out browser, curl, Swagger UI, and any
  non-LLM caller. Rejected.
