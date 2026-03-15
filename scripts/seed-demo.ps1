# =============================================================================
# Demo Seed Script for Burnout-as-a-Service (PowerShell)
# =============================================================================
# Seeds the backend with realistic issue data + study snapshots so all stress
# metrics are populated for live demos.
#
# Usage:
#   .\scripts\seed-demo.ps1                                      # seeds BEFORE (chaotic) state
#   .\scripts\seed-demo.ps1 -Mode after                          # seeds AFTER (reshaped) state
#   .\scripts\seed-demo.ps1 -Mode before                         # explicit BEFORE
#   .\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io
#
# What it does:
#   1. Seeds 16 issues into IssueCache (chaotic BEFORE or reshaped AFTER)
#   2. Runs 3 checkins each for roryp, alice, bob to generate study snapshots
#   3. Seeds 14 days of dummy study data (alice, bob, carol, dave)
#
# BEFORE mode: All issues assigned to roryp, blank bodies, after-hours updates,
#   multiple URGENT unassigned items -> stress score ~100 (CRITICAL)
#
# AFTER mode: Work delegated to alice/bob, clear descriptions, business-hours
#   updates, 3-3-3 structure -> stress score ~31 (MODERATE)
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

if ($Mode -eq "before") {
    # =========================================================================
    # BEFORE: Chaotic state — everything piled on roryp, no descriptions,
    # after-hours activity, unassigned URGENTs, high context switching
    # Target: stress ~100 (CRITICAL)
    # =========================================================================
    Write-Host "📦 Step 1/3: Seeding 16 CHAOTIC issues (BEFORE reshape)..." -ForegroundColor Yellow

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
} else {
    # =========================================================================
    # AFTER: Reshaped state — work delegated, clear descriptions, business
    # hours, 3-3-3 structure (1 deep + 3 quick + 3 maintenance for roryp)
    # Target: stress ~31 (MODERATE)
    # =========================================================================
    Write-Host "📦 Step 1/3: Seeding 16 RESHAPED issues (AFTER reshape)..." -ForegroundColor Yellow

    $biz1      = $now.Date.AddHours(10).ToString($fmt)   # 10 AM UTC
    $biz2      = $now.Date.AddHours(11).ToString($fmt)   # 11 AM UTC
    $biz3      = $now.Date.AddHours(14).ToString($fmt)   # 2 PM UTC
    $biz4      = $now.Date.AddHours(15).ToString($fmt)   # 3 PM UTC
    $biz5      = $now.Date.AddHours(16).ToString($fmt)   # 4 PM UTC
    $weekAgo   = $now.AddDays(-7).ToString($fmt)
    $twoWeeks  = $now.AddDays(-14).ToString($fmt)
    $monthAgo  = $now.AddDays(-30).ToString($fmt)
    $yesterday = $now.AddDays(-1).ToString($fmt)

    $issues = @(
        # === roryp's focused day (7 issues: 1 deep work + 3 quick wins + 3 maintenance) ===
        @{number=1;  title="Critical auth bypass in OAuth flow"; body="Security vulnerability in the OAuth callback handler. Steps to reproduce: 1) Initiate OAuth flow 2) Modify callback URL. Fix: validate redirect_uri against allowlist."; labels=@(@{name="priority:critical"},@{name="security"},@{name="deep-work"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz1; state="open"}
        @{number=7;  title="Fix typo in README"; body="Line 42: 'recieve' should be 'receive'. Simple find-and-replace."; labels=@(@{name="good-first-issue"},@{name="quick-win"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz2; state="open"}
        @{number=14; title="Fix CORS headers on demo endpoints"; body="Add Access-Control-Allow-Origin for azurecontainerapps.io domains. One-line config change in SecurityConfig.java."; labels=@(@{name="quick-win"},@{name="bug"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz3; state="open"}
        @{number=13; title="Add dark mode toggle"; body="Add CSS custom properties for dark/light themes. Toggle button in header. Store preference in localStorage."; labels=@(@{name="quick-win"},@{name="enhancement"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz4; state="open"}
        @{number=8;  title="Update Spring Boot to 3.5.11"; body="Bump spring-boot-starter-parent from 3.5.10 to 3.5.11 in pom.xml. Run full test suite after."; labels=@(@{name="dependencies"},@{name="maintenance"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz5; state="open"}
        @{number=11; title="CI pipeline failing intermittently"; body="Flaky test in IntegrationTest.java line 45. Root cause: race condition in async handler. Fix: add CountDownLatch."; labels=@(@{name="ci"},@{name="devops"},@{name="maintenance"}); assignees=@(@{login="roryp"}); createdAt=$weekAgo; updatedAt=$biz1; state="open"}
        @{number=16; title="Upgrade Node.js to v22"; body="Update Dockerfile and package.json engine field. Test MCP app build with Node 22 LTS."; labels=@(@{name="dependencies"},@{name="maintenance"}); assignees=@(@{login="roryp"}); createdAt=$twoWeeks; updatedAt=$biz2; state="open"}
        # === Delegated to alice (architecture and docs) ===
        @{number=2;  title="Refactor agent orchestration layer"; body="Extract supervisor pattern into separate module. Alice has context from last sprint."; labels=@(@{name="architecture"},@{name="deep-work"}); assignees=@(@{login="alice"}); createdAt=$weekAgo; updatedAt=$biz3; state="open"}
        @{number=3;  title="Implement new feature flags system"; body="Design doc approved. Start with LaunchDarkly SDK integration in backend."; labels=@(@{name="epic"},@{name="feature"}); assignees=@(@{login="alice"}); createdAt=$monthAgo; updatedAt=$biz4; state="open"}
        @{number=12; title="Write API documentation"; body="Generate OpenAPI specs from Spring annotations. Add examples for /api/stress and /api/reshape."; labels=@(@{name="documentation"},@{name="maintenance"}); assignees=@(@{login="alice"}); createdAt=$twoWeeks; updatedAt=$biz5; state="open"}
        # === Delegated to bob (urgent triage + quick fixes) ===
        @{number=4;  title="Investigate production memory leak"; body="Heap dump shows leak in connection pool. Bob to profile with JFR and report findings."; labels=@(@{name="bug"},@{name="priority:high"}); assignees=@(@{login="bob"}); createdAt=$twoWeeks; updatedAt=$biz1; state="open"}
        @{number=5;  title="Review API rate limiting config"; body="Rate limiter config may need tuning. Bob to benchmark current limits and propose changes."; labels=@(@{name="bug"},@{name="maintenance"}); assignees=@(@{login="bob"}); createdAt=$weekAgo; updatedAt=$biz2; state="open"}
        @{number=6;  title="Database connection pool exhaustion"; body="Increase max pool size from 10 to 25. Add connection timeout of 30s. Monitor with Actuator."; labels=@(@{name="priority:high"},@{name="bug"}); assignees=@(@{login="bob"}); createdAt=$monthAgo; updatedAt=$biz3; state="open"}
        # === Deferred / Closed ===
        @{number=9;  title="Investigate unclear bug report"; body="Needs reproduction steps. Deferred until reporter provides more info."; labels=@(@{name="triage"},@{name="bug"}); assignees=@(); createdAt=$monthAgo; updatedAt=$yesterday; state="open"}
        @{number=10; title="Review vague feature request"; body="Waiting on product team clarification. Parked for next sprint planning."; labels=@(@{name="triage"}); assignees=@(); createdAt=$monthAgo; updatedAt=$yesterday; state="open"}
        @{number=15; title="Stale tracking issue from last quarter"; body="Closed as stale. No activity in 60 days. Can reopen if needed."; labels=@(@{name="triage"}); assignees=@(); createdAt=$monthAgo; updatedAt=$yesterday; state="closed"}
    )
}

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
