# syntax=docker/dockerfile:1.7
# -----------------------------------------------------------------------------
# cargo-iq web SPA — multi-stage build.
#   Stage 1: build the Vite/React bundle with Node.
#   Stage 2: serve the static bundle from nginx and reverse-proxy /api to `app`.
#
# VITE_API_BASE_URL is intentionally left empty so the client uses same-origin
# /api/... paths, which nginx proxies to the backend — no CORS in the single
# compose deployment.
# -----------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Cache dependencies separately from source so most rebuilds skip npm install.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: nginx runtime
# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
