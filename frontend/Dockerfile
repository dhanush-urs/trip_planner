# =============================================================================
# TripForge Frontend Dockerfile
# Build: Vite → static files
# Serve: Nginx with SPA routing
# =============================================================================

# ── Build stage ───────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies (cached layer)
COPY package.json ./
RUN npm install

# Copy source and build
COPY . .

# Bake the API base URL at build time
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ── Runtime stage: Nginx ──────────────────────────────────────────────────────
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy built assets
COPY --from=builder /app/dist .

# Nginx config: SPA routing + API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
