# =============================================================================
# Demo Seed Script for Burnout-as-a-Service (PowerShell)
# =============================================================================
# Mirrors scripts/seed-demo.sh — fetches REAL GitHub issues for roryp/burnout-app
# and overlays a chaos pattern that produces a HIGH stress score (matches README).
#
# Usage:
#   .\scripts\seed-demo.ps1                                                    # local, BEFORE
#   .\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io
#   .\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io -Mode after
#
# What it does:
#   1. Fetches up to 30 open issues from github.com/roryp/burnout-app
#   2. Drops PRs, takes first 16
#   3. Overlays chaos:
#        - First 6  -> unassigned + urgent/priority:critical + after-hours updatedAt
#        - Last 10 -> assigned to roryp + last-100-min staggered updatedAt
#        - All bodies blanked (Clarity hit)
#   4. POSTs to /demo/api/seed
#   5. (AFTER mode) Calls /demo/api/reshape
#   6. Runs 3 checkins each for roryp/alice/bob
#   7. Re-seeds chaotic issues so the cache reflects the demo state
#   8. Seeds 14 days of study history
#
# Expected result: roryp -> HIGH stress (typical: 50-70/100).
# After reshape: typically MODERATE (chaos defused, real after-hours signal
# preserved per the acknowledge-don't-erase rule — see AGENTS.md).
# Falls back to a small synthetic set when GitHub is unreachable / rate-limited.
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

# Recent ladder (last 100 minutes) — drives Context Switching
$recent = @()
for ($m = 10; $m -le 100; $m += 10) {
    $recent += $now.AddMinutes(-$m).ToString($fmt)
}

# After-hours rotation — drives After Hours metric
$afterHours = @(
    $now.Date.AddHours(3).ToString($fmt),   # 3 AM UTC
    $now.Date.AddHours(4).ToString($fmt),   # 4 AM UTC
    $now.Date.AddHours(22).ToString($fmt)   # 10 PM UTC
)

$weekAgo  = $now.AddDays(-7).ToString($fmt)
$monthAgo = $now.AddDays(-30).ToString($fmt)

$stepLabel = if ($Mode -eq "after") { "Step 1/4" } else { "Step 1/3" }
Write-Host "📦 ${stepLabel}: Seeding 16 CHAOTIC issues (real titles + chaos overlay)..." -ForegroundColor Yellow

# --- Fetch real issues from GitHub ---
$ghHeaders = @{ "Accept" = "application/vnd.github+json"; "User-Agent" = "burnout-app-seed" }
if ($env:GITHUB_TOKEN) { $ghHeaders["Authorization"] = "Bearer $($env:GITHUB_TOKEN)" }

$rawIssues = $null
try {
    $rawIssues = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$repo/issues?state=open&per_page=30" `
        -Headers $ghHeaders -TimeoutSec 15
} catch {
    Write-Host "  ⚠️  GitHub fetch failed ($($_.Exception.Message)). Using synthetic fallback." -ForegroundColor Yellow
}

# Drop PRs and take first 16
$realIssues = @()
if ($rawIssues) {
    $realIssues = @($rawIssues | Where-Object { -not $_.pull_request } | Select-Object -First 16)
}

if ($realIssues.Count -lt 4) {
    Write-Host "  ⚠️  Only $($realIssues.Count) real issues fetched — using synthetic fallback." -ForegroundColor Yellow
    # Minimal synthetic set that still produces the chaotic shape.
    $realIssues = @(
        [pscustomobject]@{ number = 1; title = "Critical auth bypass in OAuth flow"; labels = @(@{ name = "priority:critical" }, @{ name = "security" }) },
        [pscustomobject]@{ number = 2; title = "URGENT: Production memory leak"; labels = @(@{ name = "bug" }) },
        [pscustomobject]@{ number = 3; title = "URGENT: API rate limiting broken"; labels = @(@{ name = "bug" }) },
        [pscustomobject]@{ number = 4; title = "URGENT: Database connection pool exhaustion"; labels = @(@{ name = "priority:critical" }) },
        [pscustomobject]@{ number = 5; title = "Refactor agent orchestration layer"; labels = @(@{ name = "architecture" }) },
        [pscustomobject]@{ number = 6; title = "Implement new feature flags system"; labels = @(@{ name = "epic" }, @{ name = "feature" }) },
        [pscustomobject]@{ number = 7; title = "Fix typo in README"; labels = @(@{ name = "good-first-issue" }) },
        [pscustomobject]@{ number = 8; title = "Update Spring Boot to 3.5.11"; labels = @(@{ name = "dependencies" }) },
        [pscustomobject]@{ number = 9; title = "Something unclear"; labels = @(@{ name = "bug" }) },
        [pscustomobject]@{ number = 10; title = "Another vague issue"; labels = @(@{ name = "bug" }) },
        [pscustomobject]@{ number = 11; title = "CI pipeline failing intermittently"; labels = @(@{ name = "ci" }) },
        [pscustomobject]@{ number = 12; title = "Write API documentation"; labels = @(@{ name = "documentation" }) },
        [pscustomobject]@{ number = 13; title = "Add dark mode toggle"; labels = @(@{ name = "enhancement" }) },
        [pscustomobject]@{ number = 14; title = "Fix CORS headers on demo endpoints"; labels = @(@{ name = "bug" }) },
        [pscustomobject]@{ number = 15; title = "Stale tracking issue from last quarter"; labels = @(@{ name = "triage" }) },
        [pscustomobject]@{ number = 16; title = "Upgrade Node.js to v22"; labels = @(@{ name = "dependencies" }) }
    )
}

# --- Overlay chaos ---
# First 3  -> unassigned URGENT + after-hours updatedAt (pre-pass triages these)
# Next 3   -> URGENT assigned to alice/bob (survives reshape — teammate fires)
# Last 10  -> assigned to roryp + last-100-min recent updatedAt
#
# NOTE: We use [System.Collections.ArrayList] for labels/assignees because
# PowerShell `if` expressions unwrap single-element arrays to scalars and
# convert empty arrays to $null, which breaks ConvertTo-Json's array output.
$teammates = @("alice", "bob", "carol")
$issues = [System.Collections.ArrayList]::new()
for ($i = 0; $i -lt $realIssues.Count; $i++) {
    $src = $realIssues[$i]
    $isUnassignedUrgent = $i -lt 3
    $isTeammateUrgent   = ($i -ge 3) -and ($i -lt 6)
    $isUrgent           = $isUnassignedUrgent -or $isTeammateUrgent

    $labels = [System.Collections.ArrayList]::new()
    if ($src.labels) {
        foreach ($lbl in $src.labels) {
            if ($lbl.name) { [void]$labels.Add(@{ name = [string]$lbl.name }) }
        }
    }
    if ($isUrgent) {
        [void]$labels.Add(@{ name = "urgent" })
        [void]$labels.Add(@{ name = "priority:critical" })
    }

    $assignees = [System.Collections.ArrayList]::new()
    if ($isTeammateUrgent) {
        [void]$assignees.Add(@{ login = $teammates[($i - 3) % $teammates.Count] })
    } elseif (-not $isUnassignedUrgent) {
        [void]$assignees.Add(@{ login = "roryp" })
    }

    if ($isUrgent) {
        $createdAt = $monthAgo
        $updatedAt = $afterHours[$i % $afterHours.Count]
    } else {
        $createdAt = $weekAgo
        $updatedAt = $recent[($i - 6) % $recent.Count]
    }

    [void]$issues.Add(@{
        number    = [int]$src.number
        title     = [string]$src.title
        body      = ""                      # blank body -> Clarity penalty
        labels    = $labels
        assignees = $assignees
        createdAt = $createdAt
        updatedAt = $updatedAt
        state     = "open"
    })
}

$seedBody = @{ repo = $repo; issues = $issues } | ConvertTo-Json -Depth 5 -Compress

# Seed chaotic issues
$r = Invoke-RestMethod -Uri "$BaseUrl/demo/api/seed" -Method POST -ContentType "application/json" -Body $seedBody
Write-Host "  Seeded $($r.issueCount) issues for $($r.repo) (overlayed chaos on $($realIssues.Count) real titles)" -ForegroundColor Green

# --- Step 2 (AFTER mode only): Call reshape endpoint ---
if ($Mode -eq "after") {
    Write-Host "`n🤖 Step 2/4: Running reshape (deterministic pre-pass + supervisor)..." -ForegroundColor Yellow
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

# Re-seed issues after checkins to ensure curated data is the final cache state
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
$check = Invoke-RestMethod -Uri "$BaseUrl/demo/api/checkin" -Method POST -ContentType "application/json" -Body (@{ userId = "roryp"; repo = $repo; selfScore = 50 } | ConvertTo-Json -Compress)
Write-Host "`n📊 Validation for roryp:" -ForegroundColor Cyan
Write-Host "   Stress: $($check.stressScore) ($($check.stressLevel))"
Write-Host "   Workload=$($check.breakdown.workload) Chaos=$($check.breakdown.chaos) CtxSwitch=$($check.breakdown.contextSwitching) Clarity=$($check.breakdown.clarity) Sustained=$($check.breakdown.sustained) AfterHrs=$($check.breakdown.afterHours)"

Write-Host "`n✅ Demo data seeded ($Mode)! Open these pages:" -ForegroundColor Cyan
Write-Host "   $BaseUrl/                              -> Landing page"
Write-Host "   $BaseUrl/checkin.html              -> Stress check-in (use: roryp / roryp/burnout-app)"
Write-Host "   $BaseUrl/flamegraph.html?repo=roryp/burnout-app  -> Flamegraph"
Write-Host "   $BaseUrl/study.html                -> Study dashboard (click Load Data)"
Write-Host ""
