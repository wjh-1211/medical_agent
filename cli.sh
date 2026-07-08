#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-test}"

exec ./mvnw -q -DskipTests exec:java \
  -Dexec.mainClass=com.medicalagent.cli.AgentCliApplication \
  -Dapp.profile="${PROFILE}"
