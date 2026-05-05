#!/bin/bash
# =============================================================================
# Demo Seed Script for Burnout-as-a-Service
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   bash scripts/seed-demo.sh                          # seeds BEFORE (chaotic) state
#   bash scripts/seed-demo.sh http://localhost:8080 after   # seeds BEFORE then runs reshape
#   bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io before
#
# What it does:
#   1. Seeds 16 chaotic issues into IssueCache
#   2. (AFTER mode) Calls /demo/api/reshape to run the real supervisor agent
#   3. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   4. Seeds 14 days of dummy study data (alice, bob, carol, dave, roryp)
#
# BEFORE mode: All issues assigned to roryp, blank bodies, after-hours updates,
#   multiple URGENT unassigned items → stress score ~100 (CRITICAL)
#
# AFTER mode: Seeds the same chaotic issues, then calls the reshape endpoint
#   which runs the real supervisor agent (LLM or deterministic fallback) to
#   reorganize the workload. No hardcoded AFTER state.
#
# After running, open:
#   - /                    → landing page with links to all pages
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

# =========================================================================
# Chaotic issues — always seeded first (both BEFORE and AFTER modes).
# Everything piled on roryp, no descriptions, after-hours activity,
# unassigned URGENTs, high context switching.
# =========================================================================
if [ "$MODE" = "after" ]; then
    STEP_LABEL="Step 1/4"
else
    STEP_LABEL="Step 1/3"
fi
echo "📦 $STEP_LABEL: Seeding 16 CHAOTIC issues..."

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

# --- Fetch REAL issues from GitHub, then overlay chaos metadata ---
# Real titles, numbers, and labels are preserved. Bodies, assignees, and
# updatedAt timestamps are rewritten so stress metrics light up.
GH_AUTH=()
if [ -n "${GITHUB_TOKEN:-}" ]; then
    GH_AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN")
fi
RAW_ISSUES=$(curl -sf "${GH_AUTH[@]}" \
    "https://api.github.com/repos/$REPO/issues?state=open&per_page=30" || echo "")

if [ -z "$RAW_ISSUES" ] || [ "$(echo "$RAW_ISSUES" | jq 'type' 2>/dev/null)" != '"array"' ]; then
    echo "⚠️  GitHub fetch failed (rate limit or network). Falling back to synthetic titles." >&2
    RAW_ISSUES='[]'
fi

# Build chaos overlay via jq:
#   - Take up to 16 issues, drop pull_requests
#   - First 6 → unassigned URGENT + after-hours timestamps
#   - Rest → assigned to roryp + last-60-min staggered timestamps
#   - All bodies blanked (Clarity), camelCase fields
ISSUES=$(echo "$RAW_ISSUES" | jq \
    --arg ah1 "$AH1" --arg ah2 "$AH2" --arg ah3 "$AH3" \
    --arg r1 "$R1" --arg r2 "$R2" --arg r3 "$R3" --arg r4 "$R4" --arg r5 "$R5" \
    --arg r6 "$R6" --arg r7 "$R7" --arg r8 "$R8" --arg r9 "$R9" --arg r10 "$R10" \
    --arg week "$WEEK_AGO" --arg month "$MONTH_AGO" '
    [ .[] | select(.pull_request | not) ] | .[0:16] |
    [ to_entries[] | .key as $i | .value |
        ($i < 6) as $is_urgent |
        [$ah1,$ah2,$ah3,$ah1,$ah2,$ah3] as $ah |
        [$r1,$r2,$r3,$r4,$r5,$r6,$r7,$r8,$r9,$r10] as $recent |
        {
            number: .number,
            title: .title,
            body: "",
            labels: (
                [ .labels[]? | {name: .name} ] +
                (if $is_urgent then [{name:"urgent"},{name:"priority:critical"}] else [] end)
            ),
            assignees: (if $is_urgent then [] else [{login:"roryp"}] end),
            createdAt: (if $is_urgent then $month else $week end),
            updatedAt: (if $is_urgent then $ah[$i % 3] else $recent[($i - 6) % 10] end),
            state: "open"
        }
    ]
')

ISSUE_COUNT=$(echo "$ISSUES" | jq 'length')
if [ "$ISSUE_COUNT" -lt 4 ]; then
    echo "⚠️  Only $ISSUE_COUNT real issues fetched — chaos overlay needs ≥4. Falling back to synthetic." >&2
    ISSUES="[
        {\"number\":1,\"title\":\"Critical auth bypass in OAuth flow\",\"body\":\"\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"security\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R1\",\"state\":\"open\"},
        {\"number\":2,\"title\":\"URGENT: Production memory leak\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"bug\"}],\"assignees\":[],\"createdAt\":\"$TWO_WEEKS\",\"updatedAt\":\"$AH1\",\"state\":\"open\"},
        {\"number\":3,\"title\":\"URGENT: Database connection pool exhaustion\",\"body\":\"\",\"labels\":[{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH3\",\"state\":\"open\"},
        {\"number\":4,\"title\":\"Fix typo in README\",\"body\":\"\",\"labels\":[{\"name\":\"good-first-issue\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R4\",\"state\":\"open\"}
    ]"
else
    echo "  ↳ Fetched $ISSUE_COUNT real issues from GitHub; applied chaos overlay."
fi

# --- Step 1: Seed chaotic issues ---
curl -s -X POST "$BASE_URL/demo/api/seed" \
  -H 'Content-Type: application/json' \
  -d "{\"repo\":\"$REPO\",\"issues\":$ISSUES}" | python3 -m json.tool 2>/dev/null || cat

# --- Step 2 (AFTER mode only): Call reshape endpoint ---
if [ "$MODE" = "after" ]; then
    echo ""
    echo "🤖 Step 2/4: Running reshape (supervisor agent)..."
    RESHAPE_RESULT=$(curl -s -X POST "$BASE_URL/demo/api/reshape" \
      -H 'Content-Type: application/json' \
      -d "{\"repo\":\"$REPO\",\"userId\":\"roryp\"}")
    echo "$RESHAPE_RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f\"  Before: {d['beforeScore']} -> After: {d['afterScore']} ({d['afterLevel']})\")
print(f\"  Actions applied: {d['actionsApplied']}, LLM used: {d['llmUsed']}\")
" 2>/dev/null || echo "$RESHAPE_RESULT"
fi

# --- Checkins ---
echo ""
if [ "$MODE" = "after" ]; then
    CHECKIN_STEP="Step 3/4"
else
    CHECKIN_STEP="Step 2/3"
fi
echo "📊 $CHECKIN_STEP: Running checkins for roryp, alice, bob..."
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

if [ "$MODE" = "after" ]; then
    # Re-run reshape to restore the reshaped state
    curl -s -X POST "$BASE_URL/demo/api/reshape" \
      -H 'Content-Type: application/json' \
      -d "{\"repo\":\"$REPO\",\"userId\":\"roryp\"}" > /dev/null 2>&1
fi

# --- Seed study history ---
echo ""
if [ "$MODE" = "after" ]; then
    STUDY_STEP="Step 4/4"
else
    STUDY_STEP="Step 3/3"
fi
echo "📈 $STUDY_STEP: Seeding 14 days of study history..."
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
echo "   ${BASE_URL}/                              → Landing page"
echo "   ${BASE_URL}/checkin.html              → Stress check-in (use: roryp / roryp/burnout-app)"
echo "   ${BASE_URL}/flamegraph.html?repo=roryp/burnout-app  → Flamegraph"
echo "   ${BASE_URL}/study.html                → Study dashboard (click Load Data)"
