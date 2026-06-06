# cargo-iq web

A modern single-page client for the cargo-iq API — React 18 + TypeScript + Vite
+ Tailwind, with framer-motion micro-interactions and an Aceternity/21st.dev-style
visual language (aurora fields, glassmorphism, spotlight).

## What it covers

The UI exercises the backend end to end:

- **Auth** — register / sign in, JWT stored client-side, role-aware UI (ADMIN
  vs USER gates ingest and delete).
- **Dashboard** — live corpus overview from `/api/v1/overview` (totals + type and
  INCOTERM breakdowns, animated).
- **Documents** — browse, filter by type, ingest (ADMIN), delete (ADMIN).
- **Ask the corpus** — RAG query with the grounded/ungrounded badge and citation
  cards scored back to the source chunk.
- **Reference** — INCOTERMS 2020 rule cards and HS-code lookup/search.

## Run it

```bash
cd frontend
cp .env.example .env        # optional; leave VITE_API_BASE_URL empty for the dev proxy
npm install
npm run dev                 # http://localhost:5173
```

The dev server proxies `/api` to `http://localhost:8080`, so start the backend
first (`docker compose up` from the repo root). Sign in with the dev bootstrap
admin (`admin` / `admin12345`) and seed the corpus with
`./sample-corpus/seed-corpus.sh` to see the dashboard and query fill in.

## Build

```bash
npm run build               # type-checks (tsc) then bundles to dist/
npm run preview             # serve the production build locally
```

## Structure

```
src/
├── lib/        api client (typed), shared types, helpers
├── auth/       JWT auth context (login/register/logout, role flags)
├── components/ layout, route guard, UI primitives + decorative backgrounds
└── pages/      Login, Dashboard, Documents, Query, Reference
```

The API base URL is the only deployment knob (`VITE_API_BASE_URL`); everything
else is driven by the backend's responses.
