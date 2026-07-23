#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-offline}"
STRATEGY="${2:-vector}"

case "$MODE" in
  offline)
    REPORT_PATH="evaluation-report.json"
    SUMMARY_PATH="worklog/T013_RAG_EVALUATION_SUMMARY.md"
    if [[ "$STRATEGY" == "hybrid" ]]; then
      REPORT_PATH="evaluation/hybrid-rag-report.json"
      SUMMARY_PATH="worklog/T018_HYBRID_RAG_SUMMARY.md"
    elif [[ "$STRATEGY" != "vector" ]]; then
      echo "Usage: $0 offline [vector|hybrid]" >&2
      exit 2
    fi
    exec ./mvnw -q compile exec:java -DskipTests \
      -Dexec.mainClass=com.medicalagent.evaluation.RagEvaluationApplication \
      -Dapp.profile=evaluation \
      -Devaluation.retrievalStrategy="$STRATEGY" \
      -Devaluation.reportPath="$REPORT_PATH" \
      -Devaluation.markdownSummaryPath="$SUMMARY_PATH"
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
