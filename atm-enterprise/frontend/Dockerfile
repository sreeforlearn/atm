# ---------- Builder ----------
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps (cached layer)
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

# Build
COPY . .
RUN npm run build

# ---------- Runner ----------
FROM nginx:alpine AS runner
WORKDIR /usr/share/nginx/html

# SPA nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Built artifacts
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
