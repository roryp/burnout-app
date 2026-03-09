# =============================================================================
# Post-Deployment Smoke Test for Burnout-as-a-Service
# =============================================================================
# Verifies that all API endpoints, stress metrics, tooltips, and study features
# are working correctly after deployment.
#
# Usage:
#   .\scripts\smoke-test.ps1                                      # local
#   .\scripts\smoke-test.ps1 -BaseUrl https://your-app.azurecontainerapps.io
#
# Prerequisites: Run seed-demo.ps1 first (or this script seeds automatically).
# Exit code: 0 = all pass, 1 = failures detected
# =============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"
$passed = 0
$failed = 0
$total = 0

function Assert($name, $condition, $detail = "") {
    $script:total++
    if ($condition) {
        $script:passed++
        Write-Host "  PASS  $name" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "  FAIL  $name  $detail" -ForegroundColor Red
    }
}

Write-Host "`n=== Burnout Smoke Test ===" -ForegroundColor Cyan
Write-Host "Target: $BaseUrl`n"

# --- Step 0: Health check ---
Write-Host "--- Health Check ---" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
    Assert "Health endpoint responds" ($health.status -eq "UP") "status=$($health.status)"
} catch {
    Write-Host "  FAIL  Health endpoint unreachable: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nServer is not running. Exiting." -ForegroundColor Red
    exit 1
}

# --- Step 1: Seed data ---
if (-not $SkipSeed) {
    Write-Host "`n--- Seeding Data ---" -ForegroundColor Yellow
    $now = [System.DateTimeOffset]::UtcNow
    $fmt = "yyyy-MM-ddTHH:mm:ssZ"
    $ts = @{
        recent       = $now.AddMinutes(-15).ToString($fmt)
        recent2      = $now.AddMinutes(-30).ToString($fmt)
        recent3      = $now.AddMinutes(-45).ToString($fmt)
        afterHours   = $now.Date.AddHours(3).ToString($fmt)
        yesterday    = $now.AddDays(-1).ToString($fmt)
        twoDaysAgo   = $now.AddDays(-2).ToString($fmt)
        weekAgo      = $now.AddDays(-7).ToString($fmt)
        twoWeeksAgo  = $now.AddDays(-14).ToString($fmt)
        monthAgo     = $now.AddDays(-30).ToString($fmt)
        twoMonthsAgo = $now.AddDays(-60).ToString($fmt)
    }

    $issues = @(
        @{number=1;  title="Critical auth bypass"; body="Security vuln"; labels=@(@{name="priority:critical"},@{name="security"},@{name="bug"}); assignees=@(@{login="smoketest"}); createdAt=$ts.twoDaysAgo; updatedAt=$ts.recent; state="open"}
        @{number=2;  title="Refactor agent layer"; body="Complex split"; labels=@(@{name="architecture"},@{name="deep-work"}); assignees=@(@{login="smoketest"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent2; state="open"}
        @{number=3;  title="Fix typo"; body="Small fix"; labels=@(@{name="quick-win"}); assignees=@(@{login="smoketest"}); createdAt=$ts.yesterday; updatedAt=$ts.recent3; state="open"}
        @{number=4;  title="Update deps"; body="Bump"; labels=@(@{name="dependencies"}); assignees=@(@{login="smoketest"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent; state="open"}
        @{number=5;  title="URGENT: leak"; body=""; labels=@(@{name="urgent"},@{name="priority:high"}); assignees=@(); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.afterHours; state="open"}
        @{number=6;  title="URGENT: rate limit"; body=""; labels=@(@{name="urgent"}); assignees=@(); createdAt=$ts.twoDaysAgo; updatedAt=$ts.afterHours; state="open"}
        @{number=7;  title="URGENT: db pool"; body="Pool exhaustion"; labels=@(@{name="urgent"},@{name="priority:critical"}); assignees=@(@{login="smoketest"}); createdAt=$ts.monthAgo; updatedAt=$ts.recent; state="open"}
        @{number=8;  title="Dark mode"; body="Users want this"; labels=@(@{name="enhancement"}); assignees=@(@{login="smoketest"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent2; state="open"}
        @{number=9;  title="Unclear"; body=""; labels=@(); assignees=@(); createdAt=$ts.twoMonthsAgo; updatedAt=$ts.monthAgo; state="open"}
        @{number=10; title="Vague"; body=""; labels=@(); assignees=@(); createdAt=$ts.monthAgo; updatedAt=$ts.twoWeeksAgo; state="open"}
        @{number=11; title="CI failing"; body="Random fails"; labels=@(@{name="ci"},@{name="devops"}); assignees=@(@{login="smoketest"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent3; state="open"}
        @{number=12; title="Write docs"; body="OpenAPI specs"; labels=@(@{name="documentation"},@{name="tech-debt"}); assignees=@(@{login="smoketest"}); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.weekAgo; state="open"}
        @{number=13; title="Feature flags"; body="Large epic"; labels=@(@{name="epic"},@{name="architecture"}); assignees=@(@{login="smoketest"}); createdAt=$ts.monthAgo; updatedAt=$ts.afterHours; state="open"}
        @{number=14; title="Fix CORS"; body="Quick config"; labels=@(@{name="quick-win"},@{name="bug"}); assignees=@(@{login="smoketest"}); createdAt=$ts.yesterday; updatedAt=$ts.recent; state="open"}
        @{number=15; title="Stale issue"; body=""; labels=@(@{name="triage"}); assignees=@(); createdAt=$ts.twoMonthsAgo; updatedAt=$ts.twoMonthsAgo; state="open"}
        @{number=16; title="Upgrade Node"; body="LTS"; labels=@(@{name="dependencies"}); assignees=@(@{login="smoketest"}); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.recent2; state="open"}
    )

    $seedBody = @{ repo = "smoketest/repo"; issues = $issues } | ConvertTo-Json -Depth 4 -Compress
    $seed = Invoke-RestMethod -Uri "$BaseUrl/demo/api/seed" -Method POST -ContentType "application/json" -Body $seedBody
    Assert "Seed issues" ($seed.issueCount -eq 16) "got $($seed.issueCount)"
}

# --- Step 2: Test checkin endpoint ---
Write-Host "`n--- Checkin API ---" -ForegroundColor Yellow
$checkin = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" `
    -Body '{"userId":"smoketest","repo":"smoketest/repo","selfScore":50}'

Assert "Checkin returns stress score" ($checkin.stressScore -gt 0) "score=$($checkin.stressScore)"
Assert "Checkin returns stress level" ($checkin.stressLevel -ne $null -and $checkin.stressLevel -ne "") "level=$($checkin.stressLevel)"
Assert "Checkin returns total issues" ($checkin.totalIssues -eq 16) "issues=$($checkin.totalIssues)"

# Breakdown metrics (the whole point — these were 0 when using snake_case)
$bd = $checkin.breakdown
Assert "Workload > 0" ($bd.workload -gt 0) "workload=$($bd.workload)"
Assert "Chaos > 0" ($bd.chaos -gt 0) "chaos=$($bd.chaos)"
Assert "Context Switching > 0" ($bd.contextSwitching -gt 0) "contextSwitching=$($bd.contextSwitching)"
Assert "Clarity > 0" ($bd.clarity -gt 0) "clarity=$($bd.clarity)"
Assert "After Hours > 0" ($bd.afterHours -gt 0) "afterHours=$($bd.afterHours)"

# Breakdown hints (tooltip data)
$hints = $checkin.breakdownHints
Assert "Workload hint present" ($hints.workload -ne $null -and $hints.workload.Length -gt 5) "hint=$($hints.workload)"
Assert "Chaos hint present" ($hints.chaos -ne $null -and $hints.chaos.Length -gt 5) "hint=$($hints.chaos)"
Assert "Context hint present" ($hints.contextSwitching -ne $null -and $hints.contextSwitching.Length -gt 5)
Assert "Clarity hint present" ($hints.clarity -ne $null -and $hints.clarity.Length -gt 5)
Assert "After Hours hint present" ($hints.afterHours -ne $null -and $hints.afterHours.Length -gt 5)

# --- Step 3: Test flamegraph endpoint ---
Write-Host "`n--- Flamegraph API ---" -ForegroundColor Yellow
$fg = Invoke-RestMethod -Uri "$BaseUrl/demo/api/flamegraph?repo=smoketest/repo"
Assert "Flamegraph status ok" ($fg.status -eq "ok") "status=$($fg.status)"
Assert "Flamegraph stress score > 0" ($fg.stressScore -gt 0) "score=$($fg.stressScore)"
Assert "Flamegraph has day plan" ($fg.dayPlan -ne $null)
Assert "Flamegraph issue count" ($fg.totalIssues -eq 16) "issues=$($fg.totalIssues)"

# --- Step 4: Test repos endpoint ---
Write-Host "`n--- Repos API ---" -ForegroundColor Yellow
$repos = Invoke-RestMethod -Uri "$BaseUrl/demo/api/repos"
Assert "Repos list contains seeded repo" ($repos -contains "smoketest/repo") "repos=$($repos -join ',')"

# --- Step 5: Test study seed + snapshots ---
Write-Host "`n--- Study API ---" -ForegroundColor Yellow
$studySeed = Invoke-RestMethod -Uri "$BaseUrl/demo/api/study/seed" -Method POST
Assert "Study seed creates snapshots" ($studySeed.seeded -gt 0) "seeded=$($studySeed.seeded)"

$from = (Get-Date).AddDays(-30).ToString("yyyy-MM-dd")
$to = (Get-Date).ToString("yyyy-MM-dd")
$snapshots = Invoke-RestMethod -Uri "$BaseUrl/demo/api/study/snapshots?from=$from&to=$to"
Assert "Study snapshots returned" ($snapshots.Count -gt 0) "count=$($snapshots.Count)"
Assert "Snapshots have breakdown fields" ($snapshots[0].workloadStress -ne $null)

# --- Step 6: Static pages serve ---
Write-Host "`n--- Static Pages ---" -ForegroundColor Yellow
foreach ($page in @("checkin.html", "flamegraph.html", "study.html")) {
    try {
        $resp = Invoke-WebRequest -Uri "$BaseUrl/$page" -UseBasicParsing -TimeoutSec 10
        Assert "$page serves (200)" ($resp.StatusCode -eq 200)
    } catch {
        Assert "$page serves (200)" $false "error=$($_.Exception.Message)"
    }
}

# --- Summary ---
Write-Host "`n=== Results ===" -ForegroundColor Cyan
Write-Host "  Passed: $passed / $total" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Yellow" })
if ($failed -gt 0) {
    Write-Host "  Failed: $failed" -ForegroundColor Red
    exit 1
} else {
    Write-Host "  All tests passed!" -ForegroundColor Green
    exit 0
}
