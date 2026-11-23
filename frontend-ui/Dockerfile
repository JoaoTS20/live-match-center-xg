# Stage 1: Build Angular production bundle
FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve static files + reverse proxy with non-root Nginx
FROM nginxinc/nginx-unprivileged:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist/frontend-ui/browser /usr/share/nginx/html

EXPOSE 8080
