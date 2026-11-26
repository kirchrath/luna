# Image of the Luna frontend: build with Node, serve the static files with nginx (without root).
# Build on the server: docker build --platform linux/amd64 -t luna-frontend:<version> frontend

FROM node:24.22.3-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.30.6-alpine
COPY nginx/default.conf nginx/security-headers.inc /etc/nginx/conf.d/
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
