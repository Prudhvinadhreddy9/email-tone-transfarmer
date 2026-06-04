#!/usr/bin/env bash
# Vite dev server (HMR). Pulls VITE_API_BASE_URL from a frontend/.env or falls
# back to http://localhost:8080.
set -euo pipefail

cd "$(dirname "$0")/../frontend"

if [ ! -d node_modules ]; then
  echo "Installing dependencies..."
  npm install --no-audit --no-fund
fi

npm run dev
