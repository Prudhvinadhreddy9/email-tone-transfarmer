#!/usr/bin/env bash
# Sanity curl for the running backend. Requires OPENAI_API_KEY exported and
# the backend running on http://localhost:8080.
#
# Cost note: each run is a real LLM call (~$0.0003 with gpt-4o-mini).
set -euo pipefail

curl -sS -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/json" \
  -d '{
    "originalEmail": "Hey can you send me the report ASAP I really need it",
    "tone": "PROFESSIONAL",
    "context": "my manager",
    "preserveLength": false
  }' \
  | python -m json.tool
