# =============================================================================
# Demo Seed Script for Burnout-as-a-Service (PowerShell)
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   .\scripts\seed-demo.ps1                                      # local
#   .\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io
#
# What it does:
#   1. Seeds 16 issues with current timestamps (camelCase!) into IssueCache
#   2. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   3. Seeds 14 days of dummy study data (alice, bob, carol, dave)
#
# After running, open:
#   - /checkin.html         -> stress check-in (enter roryp + roryp/burnout-app)
#   - /flamegraph.html      -> flamegraph visualization
#   - /study.html           -> researcher dashboard (click Load Data)
# =============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$repo = "roryp/burnout-app"

Write-Host "`n🔥 Seeding demo data on $BaseUrl ...`n" -ForegroundColor Cyan

# --- Generate current timestamps ---
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

# --- Step 1: Seed issues ---
Write-Host "📦 Step 1/3: Seeding 16 issues..." -ForegroundColor Yellow

$issues = @(
    @{number=1;  title="Critical auth bypass in OAuth flow"; body="Security vulnerability in the OAuth callback handler."; labels=@(@{name="priority:critical"},@{name="security"},@{name="bug"}); assignees=@(@{login="roryp"},@{login="alice"}); createdAt=$ts.twoDaysAgo; updatedAt=$ts.recent; state="open"}
    @{number=2;  title="Refactor agent orchestration layer"; body="The AgentOrchestrator has grown too complex."; labels=@(@{name="architecture"},@{name="deep-work"}); assignees=@(@{login="roryp"},@{login="alice"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent2; state="open"}
    @{number=3;  title="Fix typo in README"; body="Small typo fix needed"; labels=@(@{name="good-first-issue"},@{name="quick-win"}); assignees=@(@{login="roryp"},@{login="bob"}); createdAt=$ts.yesterday; updatedAt=$ts.recent3; state="open"}
    @{number=4;  title="Update Spring Boot to 3.5.11"; body="Dependency bump"; labels=@(@{name="dependencies"},@{name="maintenance"}); assignees=@(@{login="roryp"},@{login="bob"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent; state="open"}
    @{number=5;  title="URGENT: Production memory leak"; body=""; labels=@(@{name="urgent"},@{name="bug"},@{name="priority:high"}); assignees=@(@{login="roryp"}); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.afterHours; state="open"}
    @{number=6;  title="URGENT: API rate limiting broken"; body=""; labels=@(@{name="urgent"},@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$ts.twoDaysAgo; updatedAt=$ts.afterHours; state="open"}
    @{number=7;  title="URGENT: Database connection pool exhaustion"; body="Pool runs out under load."; labels=@(@{name="urgent"},@{name="priority:critical"}); assignees=@(@{login="roryp"},@{login="alice"}); createdAt=$ts.monthAgo; updatedAt=$ts.recent; state="open"}
    @{number=8;  title="Add dark mode toggle"; body="Users want a dark mode option."; labels=@(@{name="enhancement"},@{name="size:s"}); assignees=@(@{login="roryp"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent2; state="open"}
    @{number=9;  title="Something unclear"; body=""; labels=@(@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$ts.twoMonthsAgo; updatedAt=$ts.recent; state="open"}
    @{number=10; title="Another vague issue"; body=""; labels=@(@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$ts.monthAgo; updatedAt=$ts.recent2; state="open"}
    @{number=11; title="CI pipeline failing intermittently"; body="GitHub Actions fails randomly on test step."; labels=@(@{name="ci"},@{name="devops"},@{name="chore"}); assignees=@(@{login="roryp"},@{login="bob"}); createdAt=$ts.weekAgo; updatedAt=$ts.recent3; state="open"}
    @{number=12; title="Write API documentation"; body="Need OpenAPI specs for all endpoints."; labels=@(@{name="documentation"},@{name="tech-debt"}); assignees=@(@{login="roryp"},@{login="alice"}); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.recent; state="open"}
    @{number=13; title="Implement feature flags"; body="Large epic spanning multiple services."; labels=@(@{name="epic"},@{name="feature"},@{name="architecture"}); assignees=@(@{login="roryp"}); createdAt=$ts.monthAgo; updatedAt=$ts.afterHours; state="open"}
    @{number=14; title="Fix CORS headers on demo endpoints"; body="Quick config change needed"; labels=@(@{name="quick-win"},@{name="bug"},@{name="size:s"}); assignees=@(@{login="roryp"},@{login="bob"}); createdAt=$ts.yesterday; updatedAt=$ts.recent; state="open"}
    @{number=15; title="Stale tracking issue from last quarter"; body=""; labels=@(@{name="triage"}); assignees=@(@{login="roryp"}); createdAt=$ts.twoMonthsAgo; updatedAt=$ts.recent3; state="open"}
    @{number=16; title="Upgrade Node.js to v22"; body="MCP app should use latest LTS"; labels=@(@{name="dependencies"},@{name="refactor"}); assignees=@(@{login="roryp"}); createdAt=$ts.twoWeeksAgo; updatedAt=$ts.recent2; state="open"}
)

$seedBody = @{ repo = $repo; issues = $issues } | ConvertTo-Json -Depth 4 -Compress

# Seed issues first so checkin uses them (checkin skips GitHub fetch if repo is in cache)
$r = Invoke-RestMethod -Uri "$BaseUrl/demo/api/seed" -Method POST -ContentType "application/json" -Body $seedBody
Write-Host "  Seeded $($r.issueCount) issues for $($r.repo)" -ForegroundColor Green

# --- Step 2: Run checkins ---
Write-Host "`n📊 Step 2/3: Running checkins for roryp, alice, bob..." -ForegroundColor Yellow

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

# --- Step 3: Seed study history ---
Write-Host "`n📈 Step 3/3: Seeding 14 days of study history..." -ForegroundColor Yellow
$sr = Invoke-RestMethod -Uri "$BaseUrl/demo/api/study/seed" -Method POST
Write-Host "  Seeded $($sr.seeded) snapshots for $($sr.users -join ', ')" -ForegroundColor Green

Write-Host "`n✅ Demo data seeded! Open these pages:" -ForegroundColor Cyan
Write-Host "   $BaseUrl/checkin.html              -> Stress check-in (use: roryp / roryp/burnout-app)"
Write-Host "   $BaseUrl/flamegraph.html?repo=roryp/burnout-app  -> Flamegraph"
Write-Host "   $BaseUrl/study.html                -> Study dashboard (click Load Data)"
Write-Host ""
