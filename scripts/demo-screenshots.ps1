# =============================================================================
# Demo Screenshot Script for Burnout-as-a-Service
# =============================================================================
# Captures a full set of before/after screenshots for presentations and docs.
# Uses the REAL Azure deployment (or a local server) — never dummy endpoints.
#
# Usage:
#   .\scripts\demo-screenshots.ps1                                               # auto-discovers Azure URL via azd
#   .\scripts\demo-screenshots.ps1 -BaseUrl https://your-app.azurecontainerapps.io  # explicit URL
#   .\scripts\demo-screenshots.ps1 -BaseUrl http://localhost:8080                   # local server
#
# What it does:
#   1. Seeds BEFORE (chaotic) data via seed-demo.ps1
#   2. Opens checkin + flamegraph pages and takes BEFORE screenshots
#   3. Seeds AFTER (reshaped) data via seed-demo.ps1 -Mode after (calls real reshape endpoint)
#   4. Opens checkin + flamegraph pages and takes AFTER screenshots
#   5. Opens study dashboard, loads data, takes screenshot
#   6. Copies all screenshots to docs/images/demo/
#
# Prerequisites:
#   - npx playwright install chromium (one-time browser install)
#   - Azure deployment running (azd up) OR local backend running
#   - PowerShell 7+
#
# Output:
#   docs/images/demo/checkin-before.png
#   docs/images/demo/checkin-after.png
#   docs/images/demo/flamegraph-before.png
#   docs/images/demo/flamegraph-after.png
#   docs/images/demo/study-dashboard.png
# =============================================================================

param(
    [string]$BaseUrl = "",
    [string]$OutputDir = "docs/images/demo",
    [int]$Width = 1280,
    [int]$Height = 900,
    [int]$WaitMs = 3000
)

$ErrorActionPreference = "Stop"

# --- Auto-discover BaseUrl from azd if not provided ---
if (-not $BaseUrl) {
    Write-Host "No -BaseUrl provided. Attempting to discover from azd..." -ForegroundColor Yellow
    try {
        $azdValues = azd env get-values 2>$null
        $match = $azdValues | Select-String -Pattern 'SERVICE_BACKEND_URI="([^"]+)"'
        if ($match) {
            $BaseUrl = $match.Matches[0].Groups[1].Value
            Write-Host "  Found Azure deployment: $BaseUrl" -ForegroundColor Green
        }
    } catch {}

    if (-not $BaseUrl) {
        Write-Host "  Could not discover Azure URL. Falling back to http://localhost:8080" -ForegroundColor Yellow
        $BaseUrl = "http://localhost:8080"
    }
}

# Remove trailing slash
$BaseUrl = $BaseUrl.TrimEnd('/')

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Demo Screenshot Capture" -ForegroundColor Cyan
Write-Host " Target: $BaseUrl" -ForegroundColor Cyan
Write-Host " Output: $OutputDir" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# --- Step 0: Health check ---
Write-Host "Step 0: Health check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
    if ($health.status -ne "UP") { throw "Status is $($health.status)" }
    Write-Host "  Server is UP" -ForegroundColor Green
} catch {
    Write-Host "  FAILED: Server not reachable at $BaseUrl" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`n  Start the server first:" -ForegroundColor Yellow
    Write-Host "    Local:  cd backend && mvn clean package -DskipTests && java -Dsecurity.enabled=false -Dazure.openai.endpoint=... -jar target/burnout-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray
    Write-Host "    Azure:  azd up" -ForegroundColor Gray
    exit 1
}

# --- Create output directory ---
$outPath = Join-Path $PSScriptRoot ".." $OutputDir
$outPath = [System.IO.Path]::GetFullPath($outPath)
New-Item -ItemType Directory -Path $outPath -Force | Out-Null
Write-Host "  Output directory: $outPath`n" -ForegroundColor Gray

# --- Create the Playwright script ---
$playwrightScript = @"
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

    console.log('BEFORE screenshots done. Signaling for AFTER seed...');
    console.log('__BEFORE_DONE__');

    // Wait for AFTER seed (the PowerShell script will signal us)
    // We just wait a fixed time since we're called after seeding
    await new Promise(r => setTimeout(r, 2000));

    // --- AFTER: Checkin ---
    console.log('Taking AFTER checkin screenshot...');
    await screenshot(baseUrl + '/checkin.html', 'checkin-after.png', async (page) => {
        await page.fill('input[placeholder*="octocat"]', 'roryp');
        await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
        await page.click('button:has-text("Check My Stress")');
        await page.waitForSelector('text=LOW', { timeout: 15000 }).catch(() => {
            return page.waitForSelector('[class*="score"]', { timeout: 5000 });
        });
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
"@

$tempScript = Join-Path $env:TEMP "burnout-demo-screenshots.js"
Set-Content -Path $tempScript -Value $playwrightScript -Encoding UTF8

# --- Step 1: Seed BEFORE data ---
Write-Host "Step 1: Seeding BEFORE (chaotic) data..." -ForegroundColor Yellow
& "$PSScriptRoot\seed-demo.ps1" -BaseUrl $BaseUrl -Mode before

# --- Step 2: Take BEFORE screenshots + seed AFTER + take AFTER screenshots ---
Write-Host "`nStep 2: Taking BEFORE screenshots..." -ForegroundColor Yellow

# Check if playwright is available
$playwrightAvailable = $false
try {
    npx playwright --version 2>$null | Out-Null
    $playwrightAvailable = $true
} catch {}

if (-not $playwrightAvailable) {
    Write-Host "  Playwright not found. Installing..." -ForegroundColor Yellow
    npm install -g playwright 2>$null
    npx playwright install chromium 2>$null
}

# Take BEFORE screenshots
$env:PLAYWRIGHT_BROWSERS_PATH = "0"
$beforeOutput = npx playwright test --reporter=list 2>$null
# Actually, use node directly with the script
node $tempScript $BaseUrl $outPath $Width $Height $WaitMs 2>&1 | ForEach-Object {
    if ($_ -match '__BEFORE_DONE__') {
        # Seed AFTER data
        Write-Host "`nStep 3: Seeding AFTER (reshaped) data..." -ForegroundColor Yellow
        & "$PSScriptRoot\seed-demo.ps1" -BaseUrl $BaseUrl -Mode after
        Write-Host ""
    } else {
        Write-Host $_
    }
}

# If the node script failed (playwright not installed as a module), fall back to simpler approach
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n  Playwright node module not found. Falling back to API-only validation..." -ForegroundColor Yellow
    Write-Host "  To enable screenshots, run: npm install playwright && npx playwright install chromium" -ForegroundColor Yellow
    Write-Host "  Or use the Playwright MCP tool in VS Code Copilot Chat instead.`n" -ForegroundColor Gray

    # --- Fallback: API-only validation (no screenshots) ---
    Write-Host "Step 2 (fallback): Validating BEFORE state via API..." -ForegroundColor Yellow
    $beforeCheck = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{userId="roryp";repo="roryp/burnout-app";selfScore=50} | ConvertTo-Json -Compress)
    Write-Host "  BEFORE stress: $($beforeCheck.stressScore) ($($beforeCheck.stressLevel))" -ForegroundColor $(if ($beforeCheck.stressScore -ge 80) { "Red" } else { "Yellow" })
    Write-Host "  Workload=$($beforeCheck.breakdown.workload) Chaos=$($beforeCheck.breakdown.chaos) CtxSwitch=$($beforeCheck.breakdown.contextSwitching) Clarity=$($beforeCheck.breakdown.clarity) AfterHrs=$($beforeCheck.breakdown.afterHours)"

    $beforeFg = Invoke-RestMethod -Uri "$BaseUrl/demo/api/flamegraph?repo=roryp/burnout-app"
    $qw = ($beforeFg.plan | Where-Object { $_.category -eq 'QUICK_WIN' }).issues.Count
    $df = ($beforeFg.plan | Where-Object { $_.category -eq 'DEFERRED' }).issues.Count
    Write-Host "  Flamegraph: stress=$($beforeFg.stressScore)/100, quickWins=$qw, deferred=$df"

    Write-Host "`nStep 3: Seeding AFTER (reshaped) data..." -ForegroundColor Yellow
    & "$PSScriptRoot\seed-demo.ps1" -BaseUrl $BaseUrl -Mode after

    Write-Host "`nStep 4 (fallback): Validating AFTER state via API..." -ForegroundColor Yellow
    $afterCheck = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{userId="roryp";repo="roryp/burnout-app";selfScore=50} | ConvertTo-Json -Compress)
    Write-Host "  AFTER stress: $($afterCheck.stressScore) ($($afterCheck.stressLevel))" -ForegroundColor $(if ($afterCheck.stressScore -le 40) { "Green" } else { "Yellow" })
    Write-Host "  Workload=$($afterCheck.breakdown.workload) Chaos=$($afterCheck.breakdown.chaos) CtxSwitch=$($afterCheck.breakdown.contextSwitching) Clarity=$($afterCheck.breakdown.clarity) AfterHrs=$($afterCheck.breakdown.afterHours)"

    $afterFg = Invoke-RestMethod -Uri "$BaseUrl/demo/api/flamegraph?repo=roryp/burnout-app"
    $qw = ($afterFg.plan | Where-Object { $_.category -eq 'QUICK_WIN' }).issues.Count
    $dw = ($afterFg.plan | Where-Object { $_.category -eq 'DEEP_WORK' }).issues.Count
    $mt = ($afterFg.plan | Where-Object { $_.category -eq 'MAINTENANCE' }).issues.Count
    Write-Host "  Flamegraph: stress=$($afterFg.stressScore)/100, deepWork=$dw, quickWins=$qw, maintenance=$mt (3-3-3 structure)"
}

# --- Summary ---
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Demo Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$pngs = Get-ChildItem -Path $outPath -Filter "*.png" -ErrorAction SilentlyContinue
if ($pngs) {
    Write-Host "`nScreenshots saved:" -ForegroundColor Green
    $pngs | ForEach-Object { Write-Host "  $($_.FullName)" -ForegroundColor Gray }
} else {
    Write-Host "`nNo screenshots captured (Playwright not available)." -ForegroundColor Yellow
    Write-Host "To capture screenshots interactively, use the Playwright MCP tool:" -ForegroundColor Yellow
    Write-Host '  1. Seed data:  .\scripts\seed-demo.ps1 -BaseUrl <url>' -ForegroundColor Gray
    Write-Host '  2. In Copilot Chat: "Take screenshots of checkin, flamegraph, and study pages"' -ForegroundColor Gray
}

Write-Host "`nLive pages:" -ForegroundColor Cyan
Write-Host "  $BaseUrl/checkin.html                              -> Stress Check-In"
Write-Host "  $BaseUrl/flamegraph.html?repo=roryp/burnout-app    -> Flamegraph"
Write-Host "  $BaseUrl/study.html                                -> Study Dashboard"
Write-Host ""

# Cleanup temp file
Remove-Item -Path $tempScript -ErrorAction SilentlyContinue
