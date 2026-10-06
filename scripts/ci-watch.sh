#!/usr/bin/env bash
# ci-watch.sh — poll the latest Actions run for the current branch and
# dump the failing logs when it completes.
#
# Usage:
#   ./scripts/ci-watch.sh [branch] [timeout_seconds]
#
# Requires: gh (authenticated with access to the repo).
# Exit code: 0 if the run succeeds, 1 otherwise.
set -u
BRANCH="${1:-$(git branch --show-current)}"
TIMEOUT="${2:-1200}"
POLL=20

echo "Watching latest run on branch '$BRANCH' (timeout ${TIMEOUT}s)..."
RUN_ID=""
elapsed=0
while [ "$elapsed" -lt "$TIMEOUT" ]; do
  # Newest run first; pick the first one for our branch + event.
  RUN_ID="$(gh run list --branch "$BRANCH" --limit 5 --json databaseId,headBranch,status \
    --jq '[.[] | select(.headBranch=="'"$BRANCH"'")][0].databaseId' 2>/dev/null)"
  if [ -n "$RUN_ID" ] && [ "$RUN_ID" != "null" ]; then
    STATUS="$(gh run view "$RUN_ID" --json status --jq .status 2>/dev/null)"
    if [ "$STATUS" = "completed" ]; then
      break
    fi
  else
    echo "  no run found yet for '$BRANCH'..."
  fi
  sleep "$POLL"
  elapsed=$((elapsed + POLL))
done

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
  echo "Timed out waiting for a run on '$BRANCH'."
  exit 1
fi

echo "Run $RUN_ID finished:"
gh run view "$RUN_ID" --json conclusion,jobs \
  --jq '.jobs[] | "\(.name): \(.conclusion)"' 2>/dev/null || gh run view "$RUN_ID" 2>&1 | tail -20

CONCLUSION="$(gh run view "$RUN_ID" --json conclusion --jq .conclusion 2>/dev/null)"
if [ "$CONCLUSION" = "success" ]; then
  echo "CI GREEN on run $RUN_ID."
  exit 0
fi

echo "--- failing logs for run $RUN_ID ---"
gh run view "$RUN_ID" --log-failed 2>&1 | tail -120
exit 1
