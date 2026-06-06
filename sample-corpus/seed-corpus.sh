#!/usr/bin/env bash
# seed-corpus.sh — POST every sample document into the running app.
# Run with `./sample-corpus/seed-corpus.sh` once the app is up on :8080.

set -euo pipefail
HOST="${CARGOIQ_HOST:-http://localhost:8080}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

post() {
  local title="$1" type="$2" file="$3"
  echo "→ $title"
  local payload
  payload=$(jq -n \
    --arg t "$title" \
    --arg ty "$type" \
    --arg u  "file://$file" \
    --arg tx "$(cat "$file")" \
    '{title:$t, type:$ty, sourceUri:$u, text:$tx}')
  curl -fsS -X POST "$HOST/api/v1/documents" \
    -H 'Content-Type: application/json' \
    -d "$payload" \
    | jq -r '"  ✓ " + .id + " (" + (.chunkCount|tostring) + " chunks)"'
}

post "BL — Pacific Roasters → Brisbane Coffee Co." \
     BILL_OF_LADING \
     "$DIR/bill-of-lading-pacific-roasters.txt"

post "Commercial Invoice — Pacific Roasters PRL-INV-2024-1107" \
     COMMERCIAL_INVOICE \
     "$DIR/commercial-invoice-pacific-roasters.txt"

post "LC — ANZ-LC-44782 (Brisbane Coffee Co.)" \
     LETTER_OF_CREDIT \
     "$DIR/letter-of-credit-anz-44782.txt"

post "BL — Karnataka Spice → Spice Route Pty Ltd" \
     BILL_OF_LADING \
     "$DIR/bill-of-lading-karnataka-spice.txt"

post "INCOTERMS 2020 Quick Reference" \
     REFERENCE \
     "$DIR/incoterms-2020-reference.txt"

echo "Done."
