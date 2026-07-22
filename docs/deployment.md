# Deployment

The whole stack runs locally with a single `docker compose up` (web + API +
pgvector + Adminer). This guide covers deploying it to a cloud host. Two paths
are documented: **Fly.io** (config included) and **Railway**.

> The app is **zero-key by default** (mock chat + mock embedding), so it will
> boot and serve without any provider keys. Users can add their own OpenAI /
> Anthropic keys at runtime from the in-app **Settings** page.

## Prerequisites (any host)

The backend needs a **Postgres with the `pgvector` extension**. Set these as
secrets/env on the backend service:

| Variable | Notes |
| --- | --- |
| `POSTGRES_URL` | JDBC url, e.g. `jdbc:postgresql://host:5432/cargoiq` |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | DB credentials |
| `JWT_SECRET` | ≥ 32 bytes — signs auth tokens |
| `SECRETS_KEY`, `SECRETS_SALT` | encrypt per-user API keys (rotating them invalidates stored keys) |
| `CORS_ALLOWED_ORIGINS` | the deployed SPA origin(s) |

Flyway creates the schema on first boot; the pgvector store initialises itself.

## Fly.io

A [`fly.toml`](../fly.toml) is included for the backend.

```bash
fly launch --no-deploy                      # creates the app from fly.toml
fly postgres create --name cargo-iq-db      # a Postgres cluster (enable pgvector)
fly postgres connect -a cargo-iq-db -c "CREATE EXTENSION IF NOT EXISTS vector;"

# Wire the DB + secrets into the backend
fly secrets set \
  POSTGRES_URL="jdbc:postgresql://cargo-iq-db.flycast:5432/cargo_iq" \
  POSTGRES_USER="postgres" POSTGRES_PASSWORD="<from fly>" \
  JWT_SECRET="$(openssl rand -hex 32)" \
  SECRETS_KEY="$(openssl rand -hex 24)" SECRETS_SALT="$(openssl rand -hex 8)"

fly deploy
```

**Frontend:** deploy `frontend/Dockerfile` as a second Fly app, or host the built
SPA anywhere. Point it at the API by building with `VITE_API_BASE_URL=https://cargo-iq.fly.dev`,
and add that SPA origin to `CORS_ALLOWED_ORIGINS` on the backend. (Or keep the
nginx same-origin proxy from `frontend/nginx.conf` and deploy the two together.)

## Railway

Railway can deploy each service from this repo:

1. **Postgres**: add the Postgres plugin, then run `CREATE EXTENSION IF NOT EXISTS vector;`
   (or use a pgvector template).
2. **Backend**: new service from the repo root `Dockerfile`; set the env vars
   from the table above (`POSTGRES_URL` etc.).
3. **Frontend**: new service from `frontend/Dockerfile`; either front the backend
   with the included nginx proxy (same-origin, no CORS) or set `VITE_API_BASE_URL`
   at build time and add the SPA origin to `CORS_ALLOWED_ORIGINS`.

## Notes

- **pgvector dimensions** must match the active embedding model
  (`VECTOR_DIMENSIONS`: mock/OpenAI-3-small = 1536, Gemini/Ollama = 768). Changing
  the embedding provider means re-indexing — recreate the volume/DB.
- **Same-origin is simplest.** The included `frontend/nginx.conf` serves the SPA
  and reverse-proxies `/api`, `/mcp`, `/actuator` to the backend, so there's no
  CORS to configure — mirror that topology in production if you can.
