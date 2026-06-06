# Model providers

`cargo-iq` is provider-agnostic. The chat model sits behind `ChatModelPort` and
the embedding model behind the pgvector adapter, so the domain and application
layers never name a vendor. Four providers ship on the classpath and you pick
one for chat and one for embeddings at runtime — no recompile, no code change.

| Provider | Chat | Embeddings | Credential | Notes |
|---|:---:|:---:|---|---|
| **OpenAI** | ✓ | ✓ | API key | Default. Cheap, fast. |
| **Anthropic (Claude)** | ✓ | — | API key | No embedding model — pair with another for embeddings. |
| **Google Gemini** | ✓ | ✓ | API key (AI Studio) | `text-embedding-004` is 768-dim. |
| **Ollama** | ✓ | ✓ | none (local) | Easiest end-to-end run; no keys. |

## Selecting a provider

Two properties decide what is active (everything else stays dormant — no
credentials required for providers you aren't using):

```yaml
spring.ai.model.chat:      openai      # openai | anthropic | google-genai | ollama
spring.ai.model.embedding: openai      # openai | google-genai | ollama
```

Set them via `AI_CHAT_PROVIDER` / `AI_EMBEDDING_PROVIDER` (see `.env.example`).

### A note on Gemini

Gemini **chat** is selectable at runtime like the others
(`AI_CHAT_PROVIDER=google-genai`). Gemini **embeddings** are special-cased: the
Google starter's embedding autoconfigs are excluded by default (one of them
binds its own `spring.ai.model.embedding.text` selector with `matchIfMissing`,
and another builds the Google client at startup regardless of selection). To use
Gemini embeddings, activate the **`gemini` profile**
(`SPRING_PROFILES_ACTIVE=dev,gemini`), which lifts the exclusion and selects
Gemini for both chat and embeddings at 768 dimensions. OpenAI, Anthropic, and
Ollama have no such quirk and switch purely by the two selector properties.

## The dimension constraint (important)

A pgvector column is declared with a fixed width, and an embedding model emits a
fixed-length vector. **They must match**, or inserts fail. The width is
`spring.ai.vectorstore.pgvector.dimensions` (`VECTOR_DIMENSIONS`):

| Embedding model | Dimensions |
|---|---|
| OpenAI `text-embedding-3-small` | 1536 |
| OpenAI `text-embedding-3-large` | 3072 |
| Gemini `text-embedding-004` | 768 |
| Ollama `nomic-embed-text` | 768 |

Switching embedding providers therefore means **re-indexing**: use a fresh
database or drop the `vector_store` table first. Document metadata in the JPA
`documents` table is unaffected — only the vectors need rebuilding. This is a
property of vector search, not of this app, and is the main reason the embedding
provider is treated as a deployment-time decision rather than a per-request one.

## Mix-and-match examples

```bash
# All-local, no keys (recommended first run):
ollama pull llama3.1 && ollama pull nomic-embed-text
SPRING_PROFILES_ACTIVE=dev,ollama   # flips both selectors to ollama, sets dims=768

# Claude for reasoning, OpenAI for embeddings (Anthropic has none):
AI_CHAT_PROVIDER=anthropic  AI_EMBEDDING_PROVIDER=openai  VECTOR_DIMENSIONS=1536

# Gemini chat only, OpenAI embeddings (no profile needed):
AI_CHAT_PROVIDER=google-genai  AI_EMBEDDING_PROVIDER=openai  VECTOR_DIMENSIONS=1536

# Gemini end-to-end (chat + embeddings) — needs the gemini profile:
GEMINI_API_KEY=...  SPRING_PROFILES_ACTIVE=dev,gemini
```

## Why this is clean

Retrieval quality and generation quality are separable concerns, and the best
chat model for a domain is rarely from the same vendor as the best/cheapest
embedding model. Decoupling the two selectors — and hiding both behind ports —
means the choice is a configuration decision, reversible in one line, and the
re-ranking / multi-query strategy layered on top in `AnswerQueryService` is
identical regardless of which vendor answers.
