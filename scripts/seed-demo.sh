#!/bin/bash
# =============================================================================
# Demo Seed Script for Burnout-as-a-Service (bash)
# =============================================================================
# Mirrors scripts/seed-demo.ps1 — fetches REAL GitHub issues for roryp/burnout-app
# and overlays a chaos pattern that produces stress=58/HIGH (matches README).
#
# Usage:
#   bash scripts/seed-demo.sh                                                    # local, BEFORE
#   bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io
#   bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io after
#
# What it does:
#   1. Fetches up to 30 open issues from github.com/roryp/burnout-app
#   2. Drops PRs, takes first 16
#   3. Overlays chaos:
#        - First 3  -> unassigned + urgent/priority:critical + after-hours updatedAt
#        - Next 3   -> URGENT assigned to alice/bob/carol (teammate fires)
#        - Last 10  -> assigned to roryp + last-100-min staggered updatedAt
#        - All bodies blanked (Clarity hit)
#   4. POSTs to /demo/api/seed
#   5. (AFTER mode) Calls /demo/api/reshape
#   6. Runs 3 checkins each for roryp/alice/bob
#   7. Re-seeds chaotic issues so the cache reflects the demo state
#   8. Seeds 14 days of study history
#
# Expected result: roryp -> stress 58 (HIGH).  (After reshape: ~8 / LOW.)
# Falls back to a 16-issue synthetic set when GitHub is unreachable / rate-limited.
#
# Requires: bash, curl, jq
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
MODE="${2:-before}"
REPO="roryp/burnout-app"

if ! command -v jq >/dev/null 2>&1; then
    echo "❌ jq is required but not installed. Install via: apt-get install jq | brew install jq | choco install jq" >&2
    echo "   On Windows, prefer scripts/seed-demo.ps1 which has no external dependencies." >&2
    exit 1
fi

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
#   - First 3 → unassigned URGENT + after-hours timestamps (pre-pass triages these)
#   - Next 3  → URGENT assigned to alice/bob/carol (teammate fires)
#   - Last 10 → assigned to roryp + last-100-min staggered timestamps
#   - All bodies blanked (Clarity), camelCase fields
ISSUES=$(echo "$RAW_ISSUES" | jq \
    --arg ah1 "$AH1" --arg ah2 "$AH2" --arg ah3 "$AH3" \
    --arg r1 "$R1" --arg r2 "$R2" --arg r3 "$R3" --arg r4 "$R4" --arg r5 "$R5" \
    --arg r6 "$R6" --arg r7 "$R7" --arg r8 "$R8" --arg r9 "$R9" --arg r10 "$R10" \
    --arg week "$WEEK_AGO" --arg month "$MONTH_AGO" '
    [ .[] | select(.pull_request | not) ] | .[0:16] |
    [ to_entries[] | .key as $i | .value |
        ($i < 3) as $is_unassigned_urgent |
        (($i >= 3) and ($i < 6)) as $is_teammate_urgent |
        ($is_unassigned_urgent or $is_teammate_urgent) as $is_urgent |
        ["alice","bob","carol"] as $teammates |
        [$ah1,$ah2,$ah3] as $ah |
        [$r1,$r2,$r3,$r4,$r5,$r6,$r7,$r8,$r9,$r10] as $recent |
        {
            number: .number,
            title: .title,
            body: "",
            labels: (
                [ .labels[]? | {name: .name} ] +
                (if $is_urgent then [{name:"urgent"},{name:"priority:critical"}] else [] end)
            ),
            assignees: (
                if $is_unassigned_urgent then []
                elif $is_teammate_urgent then [{login: $teammates[($i - 3) % 3]}]
                else [{login:"roryp"}] end
            ),
            createdAt: (if $is_urgent then $month else $week end),
            updatedAt: (if $is_urgent then $ah[$i % 3] else $recent[($i - 6) % 10] end),
            state: "open"
        }
    ]
')

ISSUE_COUNT=$(echo "$ISSUES" | jq 'length')
if [ "$ISSUE_COUNT" -lt 4 ]; then
    echo "⚠️  Only $ISSUE_COUNT real issues fetched — using 16-issue synthetic fallback." >&2
    ISSUES="[
        {\"number\":1,\"title\":\"Critical auth bypass in OAuth flow\",\"body\":\"\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"security\"},{\"name\":\"urgent\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH1\",\"state\":\"open\"},
        {\"number\":2,\"title\":\"URGENT: Production memory leak\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"},{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH2\",\"state\":\"open\"},
        {\"number\":3,\"title\":\"URGENT: API rate limiting broken\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"},{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH3\",\"state\":\"open\"},
        {\"number\":4,\"title\":\"URGENT: Database connection pool exhaustion\",\"body\":\"\",\"labels\":[{\"name\":\"priority:critical\"},{\"name\":\"urgent\"}],\"assignees\":[{\"login\":\"alice\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH1\",\"state\":\"open\"},
        {\"number\":5,\"title\":\"Refactor agent orchestration layer\",\"body\":\"\",\"labels\":[{\"name\":\"architecture\"},{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[{\"login\":\"bob\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH2\",\"state\":\"open\"},
        {\"number\":6,\"title\":\"Implement new feature flags system\",\"body\":\"\",\"labels\":[{\"name\":\"epic\"},{\"name\":\"feature\"},{\"name\":\"urgent\"},{\"name\":\"priority:critical\"}],\"assignees\":[{\"login\":\"carol\"}],\"createdAt\":\"$MONTH_AGO\",\"updatedAt\":\"$AH3\",\"state\":\"open\"},
        {\"number\":7,\"title\":\"Fix typo in README\",\"body\":\"\",\"labels\":[{\"name\":\"good-first-issue\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R1\",\"state\":\"open\"},
        {\"number\":8,\"title\":\"Update Spring Boot to 3.5.11\",\"body\":\"\",\"labels\":[{\"name\":\"dependencies\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R2\",\"state\":\"open\"},
        {\"number\":9,\"title\":\"Something unclear\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R3\",\"state\":\"open\"},
        {\"number\":10,\"title\":\"Another vague issue\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R4\",\"state\":\"open\"},
        {\"number\":11,\"title\":\"CI pipeline failing intermittently\",\"body\":\"\",\"labels\":[{\"name\":\"ci\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R5\",\"state\":\"open\"},
        {\"number\":12,\"title\":\"Write API documentation\",\"body\":\"\",\"labels\":[{\"name\":\"documentation\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R6\",\"state\":\"open\"},
        {\"number\":13,\"title\":\"Add dark mode toggle\",\"body\":\"\",\"labels\":[{\"name\":\"enhancement\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R7\",\"state\":\"open\"},
        {\"number\":14,\"title\":\"Fix CORS headers on demo endpoints\",\"body\":\"\",\"labels\":[{\"name\":\"bug\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R8\",\"state\":\"open\"},
        {\"number\":15,\"title\":\"Stale tracking issue from last quarter\",\"body\":\"\",\"labels\":[{\"name\":\"triage\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R9\",\"state\":\"open\"},
        {\"number\":16,\"title\":\"Upgrade Node.js to v22\",\"body\":\"\",\"labels\":[{\"name\":\"dependencies\"}],\"assignees\":[{\"login\":\"roryp\"}],\"createdAt\":\"$WEEK_AGO\",\"updatedAt\":\"$R10\",\"state\":\"open\"}
    ]"
else
    echo "  ↳ Fetched $ISSUE_COUNT real issues from GitHub; applied chaos overlay."
fi

# --- Step 1: Seed chaotic issues ---
SEED_RESULT=$(curl -s -X POST "$BASE_URL/demo/api/seed" \
  -H 'Content-Type: application/json' \
  -d "{\"repo\":\"$REPO\",\"issues\":$ISSUES}")
SEEDED_COUNT=$(echo "$SEED_RESULT" | jq -r '.issueCount // empty')
SEEDED_REPO=$(echo "$SEED_RESULT" | jq -r '.repo // empty')
if [ -n "$SEEDED_COUNT" ]; then
    APPLIED_COUNT=$(echo "$ISSUES" | jq 'length')
    echo "  Seeded $SEEDED_COUNT issues for $SEEDED_REPO (overlayed chaos on $APPLIED_COUNT real titles)"
else
    echo "$SEED_RESULT"
fi

# --- Step 2 (AFTER mode only): Call reshape endpoint ---
if [ "$MODE" = "after" ]; then
    echo ""
    echo "🤖 Step 2/4: Running reshape (deterministic pre-pass + supervisor)..."
    RESHAPE_RESULT=$(curl -s -X POST "$BASE_URL/demo/api/reshape" \
      -H 'Content-Type: application/json' \
      -d "{\"repo\":\"$REPO\",\"userId\":\"roryp\"}")
    BEFORE=$(echo "$RESHAPE_RESULT" | jq -r '.beforeScore // "?"')
    AFTER=$(echo "$RESHAPE_RESULT" | jq -r '.afterScore // "?"')
    LEVEL=$(echo "$RESHAPE_RESULT" | jq -r '.afterLevel // "?"')
    ACTIONS=$(echo "$RESHAPE_RESULT" | jq -r '.actionsApplied // "?"')
    LLM_USED=$(echo "$RESHAPE_RESULT" | jq -r '.llmUsed // "?"')
    if [ "$BEFORE" != "?" ]; then
        echo "  Before: $BEFORE -> After: $AFTER ($LEVEL)"
        echo "  Actions applied: $ACTIONS, LLM used: $LLM_USED"
    else
        echo "$RESHAPE_RESULT"
    fi
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
    SCORE=$(echo "$RESULT" | jq -r '.stressScore // "?"')
    LEVEL=$(echo "$RESULT" | jq -r '.stressLevel // "?"')
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
STUDY_RESULT=$(curl -s -X POST "$BASE_URL/demo/api/study/seed")
STUDY_COUNT=$(echo "$STUDY_RESULT" | jq -r '.seeded // empty')
STUDY_USERS=$(echo "$STUDY_RESULT" | jq -r '.users // [] | join(", ")')
if [ -n "$STUDY_COUNT" ]; then
    echo "  Seeded $STUDY_COUNT snapshots for $STUDY_USERS"
else
    echo "$STUDY_RESULT"
fi

# --- Validate ---
echo ""
echo "📊 Validation for roryp:"
RESULT=$(curl -s -X POST "$BASE_URL/demo/api/checkin" \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":\"roryp\",\"repo\":\"$REPO\",\"selfScore\":50}")
echo "$RESULT" | jq -r '
    "   Stress: \(.stressScore) (\(.stressLevel))",
    "   Workload=\(.breakdown.workload) Chaos=\(.breakdown.chaos) CtxSwitch=\(.breakdown.contextSwitching) Clarity=\(.breakdown.clarity) Sustained=\(.breakdown.sustained) AfterHrs=\(.breakdown.afterHours)"
' 2>/dev/null || echo "$RESULT"

echo ""
echo "✅ Demo data seeded ($MODE)! Open these pages:"
echo "   ${BASE_URL}/                              -> Landing page"
echo "   ${BASE_URL}/checkin.html              -> Stress check-in (use: roryp / roryp/burnout-app)"
echo "   ${BASE_URL}/flamegraph.html?repo=roryp/burnout-app  -> Flamegraph"
echo "   ${BASE_URL}/study.html                -> Study dashboard (click Load Data)"
