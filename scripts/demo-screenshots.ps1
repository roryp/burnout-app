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
#   1. Seeds BEFORE (chaotic) data via seed-demo.ps1 (stress=58/HIGH)
#   2. Opens checkin + flamegraph pages and takes BEFORE screenshots
#   3. Calls /demo/api/reshape (deterministic pre-pass + LangChain4j supervisor)
#      to drop stress to ~8/LOW
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
#   docs/images/demo/landing.png
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

# --- Playwright script path (standalone JS file, no temp file needed) ---
$playwrightJs = Join-Path $PSScriptRoot "demo-screenshots.js"

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

# Take BEFORE screenshots, then sync, then AFTER screenshots (separate node runs to avoid timing issues)
Write-Host "`nStep 2: Taking BEFORE screenshots..." -ForegroundColor Yellow
node $playwrightJs $BaseUrl $outPath "before" $Width $Height $WaitMs 2>&1 | ForEach-Object { Write-Host $_ }

$playwrightOk = ($LASTEXITCODE -eq 0)

# --- Step 3: Run reshape (deterministic pre-pass + supervisor) for AFTER state ---
Write-Host "`nStep 3: Running reshape (deterministic pre-pass + supervisor) for AFTER state..." -ForegroundColor Yellow
try {
    $reshapeBody = @{ repo = "roryp/burnout-app"; userId = "roryp" } | ConvertTo-Json -Compress
    $reshapeResp = Invoke-RestMethod -Uri "$BaseUrl/demo/api/reshape" -Method POST -ContentType "application/json" -Body $reshapeBody -TimeoutSec 90
    Write-Host "  Reshape: before=$($reshapeResp.beforeScore) -> after=$($reshapeResp.afterScore) ($($reshapeResp.afterLevel))" -ForegroundColor Green
    Write-Host "  Actions applied: $($reshapeResp.actionsApplied), LLM used: $($reshapeResp.llmUsed)" -ForegroundColor Gray
} catch {
    Write-Host "  WARNING: Reshape failed. AFTER screenshots may show BEFORE data." -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

# --- Step 4: AFTER screenshots ---
if ($playwrightOk) {
    Write-Host "`nStep 4: Taking AFTER screenshots..." -ForegroundColor Yellow
    node $playwrightJs $BaseUrl $outPath "after" $Width $Height $WaitMs 2>&1 | ForEach-Object { Write-Host $_ }

    Write-Host "`nStep 5: Taking study dashboard screenshot..." -ForegroundColor Yellow
    node $playwrightJs $BaseUrl $outPath "study" $Width $Height $WaitMs 2>&1 | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "`n  Playwright failed. Falling back to API-only validation..." -ForegroundColor Yellow
    Write-Host "  To enable screenshots, run: npm install playwright && npx playwright install chromium`n" -ForegroundColor Yellow

    # Validate BEFORE via API
    Write-Host "Validating BEFORE state via API..." -ForegroundColor Yellow
    $beforeCheck = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{userId="roryp";repo="roryp/burnout-app";selfScore=50} | ConvertTo-Json -Compress)
    Write-Host "  BEFORE stress: $($beforeCheck.stressScore) ($($beforeCheck.stressLevel))" -ForegroundColor $(if ($beforeCheck.stressScore -ge 80) { "Red" } else { "Yellow" })

    # Validate AFTER via API
    Write-Host "Validating AFTER state via API..." -ForegroundColor Yellow
    $afterCheck = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{userId="roryp";repo="roryp/burnout-app";selfScore=50} | ConvertTo-Json -Compress)
    Write-Host "  AFTER stress: $($afterCheck.stressScore) ($($afterCheck.stressLevel))" -ForegroundColor $(if ($afterCheck.stressScore -le 40) { "Green" } else { "Yellow" })
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
Write-Host "  $BaseUrl/                                              -> Landing Page"
Write-Host "  $BaseUrl/checkin.html                              -> Stress Check-In"
Write-Host "  $BaseUrl/flamegraph.html?repo=roryp/burnout-app    -> Flamegraph"
Write-Host "  $BaseUrl/study.html                                -> Study Dashboard"
Write-Host ""

# (no temp file cleanup needed — demo-screenshots.js is a version-controlled file)
