#!/usr/bin/env bash
set -euo pipefail

START_MS=$(date +%s%3N)
OUTPUT_FILE="$(mktemp)"
trap 'rm -f "$OUTPUT_FILE"' EXIT

printf '你好\n/exit\n' | ./cli.sh local >"$OUTPUT_FILE" 2>&1

END_MS=$(date +%s%3N)
TOTAL_MS=$((END_MS - START_MS))

echo "Benchmark: ./cli.sh local -> 你好 -> /exit"
echo "Total wall time: ${TOTAL_MS} ms"
echo
cat "$OUTPUT_FILE"
