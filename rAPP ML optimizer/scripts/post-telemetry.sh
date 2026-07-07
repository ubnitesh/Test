#!/usr/bin/env bash
# Post each cell from samples/telemetry-cells.json to the rAPP ML Optimizer R1 telemetry endpoint.
#
# Critical congestion is declared when BOTH conditions are met:
#   - prbUtilization > 80.0%
#   - rsrq           < -12.0 dB
#
# Usage:
#   ./scripts/post-telemetry.sh
#   BASE_URL=http://localhost:8080 ./scripts/post-telemetry.sh
#   JSON_FILE=samples/telemetry-cells.json ./scripts/post-telemetry.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ENDPOINT="${BASE_URL}/r1/telemetry"
JSON_FILE="${JSON_FILE:-$(dirname "$0")/../samples/telemetry-cells.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq is required. Install from https://jqlang.github.io/jq/" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl is required." >&2
  exit 1
fi

if [[ ! -f "$JSON_FILE" ]]; then
  echo "Error: JSON file not found: $JSON_FILE" >&2
  exit 1
fi

echo "=== Critical congestion conditions ==="
jq -r '.criticalCongestionConditions | .description, (.rules[] | "  \(.kpi) \(.operator) \(.threshold) \(.unit) — \(.notes)")' "$JSON_FILE"
echo ""

NEIGHBORS=()
while IFS= read -r neighbor; do
  NEIGHBORS+=("$neighbor")
done < <(jq -r '.neighborCells[]' "$JSON_FILE")

QUERY=""
for neighbor in "${NEIGHBORS[@]}"; do
  QUERY="${QUERY}neighborCells=${neighbor}&"
done
QUERY="${QUERY%&}"
if [[ -n "$QUERY" ]]; then
  ENDPOINT="${ENDPOINT}?${QUERY}"
fi

CELL_COUNT=$(jq '.cells | length' "$JSON_FILE")
echo "Posting ${CELL_COUNT} cells to ${ENDPOINT}"
echo ""

INDEX=0
while [[ $INDEX -lt $CELL_COUNT ]]; do
  CELL_ID=$(jq -r ".cells[$INDEX].cellId" "$JSON_FILE")
  EXPECTED=$(jq -r ".cells[$INDEX].expectedCongestion // \"UNKNOWN\"" "$JSON_FILE")
  PAYLOAD=$(jq -c ".cells[$INDEX] | {cellId, rsrp, rsrq, activeUsers, prbUtilization}" "$JSON_FILE")

  echo "--- [$((INDEX + 1))/${CELL_COUNT}] ${CELL_ID} (expected: ${EXPECTED}) ---"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | sed '$d')

  if [[ "$HTTP_CODE" == "202" ]]; then
    CRITICAL=$(echo "$BODY" | jq -r '.criticalCongestion')
    POLICY=$(echo "$BODY" | jq -r '.policyId // "none"')
    OFFLOAD=$(echo "$BODY" | jq -r '.recommendation.offloadPercentage // 0')
    echo "  HTTP ${HTTP_CODE} | criticalCongestion=${CRITICAL} | offload=${OFFLOAD}% | policyId=${POLICY}"
  else
    echo "  HTTP ${HTTP_CODE} | ${BODY}"
  fi
  echo ""

  INDEX=$((INDEX + 1))
done

echo "Done."
