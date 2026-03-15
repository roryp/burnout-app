#!/bin/bash
# =============================================================================
# Demo Seed Script for Burnout-as-a-Service
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   bash scripts/seed-demo.sh                          # seeds BEFORE (chaotic) state
#   bash scripts/seed-demo.sh http://localhost:8080 after   # seeds AFTER (reshaped) state
#   bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io before
#
# What it does:
#   1. Seeds 16 issues into IssueCache (chaotic BEFORE or reshaped AFTER)
#   2. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   3. Seeds 14 days of dummy study data (alice, bob, carol, dave)
#
# BEFORE mode: All issues assigned to roryp, blank bodies, after-hours updates,
#   multiple URGENT unassigned items → stress score ~100 (CRITICAL)
#
# AFTER mode: Work delegated to alice/bob, clear descriptions, business-hours
#   updates, 3-3-3 structure → stress score ~31 (MODERATE)
#
# After running, open:
#   - /checkin.html         → stress check-in (enter roryp + roryp/burnout-app)
#   - /flamegraph.html      → flamegraph visualization
#   - /study.html           → researcher dashboard (click Load Data)
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
MODE="${2:-before}"
REPO="roryp/burnout-app"

echo "🔥 Seeding demo data ($MODE) on $BASE_URL ..."

# --- Generate current timestamps ---
# Cross-platform: try GNU date first, fall back to BSD (macOS)
ts() { date -u -d "$1" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v"$2" +%Y-%m-%dT%H:%M:%SZ; }

WEEK_AGO=$(ts '-7 days' '-7d')
TWO_WEEKS=$(ts '-14 days' '-14d')
MONTH_AGO=$(ts '-30 days' '-30d')
YESTERDAY=$(ts '-1 day' '-1d')

if [ "$MODE" = "before" ]; then
    # =========================================================================
    # BEFORE: Chaotic state — everything piled on roryp, no descriptions,
    # after-hours activity, unassigned URGENTs, high context switching
    # Target: stress ~100 (CRITICAL)
    # =========================================================================
    echo "📦 Step 1/3: Seeding 16 CHAOTIC issues (BEFORE reshape)..."

    R1=$(ts '-10 minutes' '-10M')
    R2=$(ts '-20 minutes' '-20M')
    R3=$(ts '-30 minutes' '-30M')
    R4=$(ts '-40 minutes' '-40M')
    R5=$(ts '-50 minutes' '-50M')
    R6=$(ts '-60 minutes' '-60M')
    R7=$(ts '-70 minutes' '-70M')
    R8=$(ts '-80 minutes' '-80M')
    R9=$(ts '-90 minutes' '-90M')
    R10=$(ts '-100 minutes' '-100M')
    AH1=$(date -u +%Y-%m-%dT03:00:00Z)
    AH2=$(date -u +%Y-%m-%dT04:00:00Z)
    AH3=$(date -u +%Y-%m-%dT22:00:00Z)

    ISSUES="[
        {\"number\":1,\"title\":\"Critical auth bypass in OAuth flow\",\"body\":\"\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"security\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R1\",\"state\":\"open\"},
        {\"number\":2,\"title\":\"Refactor agent orchestration layer\",\"body\":\"\",\"labels\":[{\"name\":\"architecture\"},{\"name\":\"deep-work\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R2\",\"state\":\"open\"},
        {\"number\":3,\"title\":\"Implement new feature flags system\",\"body\":\"\",\"labels\":[{\"name\":\"epic\"},{\"name\":\"feature\"},{\"name\":\"priority:critical\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$R3\",\"state\":\"open\"},
        {\"number\":4,\"title\":\"URGENT: Production memory leak\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"bug\"}],\"assignees\":[],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$AH1\",\"state\":\"open\"},
        {\"number\":5,\"title\":\"URGENT: API rate limiting broken\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"bug\"}],\"assignees\":[],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$AH2\",\"state\":\"open\"},
        {\"number\":6,\"title\":\"URGENT: Database connection pool exhaustion\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH3\",\"state\":\"open\"},
        {\"number\":7,\"title\":\"Fix typo in README\",\"body\":\"\",\"labels\":[{\"name\":\"good-first-issue\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R4\",\"state\":\"open\"},
        {\"number\":8,\"title\":\"Update Spring Boot to 3.5.11\",\"body\":\"\",\"labels\":[{\"name\":\"dependencies\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R5\",\"state\":\"open\"},
        {\"number\":9,\"title\":\"Something unclear\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$R6\",\"state\":\"open\"},
        {\"number\":10,\"title\":\"Another vague issue\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$R7\",\"state\":\"open\"},
        {\"number\":11,\"title\":\"CI pipeline failing intermittently\",\"body\":\"\",\"labels\":[{\"name\":\"ci\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R8\",\"state\":\"open\"},
        {\"number\":12,\"title\":\"Write API documentation\",\"body\":\"\",\"labels\":[{\"name\":\"documentation\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$R9\",\"state\":\"open\"},
        {\"number\":13,\"title\":\"Add dark mode toggle\",\"body\":\"\",\"labels\":[{\"name\":\"enhancement\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R10\",\"state\":\"open\"},
        {\"number\":14,\"title\":\"Fix CORS headers on demo endpoints\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$AH1\",\"state\":\"open\"},
        {\"number\":15,\"title\":\"Stale tracking issue from last quarter\",\"body\":\"\",\"labels\":[{\"name\":\"triage\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH2\",\"state\":\"open\"},
        {\"number\":16,\"title\":\"Upgrade Node.js to v22\",\"body\":\"\",\"labels\":[{\"name\":\"dependencies\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$AH3\",\"state\":\"open\"}
    ]"
else
    # =========================================================================
    # AFTER: Reshaped state — work delegated, clear descriptions, business
    # hours, 3-3-3 structure (1 deep + 3 quick + 3 maintenance for roryp)
    # Target: stress ~31 (MODERATE)
    # =========================================================================
    echo "📦 Step 1/3: Seeding 16 RESHAPED issues (AFTER reshape)..."

    BIZ1=$(date -u +%Y-%m-%dT10:00:00Z)
    BIZ2=$(date -u +%Y-%m-%dT11:00:00Z)
    BIZ3=$(date -u +%Y-%m-%dT14:00:00Z)
    BIZ4=$(date -u +%Y-%m-%dT15:00:00Z)
    BIZ5=$(date -u +%Y-%m-%dT16:00:00Z)

    ISSUES="[
        {\"number\":1,\"title\":\"Critical auth bypass in OAuth flow\",\"body\":\"Security vulnerability in the OAuth callback handler. Steps to reproduce: 1) Initiate OAuth flow 2) Modify callback URL. Fix: validate redirect_uri against allowlist.\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"security\"},{\"name\":\"deep-work\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ1\",\"state\":\"open\"},
        {\"number\":7,\"title\":\"Fix typo in README\",\"body\":\"Line 42: recieve should be receive. Simple find-and-replace.\",\"labels\":[{\"name\":\"good-first-issue\"},{\"name\":\"quick-win\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ2\",\"state\":\"open\"},
        {\"number\":14,\"title\":\"Fix CORS headers on demo endpoints\",\"body\":\"Add Access-Control-Allow-Origin for azurecontainerapps.io domains. One-line config change in SecurityConfig.java.\",\"labels\":[{\"name\":\"quick-win\"},{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ3\",\"state\":\"open\"},
        {\"number\":13,\"title\":\"Add dark mode toggle\",\"body\":\"Add CSS custom properties for dark/light themes. Toggle button in header. Store preference in localStorage.\",\"labels\":[{\"name\":\"quick-win\"},{\"name\":\"enhancement\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ4\",\"state\":\"open\"},
        {\"number\":8,\"title\":\"Update Spring Boot to 3.5.11\",\"body\":\"Bump spring-boot-starter-parent from 3.5.10 to 3.5.11 in pom.xml. Run full test suite after.\",\"labels\":[{\"name\":\"dependencies\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ5\",\"state\":\"open\"},
        {\"number\":11,\"title\":\"CI pipeline failing intermittently\",\"body\":\"Flaky test in IntegrationTest.java line 45. Root cause: race condition in async handler. Fix: add CountDownLatch.\",\"labels\":[{\"name\":\"ci\"},{\"name\":\"devops\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ1\",\"state\":\"open\"},
        {\"number\":16,\"title\":\"Upgrade Node.js to v22\",\"body\":\"Update Dockerfile and package.json engine field. Test MCP app build with Node 22 LTS.\",\"labels\":[{\"name\":\"dependencies\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$BIZ2\",\"state\":\"open\"},
        {\"number\":2,\"title\":\"Refactor agent orchestration layer\",\"body\":\"Extract supervisor pattern into separate module. Alice has context from last sprint.\",\"labels\":[{\"name\":\"architecture\"},{\"name\":\"deep-work\"}],\"assignees\":[{\"login\":\"alice\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ3\",\"state\":\"open\"},
        {\"number\":3,\"title\":\"Implement new feature flags system\",\"body\":\"Design doc approved. Start with LaunchDarkly SDK integration in backend.\",\"labels\":[{\"name\":\"epic\"},{\"name\":\"feature\"}],\"assignees\":[{\"login\":\"alice\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$BIZ4\",\"state\":\"open\"},
        {\"number\":12,\"title\":\"Write API documentation\",\"body\":\"Generate OpenAPI specs from Spring annotations. Add examples for /api/stress and /api/reshape.\",\"labels\":[{\"name\":\"documentation\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"alice\"}],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$BIZ5\",\"state\":\"open\"},
        {\"number\":4,\"title\":\"Investigate production memory leak\",\"body\":\"Heap dump shows leak in connection pool. Bob to profile with JFR and report findings.\",\"labels\":[{\"name\":\"bug\"},{\"name\":\"priority:high\"}],\"assignees\":[{\"login\":\"bob\"}],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$BIZ1\",\"state\":\"open\"},
        {\"number\":5,\"title\":\"Review API rate limiting config\",\"body\":\"Rate limiter config may need tuning. Bob to benchmark current limits and propose changes.\",\"labels\":[{\"name\":\"bug\"},{\"name\":\"maintenance\"}],\"assignees\":[{\"login\":\"bob\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$BIZ2\",\"state\":\"open\"},
        {\"number\":6,\"title\":\"Database connection pool exhaustion\",\"body\":\"Increase max pool size from 10 to 25. Add connection timeout of 30s. Monitor with Actuator.\",\"labels\":[{\"name\":\"priority:high\"},{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"bob\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$BIZ3\",\"state\":\"open\"},
        {\"number\":9,\"title\":\"Investigate unclear bug report\",\"body\":\"Needs reproduction steps. Deferred until reporter provides more info.\",\"labels\":[{\"name\":\"triage\"},{\"name\":\"bug\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$YESTERDAY\",\"state\":\"open\"},
        {\"number\":10,\"title\":\"Review vague feature request\",\"body\":\"Waiting on product team clarification. Parked for next sprint planning.\",\"labels\":[{\"name\":\"triage\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$YESTERDAY\",\"state\":\"open\"},
        {\"number\":15,\"title\":\"Stale tracking issue from last quarter\",\"body\":\"Closed as stale. No activity in 60 days. Can reopen if needed.\",\"labels\":[{\"name\":\"triage\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$YESTERDAY\",\"state\":\"closed\"}
    ]"
fi

# --- Step 1: Seed issues ---
curl -s -X POST "$BASE_URL/demo/api/seed" \
  -H 'Content-Type: application/json' \
  -d "{\"repo\":\"$REPO\",\"issues\":$ISSUES}" | python3 -m json.tool 2>/dev/null || cat

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

# Re-seed issues after checkins to ensure curated data is the final cache state
curl -s -X POST "$BASE_URL/demo/api/seed" \
  -H 'Content-Type: application/json' \
  -d "{\"repo\":\"$REPO\",\"issues\":$ISSUES}" > /dev/null 2>&1

# --- Step 3: Seed 14 days of dummy study data ---
echo ""
echo "📈 Step 3/3: Seeding 14 days of study history..."
curl -s -X POST "$BASE_URL/demo/api/study/seed" | python3 -m json.tool 2>/dev/null || cat

# --- Validate ---
echo ""
echo "📊 Validation for roryp:"
RESULT=$(curl -s -X POST "$BASE_URL/demo/api/checkin" \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":\"roryp\",\"repo\":\"$REPO\",\"selfScore\":50}")
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
b = d['breakdown']
print(f\"   Stress: {d['stressScore']} ({d['stressLevel']})\")
print(f\"   Workload={b['workload']} Chaos={b['chaos']} CtxSwitch={b['contextSwitching']} Clarity={b['clarity']} Sustained={b['sustained']} AfterHrs={b['afterHours']}\")
" 2>/dev/null || echo "$RESULT"

echo ""
echo "✅ Demo data seeded ($MODE)! Open these pages:"
echo "   ${BASE_URL}/checkin.html              → Stress check-in (use: roryp / roryp/burnout-app)"
echo "   ${BASE_URL}/flamegraph.html?repo=roryp/burnout-app  → Flamegraph"
echo "   ${BASE_URL}/study.html                → Study dashboard (click Load Data)"
