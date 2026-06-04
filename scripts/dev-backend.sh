#!/usr/bin/env bash
# Run the backend with Spring Boot DevTools-style hot reload (via spring-boot:run).
# Reads .env from the project root if present.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "WARNING: OPENAI_API_KEY is not set. /api/transform will fail."
fi

cd backend
mvn spring-boot:run
