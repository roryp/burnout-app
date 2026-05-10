#!/bin/bash
# =============================================================================
# Demo Screenshot Script for Burnout-as-a-Service
# =============================================================================
# Captures a full set of before/after screenshots for presentations and docs.
# Uses the REAL Azure deployment (or a local server) — never dummy endpoints.
#
# Usage:
#   bash scripts/demo-screenshots.sh                                                # auto-discovers Azure URL via azd
#   bash scripts/demo-screenshots.sh https://your-app.azurecontainerapps.io         # explicit URL
#   bash scripts/demo-screenshots.sh http://localhost:8080                           # local server
#
# What it does:
#   1. Seeds BEFORE (chaotic) data via seed-demo.sh
#   2. Opens checkin + flamegraph pages and takes BEFORE screenshots
#   3. Seeds AFTER (reshaped) data via seed-demo.sh (calls real reshape endpoint)
#   4. Opens checkin + flamegraph pages and takes AFTER screenshots
#   5. Opens study dashboard, loads data, takes screenshot
#   6. Saves all screenshots to docs/images/demo/
#
# Prerequisites:
#   - npx playwright install chromium (one-time browser install)
#   - Azure deployment running (azd up) OR local backend running
#   - curl, node/npx
#
# Output:
#   docs/images/demo/checkin-before.png
#   docs/images/demo/checkin-after.png
#   docs/images/demo/flamegraph-before.png
#   docs/images/demo/flamegraph-after.png
#   docs/images/demo/study-dashboard.png
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${1:-}"
OUTPUT_DIR="${2:-docs/images/demo}"
WIDTH="${3:-1280}"
HEIGHT="${4:-900}"
WAIT_MS="${5:-3000}"

# --- Auto-discover BaseUrl from azd if not provided ---
if [ -z "$BASE_URL" ]; then
    echo "No URL provided. Attempting to discover from azd..."
    if command -v azd &>/dev/null; then
        BASE_URL=$(azd env get-values 2>/dev/null | grep 'SERVICE_BACKEND_URI=' | sed 's/SERVICE_BACKEND_URI="//' | sed 's/"$//' || true)
    fi

    if [ -z "$BASE_URL" ]; then
        echo "  Could not discover Azure URL. Falling back to http://localhost:8080"
        BASE_URL="http://localhost:8080"
    else
        echo "  Found Azure deployment: $BASE_URL"
    fi
fi

# Remove trailing slash
BASE_URL="${BASE_URL%/}"

echo ""
echo "========================================"
echo " Demo Screenshot Capture"
echo " Target: $BASE_URL"
echo " Output: $OUTPUT_DIR"
echo "========================================"
echo ""

# --- Step 0: Health check ---
echo "Step 0: Health check..."
HEALTH=$(curl -sf "$BASE_URL/actuator/health" 2>/dev/null || true)
if echo "$HEALTH" | grep -q '"UP"'; then
    echo "  Server is UP"
else
    echo "  FAILED: Server not reachable at $BASE_URL"
    echo ""
    echo "  Start the server first:"
    echo "    Local:  cd backend && mvn clean package -DskipTests && java -Dsecurity.enabled=false -jar target/burnout-backend-0.0.1-SNAPSHOT.jar"
    echo "    Azure:  azd up"
    exit 1
fi

# --- Create output directory ---
OUT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)/$OUTPUT_DIR"
mkdir -p "$OUT_PATH"
echo "  Output directory: $OUT_PATH"
echo ""

# --- Step 1: Seed BEFORE data ---
echo "Step 1: Seeding BEFORE (chaotic) data..."
bash "$SCRIPT_DIR/seed-demo.sh" "$BASE_URL" before

# --- Step 2: Take BEFORE screenshots ---
echo ""
echo "Step 2: Taking BEFORE screenshots..."

# Create temporary Playwright script
TEMP_SCRIPT=$(mktemp /tmp/burnout-demo-screenshots.XXXXX.js)
cat > "$TEMP_SCRIPT" << 'PLAYWRIGHT_EOF'
const { chromium } = require('playwright');

(async () => {
    const baseUrl = process.argv[2];
    const outDir = process.argv[3];
    const width = parseInt(process.argv[4]) || 1280;
    const height = parseInt(process.argv[5]) || 900;
    const waitMs = parseInt(process.argv[6]) || 3000;

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width, height } });

    async function screenshot(url, filename, actions) {
        const page = await context.newPage();
        await page.goto(url, { waitUntil: 'networkidle' });
        if (actions) await actions(page);
        await page.waitForTimeout(waitMs);
        const path = outDir + '/' + filename;
        await page.screenshot({ path, fullPage: true, type: 'png' });
        console.log('  Saved: ' + filename);
        await page.close();
    }

    // --- BEFORE: Checkin ---
    console.log('Taking BEFORE checkin screenshot...');
    await screenshot(baseUrl + '/checkin.html', 'checkin-before.png', async (page) => {
        await page.fill('input[placeholder*="octocat"]', 'roryp');
        await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
        await page.click('button:has-text("Check My Stress")');
        await page.waitForSelector('text=CRITICAL', { timeout: 15000 }).catch(() => {
            return page.waitForSelector('[class*="score"]', { timeout: 5000 });
        });
    });

    // --- BEFORE: Flamegraph ---
    console.log('Taking BEFORE flamegraph screenshot...');
    await screenshot(baseUrl + '/flamegraph.html?repo=roryp/burnout-app', 'flamegraph-before.png', async (page) => {
        await page.waitForSelector('text=Deep Work', { timeout: 15000 });
    });

    console.log('__BEFORE_DONE__');
    await browser.close();
})();
PLAYWRIGHT_EOF

TEMP_SCRIPT_AFTER=$(mktemp /tmp/burnout-demo-screenshots-after.XXXXX.js)
cat > "$TEMP_SCRIPT_AFTER" << 'PLAYWRIGHT_AFTER_EOF'
const { chromium } = require('playwright');

(async () => {
    const baseUrl = process.argv[2];
    const outDir = process.argv[3];
    const width = parseInt(process.argv[4]) || 1280;
    const height = parseInt(process.argv[5]) || 900;
    const waitMs = parseInt(process.argv[6]) || 3000;

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width, height } });

    async function screenshot(url, filename, actions) {
        const page = await context.newPage();
        await page.goto(url, { waitUntil: 'networkidle' });
        if (actions) await actions(page);
        await page.waitForTimeout(waitMs);
        const path = outDir + '/' + filename;
        await page.screenshot({ path, fullPage: true, type: 'png' });
        console.log('  Saved: ' + filename);
        await page.close();
    }

    // --- AFTER: Checkin ---
    console.log('Taking AFTER checkin screenshot...');
    await screenshot(baseUrl + '/checkin.html', 'checkin-after.png', async (page) => {
        await page.fill('input[placeholder*="octocat"]', 'roryp');
        await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
        await page.click('button:has-text("Check My Stress")');
        // After reshape, stress is typically MODERATE (chaos defused, real
        // after-hours signal preserved). Fall back to LOW or any score
        // element if MODERATE isn't visible.
        await page.waitForSelector('text=MODERATE', { timeout: 15000 })
            .catch(() => page.waitForSelector('text=LOW', { timeout: 5000 }))
            .catch(() => page.waitForSelector('[class*="score"]', { timeout: 5000 }));
    });

    // --- AFTER: Flamegraph ---
    console.log('Taking AFTER flamegraph screenshot...');
    await screenshot(baseUrl + '/flamegraph.html?repo=roryp/burnout-app', 'flamegraph-after.png', async (page) => {
        await page.waitForSelector('text=Deep Work', { timeout: 15000 });
    });

    // --- Study Dashboard ---
    console.log('Taking study dashboard screenshot...');
    await screenshot(baseUrl + '/study.html', 'study-dashboard.png', async (page) => {
        await page.click('button:has-text("Load Data")');
        await page.waitForSelector('text=Snapshots', { timeout: 15000 });
    });

    await browser.close();
    console.log('__ALL_DONE__');
})();
PLAYWRIGHT_AFTER_EOF

# Try Playwright screenshots
PLAYWRIGHT_OK=false
if node "$TEMP_SCRIPT" "$BASE_URL" "$OUT_PATH" "$WIDTH" "$HEIGHT" "$WAIT_MS" 2>/dev/null; then
    PLAYWRIGHT_OK=true

    # --- Step 3: Seed AFTER data ---
    echo ""
    echo "Step 3: Seeding AFTER (reshaped) data..."
    bash "$SCRIPT_DIR/seed-demo.sh" "$BASE_URL" after

    # --- Step 4: Take AFTER screenshots ---
    echo ""
    echo "Step 4: Taking AFTER screenshots + study dashboard..."
    node "$TEMP_SCRIPT_AFTER" "$BASE_URL" "$OUT_PATH" "$WIDTH" "$HEIGHT" "$WAIT_MS"
fi

if [ "$PLAYWRIGHT_OK" = false ]; then
    echo ""
    echo "  Playwright not available. Falling back to API-only validation..."
    echo "  To enable screenshots, run: npm install playwright && npx playwright install chromium"
    echo "  Or use the Playwright MCP tool in VS Code Copilot Chat instead."
    echo ""

    # --- Fallback: API-only validation (no screenshots) ---
    echo "Step 2 (fallback): Validating BEFORE state via API..."
    BEFORE_CHECK=$(curl -s -X POST "$BASE_URL/demo/api/checkin" \
        -H 'Content-Type: application/json' \
        -d '{"userId":"roryp","repo":"roryp/burnout-app","selfScore":50}')
    python3 -c "
import sys, json
d = json.loads('''$BEFORE_CHECK''')
b = d['breakdown']
print(f\"  BEFORE stress: {d['stressScore']} ({d['stressLevel']})\")
print(f\"  Workload={b['workload']} Chaos={b['chaos']} CtxSwitch={b['contextSwitching']} Clarity={b['clarity']} AfterHrs={b['afterHours']}\")
" 2>/dev/null || echo "  $BEFORE_CHECK"

    BEFORE_FG=$(curl -s "$BASE_URL/demo/api/flamegraph?repo=roryp/burnout-app")
    python3 -c "
import sys, json
d = json.loads('''$BEFORE_FG''')
print(f\"  Flamegraph: stress={d.get('stressScore','?')}/100\")
" 2>/dev/null || echo "  $BEFORE_FG"

    # --- Step 3: Seed AFTER data ---
    echo ""
    echo "Step 3: Seeding AFTER (reshaped) data..."
    bash "$SCRIPT_DIR/seed-demo.sh" "$BASE_URL" after

    # --- Step 4: Validate AFTER ---
    echo ""
    echo "Step 4 (fallback): Validating AFTER state via API..."
    AFTER_CHECK=$(curl -s -X POST "$BASE_URL/demo/api/checkin" \
        -H 'Content-Type: application/json' \
        -d '{"userId":"roryp","repo":"roryp/burnout-app","selfScore":50}')
    python3 -c "
import sys, json
d = json.loads('''$AFTER_CHECK''')
b = d['breakdown']
print(f\"  AFTER stress: {d['stressScore']} ({d['stressLevel']})\")
print(f\"  Workload={b['workload']} Chaos={b['chaos']} CtxSwitch={b['contextSwitching']} Clarity={b['clarity']} AfterHrs={b['afterHours']}\")
" 2>/dev/null || echo "  $AFTER_CHECK"

    AFTER_FG=$(curl -s "$BASE_URL/demo/api/flamegraph?repo=roryp/burnout-app")
    python3 -c "
import sys, json
d = json.loads('''$AFTER_FG''')
print(f\"  Flamegraph: stress={d.get('stressScore','?')}/100\")
" 2>/dev/null || echo "  $AFTER_FG"
fi

# --- Summary ---
echo ""
echo "========================================"
echo " Demo Complete!"
echo "========================================"

PNGS=$(find "$OUT_PATH" -name "*.png" 2>/dev/null)
if [ -n "$PNGS" ]; then
    echo ""
    echo "Screenshots saved:"
    echo "$PNGS" | while read -r f; do echo "  $f"; done
else
    echo ""
    echo "No screenshots captured (Playwright not available)."
    echo "To capture screenshots interactively, use the Playwright MCP tool:"
    echo "  1. Seed data:  bash scripts/seed-demo.sh <url>"
    echo '  2. In Copilot Chat: "Take screenshots of checkin, flamegraph, and study pages"'
fi

echo ""
echo "Live pages:"
echo "  $BASE_URL/                                              → Landing Page"
echo "  $BASE_URL/checkin.html                              → Stress Check-In"
echo "  $BASE_URL/flamegraph.html?repo=roryp/burnout-app    → Flamegraph"
echo "  $BASE_URL/study.html                                → Study Dashboard"
echo ""

# Cleanup temp files
rm -f "$TEMP_SCRIPT" "$TEMP_SCRIPT_AFTER"
