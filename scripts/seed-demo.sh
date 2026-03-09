#!/bin/bash
# =============================================================================
# Demo Seed Script for Burnout-as-a-Service
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   bash scripts/seed-demo.sh                          # local (http://localhost:8080)
#   bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io
#
# What it does:
#   1. Seeds 16 issues with current timestamps (camelCase!) into IssueCache
#   2. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   3. Seeds 14 days of dummy study data (alice, bob, carol, dave)
#
# After running, open:
#   - /checkin.html         → stress check-in (enter roryp + roryp/burnout-app)
#   - /flamegraph.html      → flamegraph visualization
#   - /study.html           → researcher dashboard (click Load Data)
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
REPO="roryp/burnout-app"

echo "🔥 Seeding demo data on $BASE_URL ..."

# --- Generate current timestamps ---
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
RECENT=$(date -u -d '-15 minutes' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-15M +%Y-%m-%dT%H:%M:%SZ)
RECENT2=$(date -u -d '-30 minutes' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30M +%Y-%m-%dT%H:%M:%SZ)
RECENT3=$(date -u -d '-45 minutes' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-45M +%Y-%m-%dT%H:%M:%SZ)
AFTER_HOURS=$(date -u -d 'today 03:00' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u +%Y-%m-%dT03:00:00Z)
YESTERDAY=$(date -u -d '-1 day' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-1d +%Y-%m-%dT%H:%M:%SZ)
TWO_DAYS_AGO=$(date -u -d '-2 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-2d +%Y-%m-%dT%H:%M:%SZ)
WEEK_AGO=$(date -u -d '-7 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-7d +%Y-%m-%dT%H:%M:%SZ)
TWO_WEEKS_AGO=$(date -u -d '-14 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-14d +%Y-%m-%dT%H:%M:%SZ)
MONTH_AGO=$(date -u -d '-30 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30d +%Y-%m-%dT%H:%M:%SZ)
TWO_MONTHS_AGO=$(date -u -d '-60 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-60d +%Y-%m-%dT%H:%M:%SZ)

# --- Step 1: Seed issues (camelCase fields, current timestamps) ---
echo "📦 Step 1/3: Seeding 16 issues..."
curl -s -X POST "$BASE_URL/demo/api/seed" \
  -H 'Content-Type: application/json' \
  -d "{\"repo\":\"$REPO\",\"issues\":[
    {\"number\":1,\"title\":\"Critical auth bypass in OAuth flow\",\"body\":\"Security vulnerability in the OAuth callback handler.\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"security\"},{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"alice\"}],\"createdAt\":\"$TWO_DAYS_AGO\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":2,\"title\":\"Refactor agent orchestration layer\",\"body\":\"The AgentOrchestrator has grown too complex.\",\"labels\":[{\"name\":\"architecture\"},{\"name\":\"deep-work\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"alice\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$RECENT2\",\"state\":\"open\"},
    {\"number\":3,\"title\":\"Fix typo in README\",\"body\":\"Small typo fix needed\",\"labels\":[{\"name\":\"good-first-issue\"},{\"name\":\"quick-win\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"bob\"}],\"createdAt\":\"$YESTERDAY\",\"updatedAt\":\"$RECENT3\",\"state\":\"open\"},
    {\"number\":4,\"title\":\"Update Spring Boot to 3.5.11\",\"body\":\"Dependency bump\",\"labels\":[{\"name\":\"dependencies\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"bob\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":5,\"title\":\"URGENT: Production memory leak\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"bug\"},{\"name\":\"priority:high\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_WEEKS_AGO\",\"updatedAt\":\"$AFTER_HOURS\",\"state\":\"open\"},
    {\"number\":6,\"title\":\"URGENT: API rate limiting broken\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_DAYS_AGO\",\"updatedAt\":\"$AFTER_HOURS\",\"state\":\"open\"},
    {\"number\":7,\"title\":\"URGENT: Database connection pool exhaustion\",\"body\":\"Pool runs out under load.\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"alice\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":8,\"title\":\"Add dark mode toggle\",\"body\":\"Users want a dark mode option.\",\"labels\":[{\"name\":\"enhancement\"},{\"name\":\"size:s\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$RECENT2\",\"state\":\"open\"},
    {\"number\":9,\"title\":\"Something unclear\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_MONTHS_AGO\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":10,\"title\":\"Another vague issue\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$RECENT2\",\"state\":\"open\"},
    {\"number\":11,\"title\":\"CI pipeline failing intermittently\",\"body\":\"GitHub Actions fails randomly on test step.\",\"labels\":[{\"name\":\"ci\"},{\"name\":\"devops\"},{\"name\":\"chore\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"bob\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$RECENT3\",\"state\":\"open\"},
    {\"number\":12,\"title\":\"Write API documentation\",\"body\":\"Need OpenAPI specs for all endpoints.\",\"labels\":[{\"name\":\"documentation\"},{\"name\":\"tech-debt\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"alice\"}],\"createdAt\":\"$TWO_WEEKS_AGO\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":13,\"title\":\"Implement feature flags\",\"body\":\"Large epic spanning multiple services.\",\"labels\":[{\"name\":\"epic\"},{\"name\":\"feature\"},{\"name\":\"architecture\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AFTER_HOURS\",\"state\":\"open\"},
    {\"number\":14,\"title\":\"Fix CORS headers on demo endpoints\",\"body\":\"Quick config change needed\",\"labels\":[{\"name\":\"quick-win\"},{\"name\":\"bug\"},{\"name\":\"size:s\"}],\"assignees\":[{\"login\":\"roryp\"},{\"login\":\"bob\"}],\"createdAt\":\"$YESTERDAY\",\"updatedAt\":\"$RECENT\",\"state\":\"open\"},
    {\"number\":15,\"title\":\"Stale tracking issue from last quarter\",\"body\":\"\",\"labels\":[{\"name\":\"triage\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_MONTHS_AGO\",\"updatedAt\":\"$RECENT3\",\"state\":\"open\"},
    {\"number\":16,\"title\":\"Upgrade Node.js to v22\",\"body\":\"MCP app should use latest LTS\",\"labels\":[{\"name\":\"dependencies\"},{\"name\":\"refactor\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_WEEKS_AGO\",\"updatedAt\":\"$RECENT2\",\"state\":\"open\"}
  ]}" | python3 -m json.tool 2>/dev/null || cat

# --- Step 2: Run checkins to generate study snapshots ---
echo ""
echo "📊 Step 2/3: Running checkins for roryp, alice, bob..."
for user in roryp alice bob; do
  for i in 1 2 3; do
    SELF=$((RANDOM % 60 + 20))
    RESULT=$(curl -s -X POST "$BASE_URL/demo/api/checkin" \
      -H 'Content-Type: application/json' \
      -d "{\"userId\":\"$user\",\"repo\":\"$REPO\",\"selfScore\":$SELF}")
    SCORE=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['stressScore'])" 2>/dev/null || echo "?")
    LEVEL=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['stressLevel'])" 2>/dev/null || echo "?")
    echo "  $user checkin $i: score=$SCORE level=$LEVEL"
  done
done

# --- Step 3: Seed 14 days of dummy study data ---
echo ""
echo "📈 Step 3/3: Seeding 14 days of study history..."
curl -s -X POST "$BASE_URL/demo/api/study/seed" | python3 -m json.tool 2>/dev/null || cat

echo ""
echo "✅ Demo data seeded! Open these pages:"
echo "   ${BASE_URL}/checkin.html              → Stress check-in (use: roryp / roryp/burnout-app)"
echo "   ${BASE_URL}/flamegraph.html?repo=roryp/burnout-app  → Flamegraph"
echo "   ${BASE_URL}/study.html                → Study dashboard (click Load Data)"
