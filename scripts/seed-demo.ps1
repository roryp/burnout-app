# =============================================================================
# Demo Seed Script for Burnout-as-a-Service (PowerShell)
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   .\scripts\seed-demo.ps1                                      # seeds BEFORE (chaotic) state
#   .\scripts\seed-demo.ps1 -Mode after                          # seeds BEFORE then runs reshape
#   .\scripts\seed-demo.ps1 -Mode before                         # explicit BEFORE
#   .\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io
#
# What it does:
#   1. Seeds 16 chaotic issues into IssueCache
#   2. (AFTER mode) Calls /demo/api/reshape to run the real supervisor agent
#   3. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   4. Seeds 14 days of dummy study data (alice, bob, carol, dave, roryp)
#
# BEFORE mode: All issues assigned to roryp, blank bodies, after-hours updates,
#   multiple URGENT unassigned items -> stress score ~100 (CRITICAL)
#
# AFTER mode: Seeds the same chaotic issues, then calls the reshape endpoint
#   which runs the real supervisor agent (LLM or deterministic fallback) to
#   reorganize the workload. No hardcoded AFTER state.
#
# After running, open:
#   - /checkin.html         -> stress check-in (enter roryp + roryp/burnout-app)
#   - /flamegraph.html      -> flamegraph visualization
#   - /study.html           -> researcher dashboard (click Load Data)
# =============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateSet("before", "after")]
    [string]$Mode = "before"
)

$ErrorActionPreference = "Stop"
$repo = "roryp/burnout-app"

Write-Host "`n🔥 Seeding demo data ($Mode) on $BaseUrl ...`n" -ForegroundColor Cyan

# --- Generate current timestamps ---
$now = [System.DateTimeOffset]::UtcNow
$fmt = "yyyy-MM-ddTHH:mm:ssZ"

# =========================================================================
# Chaotic issues — always seeded first (both BEFORE and AFTER modes).
# Everything piled on roryp, no descriptions, after-hours activity,
# unassigned URGENTs, high context switching.
# =========================================================================
$stepLabel = if ($Mode -eq "after") { "Step 1/4" } else { "Step 1/3" }
Write-Host "📦 ${stepLabel}: Seeding 16 CHAOTIC issues..." -ForegroundColor Yellow

$recent1  = $now.AddMinutes(-10).ToString($fmt)
$recent2  = $now.AddMinutes(-20).ToString($fmt)
$recent3  = $now.AddMinutes(-30).ToString($fmt)
$recent4  = $now.AddMinutes(-40).ToString($fmt)
$recent5  = $now.AddMinutes(-50).ToString($fmt)
$recent6  = $now.AddMinutes(-60).ToString($fmt)
$recent7  = $now.AddMinutes(-70).ToString($fmt)
$recent8  = $now.AddMinutes(-80).ToString($fmt)
$recent9  = $now.AddMinutes(-90).ToString($fmt)
$recent10 = $now.AddMinutes(-100).ToString($fmt)
$afterH1  = $now.Date.AddHours(3).ToString($fmt)   # 3 AM UTC
$afterH2  = $now.Date.AddHours(4).ToString($fmt)   # 4 AM UTC
$afterH3  = $now.Date.AddHours(22).ToString($fmt)  # 10 PM UTC
$weekAgo  = $now.AddDays(-7).ToString($fmt)
$twoWeeks = $now.AddDays(-14).ToString($fmt)
$monthAgo = $now.AddDays(-30).ToString($fmt)

$issues = @(
    @{number=1;  title="Critical auth bypass in OAuth flow"; body=""; labels=@(@{name="priority:critical"},@{name="security"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent1; state="open"}
    @{number=2;  title="Refactor agent orchestration layer"; body=""; labels=@(@{name="architecture"},@{name="deep-work"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent2; state="open"}
    @{number=3;  title="Implement new feature flags system"; body=""; labels=@(@{name="epic"},@{name="feature"},@{name="priority:critical"}); assignees=@(@{login="roryp"}); createdAt=$monthAgo; updatedAt=$recent3; state="open"}
    @{number=4;  title="URGENT: Production memory leak"; body=""; labels=@(@{name="urgent"},@{name="bug"}); assignees=@(); createdAt=$twoWeeks; updatedAt=$afterH1; state="open"}
    @{number=5;  title="URGENT: API rate limiting broken"; body=""; labels=@(@{name="urgent"},@{name="bug"}); assignees=@(); createdAt=$weekAgo; updatedAt=$afterH2; state="open"}
    @{number=6;  title="URGENT: Database connection pool exhaustion"; body=""; labels=@(@{name="urgent"},@{name="priority:critical"}); assignees=@(); createdAt=$monthAgo; updatedAt=$afterH3; state="open"}
    @{number=7;  title="Fix typo in README"; body=""; labels=@(@{name="good-first-issue"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent4; state="open"}
    @{number=8;  title="Update Spring Boot to 3.5.11"; body=""; labels=@(@{name="dependencies"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent5; state="open"}
    @{number=9;  title="Something unclear"; body=""; labels=@(@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$monthAgo; updatedAt=$recent6; state="open"}
    @{number=10; title="Another vague issue"; body=""; labels=@(@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$monthAgo; updatedAt=$recent7; state="open"}
    @{number=11; title="CI pipeline failing intermittently"; body=""; labels=@(@{name="ci"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent8; state="open"}
    @{number=12; title="Write API documentation"; body=""; labels=@(@{name="documentation"}); assignees=@(@{login="roryp"}); createdAt=$twoWeeks; updatedAt=$recent9; state="open"}
    @{number=13; title="Add dark mode toggle"; body=""; labels=@(@{name="enhancement"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$recent10; state="open"}
    @{number=14; title="Fix CORS headers on demo endpoints"; body=""; labels=@(@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$afterH1; state="open"}
    @{number=15; title="Stale tracking issue from last quarter"; body=""; labels=@(@{name="triage"}); assignees=@(@{login="roryp"}); createdAt=$monthAgo; updatedAt=$afterH2; state="open"}
    @{number=16; title="Upgrade Node.js to v22"; body=""; labels=@(@{name="dependencies"}); assignees=@(@{login="roryp"}); createdAt=$twoWeeks; updatedAt=$afterH3; state="open"}
)

$seedBody = @{ repo = $repo; issues = $issues } | ConvertTo-Json -Depth 4 -Compress

# Seed chaotic issues
$r = Invoke-RestMethod -Uri "$BaseUrl/demo/api/seed" -Method POST -ContentType "application/json" -Body $seedBody
Write-Host "  Seeded $($r.issueCount) issues for $($r.repo)" -ForegroundColor Green

# --- Step 2 (AFTER mode only): Call reshape endpoint ---
if ($Mode -eq "after") {
    Write-Host "`n🤖 Step 2/4: Running reshape (supervisor agent)..." -ForegroundColor Yellow
    $reshapeBody = @{ repo = $repo; userId = "roryp" } | ConvertTo-Json -Compress
    $rr = Invoke-RestMethod -Uri "$BaseUrl/demo/api/reshape" -Method POST -ContentType "application/json" -Body $reshapeBody
    Write-Host "  Before: $($rr.beforeScore) -> After: $($rr.afterScore) ($($rr.afterLevel))" -ForegroundColor Green
    Write-Host "  Actions applied: $($rr.actionsApplied), LLM used: $($rr.llmUsed)" -ForegroundColor Gray
}

# --- Checkins ---
$checkinStep = if ($Mode -eq "after") { "Step 3/4" } else { "Step 2/3" }
Write-Host "`n📊 ${checkinStep}: Running checkins for roryp, alice, bob..." -ForegroundColor Yellow

foreach ($user in @("roryp", "alice", "bob")) {
    for ($i = 1; $i -le 3; $i++) {
        $selfScore = Get-Random -Minimum 20 -Maximum 80
        $checkinBody = @{ userId = $user; repo = $repo; selfScore = $selfScore } | ConvertTo-Json -Compress
        $cr = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body $checkinBody
        Write-Host "  $user checkin ${i}: score=$($cr.stressScore) level=$($cr.stressLevel)" -ForegroundColor Gray
    }
}

# Re-seed issues after checkins to ensure the curated data is the final cache state
# (checkin may have re-fetched from GitHub, overwriting the seed)
$null = Invoke-RestMethod -Uri "$BaseUrl/demo/api/seed" -Method POST -ContentType "application/json" -Body $seedBody
if ($Mode -eq "after") {
    # Re-run reshape to restore the reshaped state
    $reshapeBody = @{ repo = $repo; userId = "roryp" } | ConvertTo-Json -Compress
    $null = Invoke-RestMethod -Uri "$BaseUrl/demo/api/reshape" -Method POST -ContentType "application/json" -Body $reshapeBody
}

# --- Seed study history ---
$studyStep = if ($Mode -eq "after") { "Step 4/4" } else { "Step 3/3" }
Write-Host "`n📈 ${studyStep}: Seeding 14 days of study history..." -ForegroundColor Yellow
$sr = Invoke-RestMethod -Uri "$BaseUrl/demo/api/study/seed" -Method POST
Write-Host "  Seeded $($sr.seeded) snapshots for $($sr.users -join ', ')" -ForegroundColor Green

# --- Validate ---
$check = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{userId="roryp";repo=$repo;selfScore=50} | ConvertTo-Json -Compress)
Write-Host "`n📊 Validation for roryp:" -ForegroundColor Cyan
Write-Host "   Stress: $($check.stressScore) ($($check.stressLevel))"
Write-Host "   Workload=$($check.breakdown.workload) Chaos=$($check.breakdown.chaos) CtxSwitch=$($check.breakdown.contextSwitching) Clarity=$($check.breakdown.clarity) Sustained=$($check.breakdown.sustained) AfterHrs=$($check.breakdown.afterHours)"

Write-Host "`n✅ Demo data seeded ($Mode)! Open these pages:" -ForegroundColor Cyan
Write-Host "   $BaseUrl/checkin.html              -> Stress check-in (use: roryp / roryp/burnout-app)"
Write-Host "   $BaseUrl/flamegraph.html?repo=roryp/burnout-app  -> Flamegraph"
Write-Host "   $BaseUrl/study.html                -> Study dashboard (click Load Data)"
Write-Host ""
