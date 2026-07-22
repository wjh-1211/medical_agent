#!/usr/bin/env bash
set -euo pipefail

./mvnw -q test
./scripts/run_rag_evaluation.sh offline
./mvnw -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.medicalagent.evaluation.RegressionGateApplication
