# Two-stage: install + build, then serve the static dist with nginx.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
# The base URL is baked in at build time (Vite static replacement). Override
# via build-arg if you need to point at a different backend.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# A minimal SPA-fallback config: any path that doesn't match a file falls
# back to index.html so client-side routes work. We don't have routes yet
# but it's the standard React-on-nginx setup.
RUN printf 'server {\n  listen 5173;\n  root /usr/share/nginx/html;\n  index index.html;\n  location / { try_files $uri /index.html; }\n}\n' > /etc/nginx/conf.d/default.conf
EXPOSE 5173
