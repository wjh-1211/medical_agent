#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-offline}"

case "$MODE" in
  offline)
    exec ./mvnw -q compile exec:java -DskipTests \
      -Dexec.mainClass=com.medicalagent.evaluation.RagEvaluationApplication \
      -Dapp.profile=evaluation
    ;;
  local)
    exec ./mvnw -q compile exec:java -DskipTests \
      -Dexec.mainClass=com.medicalagent.evaluation.RagEvaluationApplication \
      -Dapp.profile=local \
      -Devaluation.mode=local \
      -Devaluation.localSampleLimit="${EVALUATION_LOCAL_SAMPLE_LIMIT:-3}"
    ;;
  *)
    echo "Usage: $0 [offline|local]" >&2
    exit 2
    ;;
esac
