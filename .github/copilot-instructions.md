# Copilot Instructions for Burnout-as-a-Service

## Seed Data Rules (CRITICAL)

When seeding issues via `POST /demo/api/seed`:

1. **Use camelCase field names**: `createdAt`, `updatedAt` — NOT `created_at`, `updated_at`.
   The `Issue` Java record uses camelCase. Snake_case silently deserializes as `null`, causing
   Context Switching, After Hours, and Sustained Load to show as 0.

2. **Use current timestamps**: All time-based stress metrics (Context Switching, After Hours,
   Sustained Load) are calculated relative to the server's current time. Old/static dates
   produce zero values. Always generate timestamps relative to "now".

3. **Use the seed script**: Run `bash scripts/seed-demo.sh` (or `.\scripts\seed-demo.ps1` on Windows)
   to seed issues + checkins + study history in one command. Pass the Azure URL as an argument
   for remote deployments.

## Issue Field Reference

```json
{
  "number": 1,
  "title": "Issue title",
  "body": "Description (empty string = mystery meat, hurts Clarity)",
  "labels": [{"name": "priority:critical"}],
  "assignees": [{"login": "username"}],
  "createdAt": "2026-03-09T12:00:00Z",
  "updatedAt": "2026-03-09T12:00:00Z",
  "state": "open"
}
```

**Do NOT include** `created_at`, `updated_at`, or `pull_request` fields — they are ignored or cause nulls.

## Labels That Affect Stress Metrics

- **Deep Work**: `priority:critical`, `priority:high`, `architecture`, `security`, `deep-work`, `epic`, `feature`
- **Quick Win**: `good-first-issue`, `quick-win`, `low-hanging-fruit`, `trivial`
- **Maintenance**: `dependencies`, `documentation`, `triage`, `chore`, `refactor`, `tech-debt`, `ci`, `devops`, `maintenance`
- **Chaos**: `urgent` (especially if unassigned or >24h old)
- **After Hours**: set `updatedAt` to before 9 AM or after 6 PM in the user's timezone (weekends also count)
- **Context Switching**: 6+ issues with `updatedAt` in the last 60 minutes

## Demo Pages

| Page | URL | What to enter |
|------|-----|---------------|
| Home | `/` | Landing page with links to all pages |
| Stress Check-In | `/checkin.html` | Username: `roryp`, Repo: `roryp/burnout-app` |
| Flamegraph | `/flamegraph.html?repo=roryp/burnout-app&userId=roryp` | Auto-loads |
| Study Dashboard | `/study.html` | Click **Load Data**, then click a participant |

## Build & Deploy

```bash
cd backend && mvn clean package -DskipTests   # build
cd .. && azd up                                 # deploy to Azure
bash scripts/seed-demo.sh <azure-url>           # seed demo data
```

## Demo Screenshot Workflow (CRITICAL)

When asked to take screenshots, capture demo screenshots, or run the demo flow:

1. **ALWAYS use the real Azure deployment** — NEVER use dummy endpoints, dummy credentials, or localhost
   unless the user explicitly asks for local. Discover the URL via `azd env get-values` and look for
   `SERVICE_BACKEND_URI`.

2. **Run the full before/after flow:**
   ```powershell
   # Option A: Automated script (captures 8 screenshots via Playwright)
   .\scripts\demo-screenshots.ps1

   # Option B: Manual with Playwright MCP tool in Copilot Chat
   # Step 1: Seed BEFORE state
   .\scripts\seed-demo.ps1 -BaseUrl <azure-url>
   # Step 2: Take BEFORE screenshots (checkin, stress drilldown, flamegraph)
   # Step 3: Sync real GitHub issues for AFTER state
   #   POST /demo/api/sync?repo=roryp/burnout-app
   # Step 4: Take AFTER screenshots (checkin, stress drilldown, flamegraph)
   # Step 5: Take study dashboard screenshot
   ```

3. **Screenshot checklist (8 screenshots):**
   - `landing.png` — Landing page with cards linking to Check-In, Flamegraph, Study Dashboard
   - `checkin-before.png` — Stress 100, CRITICAL, all bars red, issue toggles visible
   - `stress-before.png` — Stress 100, CRITICAL, with Workload issue drilldown expanded
   - `flamegraph-before.png` — 100/100 stress, 0 quick wins, 9 deferred
   - `checkin-after.png` — Stress ~10, LOW, most bars zeroed
   - `stress-after.png` — Stress ~10, LOW, with Workload issue drilldown expanded
   - `flamegraph-after.png` — ~10/100 stress, 90% Friday Score, 3-3-3 structure
   - `study-dashboard.png` — trend chart with roryp's dramatic drop, 5 participant cards, raw snapshots

4. **Using Playwright MCP tool for screenshots:** Navigate to each page, fill in
   `roryp` / `roryp/burnout-app`, click the action button, wait for results, then
   use `browser_take_screenshot` with `fullPage: true` and `type: png`.

5. **Endpoint discovery command:**
   ```powershell
   azd env get-values | Select-String 'SERVICE_BACKEND_URI'
   # Returns: SERVICE_BACKEND_URI="https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io"
   ```

## Key Architecture Rules

- The `Issue` Java record uses **camelCase** (`createdAt`/`updatedAt`)
- The `/demo/api/sync` endpoint maps GitHub's **snake_case** (`created_at`) via `@JsonProperty` on `GitHubIssue`
- The `/demo/api/seed` endpoint deserializes directly into `Issue` — so it needs **camelCase**
- Deterministic services calculate all metrics first; AI agents only explain — they never make decisions
- Every AI agent must have a deterministic fallback when the LLM is unavailable
- **After-hours is timezone-aware**: The checkin page auto-detects the browser timezone and sends it as `tz` in the request body. Working hours are 9 AM–6 PM in the user's timezone; weekends always count as after-hours. All three after-hours implementations (ChaosMetricsService, WorldState, SyntheticTimeResolver) use the same 9 AM–6 PM + weekends rule.

## Testing the App (Comprehensive Guide)

### Prerequisites

| Tool | Purpose | Install |
|------|---------|---------|
| Java 21+ | Backend runtime | `winget install Microsoft.OpenJDK.21` |
| Maven | Backend build | `winget install Apache.Maven` |
| Node.js 18+ | MCP app build | `winget install OpenJS.NodeJS` |
| GitHub CLI | MCP auth + issue sync | `winget install GitHub.cli` then `gh auth login` |
| Azure CLI | Azure deployment | `winget install Microsoft.AzureCLI` |
| Azure Developer CLI | One-command deploy | `winget install Microsoft.Azd` |
| Playwright | Video/screenshot capture | `npm install playwright` (workspace-level) |

### 1. Run Backend Unit/Integration Tests

```bash
cd backend
mvn test
```

This runs `IntegrationTest.java` which tests:
- Health endpoint (`/actuator/health`)
- Chaos endpoint with empty cache
- Issue seeding via `/demo/api/seed`
- Stress breakdown calculation (all 6 metrics non-zero)
- Reshape endpoint with supervisor agent (deterministic fallback)
- Flamegraph API with day plan structure

Tests use `security.enabled=false` and dummy OpenAI credentials — the system falls back to deterministic responses.

### 2. Run Backend Locally

```powershell
cd backend
mvn clean package -DskipTests
java -Dsecurity.enabled=false `
     -Dazure.openai.endpoint=https://dummy.openai.azure.com `
     -Dazure.openai.api-key=dummy-key `
     -jar target/burnout-backend-0.0.1-SNAPSHOT.jar
```

- `security.enabled=false` skips GitHub token validation on `/api/**`
- Dummy OpenAI credentials trigger deterministic fallback (all features work, no LLM prose)
- Server starts on `http://localhost:8080`

### 3. Seed Demo Data (Local or Azure)

```powershell
# Local
.\scripts\seed-demo.ps1

# Azure
.\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io

# AFTER mode (seeds chaotic issues, then runs real reshape agent)
.\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io -Mode after
```

**What it seeds:** 16 chaotic issues (stress → 100), 9 checkin snapshots, 113 study history snapshots for 5 participants.

### 4. Verify All Pages Manually

After seeding, open these URLs (replace base URL for Azure):

| Page | URL | Expected Result |
|------|-----|-----------------|
| Home | `/` | Landing page with links to Check-In, Flamegraph, Study |
| Check-In | `/checkin.html` | Enter `roryp` + `roryp/burnout-app` → Score 100, CRITICAL |
| Flamegraph | `/flamegraph.html?repo=roryp/burnout-app&userId=roryp` | 100/100 stress, 16 issues, 0 quick wins |
| Study | `/study.html` | Click Load Data → 5 participants, trend chart, 113+ snapshots |
| Health | `/actuator/health` | `{"status":"UP"}` |

**CRITICAL:** Always use `&userId=roryp` on the flamegraph URL — without it, stress is calculated across ALL users (higher score). With it, stress filters to roryp's assigned issues only (matches reference screenshots: 10/100).

### 5. Post-Deployment Smoke Test (26 Assertions)

```powershell
.\scripts\smoke-test.ps1 -BaseUrl https://your-app.azurecontainerapps.io
```

Tests: health, seeding, all 6 stress metrics non-zero, breakdown hints/tooltips, flamegraph day plan, study snapshots, all 4 static pages return HTTP 200.

### 6. Test MCP Tools in VS Code

```bash
cd mcp-app && npm install && npm run build
```

Then reload VS Code (the `.vscode/mcp.json` is pre-configured). In Copilot Chat:

```
Sync issues for roryp/burnout-app
What's my stress score for roryp/burnout-app?
Show my burnout wheel for roryp/burnout-app
Reshape my day for roryp/burnout-app
```

Expected: `sync_issues` fetches 32 issues, `get_stress_score` returns 10/LOW, `show_burnout_wheel` shows 3-3-3 flamegraph, `reshape_day` applies labels and returns AI explanation.

### 7. Test the Before/After Demo Flow

This is the **live demo flow** — captures the full 100→10 stress reduction:

```powershell
# Step 1: Seed chaotic state
.\scripts\seed-demo.ps1 -BaseUrl <url>

# Step 2: Verify BEFORE (100/CRITICAL)
# Open /checkin.html → roryp → 100
# Open /flamegraph.html?repo=roryp/burnout-app&userId=roryp → 100/100

# Step 3: Sync real issues (replaces chaotic data with actual repo issues)
# Either via MCP: "Sync issues for roryp/burnout-app"
# Or via API: POST /demo/api/sync?repo=roryp/burnout-app

# Step 4: Verify AFTER (10/LOW)
# Open /checkin.html → roryp → 10
# Open /flamegraph.html?repo=roryp/burnout-app&userId=roryp → 10/100, 90% Friday

# Step 5: Check study dashboard
# Open /study.html → Load Data → see 5 participants with trend chart
```

### 8. Record Demo Video

```powershell
# Seed first, then record (script handles sync + rate limits automatically)
.\scripts\seed-demo.ps1 -BaseUrl <url>
node scripts/record-demo.mjs [base-url]
# Output: docs/images/demo/demo-pipeline.webm (~30s, 2.5MB)
```

The recording script: resets study data, syncs real GitHub issues, seeds chaotic BEFORE state, records 6 scenes with title cards (BEFORE checkin with drilldown → BEFORE flamegraph → "Reshaping Your Day" transition → AFTER checkin with drilldown → AFTER flamegraph → team dashboard).

### 9. Capture Screenshots

```powershell
.\scripts\demo-screenshots.ps1 -BaseUrl <url>
# Output: docs/images/demo/checkin-before.png, stress-before.png, flamegraph-before.png,
#         checkin-after.png, stress-after.png, flamegraph-after.png, study-dashboard.png
```

### 10. Azure Deployment Verification

```powershell
# Deploy
azd up

# Get URL
azd env get-values | Select-String 'SERVICE_BACKEND_URI'

# Seed + smoke test
.\scripts\seed-demo.ps1 -BaseUrl <url>
.\scripts\smoke-test.ps1 -BaseUrl <url>
```

### Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| LLM fallback message ("agents unavailable") | Azure AD token expired (tokens live ~1h, fetched once at startup) | Restart container: `az containerapp revision restart --name <app> --resource-group <rg> --revision <rev>` |
| Flamegraph shows 40 instead of 14 | Missing `&userId=roryp` in URL | Add `&userId=roryp` to flamegraph URL |
| Stress is 0 for all time-based metrics | Seed data uses snake_case (`created_at`) or old timestamps | Use camelCase (`createdAt`) and generate timestamps relative to now |
| Sync returns rate_limited | `/demo/api/sync` allows 1 request per repo per 5 minutes | Wait for `retryAfterSeconds` or use `/demo/api/seed` to inject data directly |
| Study dashboard shows stale data | Old snapshots accumulating from previous runs | Call `DELETE /demo/api/study/reset` then `POST /demo/api/study/seed` |
| MCP tools not appearing in VS Code | MCP app not built or VS Code not reloaded | Run `cd mcp-app && npm run build`, then reload VS Code window |
| 403 on `/api/**` endpoints | CSRF or security filter blocking | Use `/demo/api/**` endpoints (no auth) or send GitHub Bearer token |
| Container cache empty after deploy | In-memory `IssueCache` resets on restart | Re-seed via `seed-demo.ps1` or sync via MCP |

## Agent Profile: Seed → Reshape → Validate (Demo Flow)

**When to use:** User says "seed, reshape, validate with playwright mcp tool" or similar.

**Goal:** Execute the full 100→10 stress reduction demo with live screenshots showing before/after state.

**Prerequisites:**
- Azure deployment running (verify via `azd env get-values | Select-String 'SERVICE_BACKEND_URI'`)
- Playwright MCP tools available in Copilot Chat
- Network access to GitHub API (for sync)

**Step 1: Seed Chaotic BEFORE State**
```powershell
.\scripts\seed-demo.ps1 -BaseUrl <azure-url>
```
- Outputs: 16 chaotic issues (100/CRITICAL), 9 checkins, 113 study snapshots
- Verify output: `Stress: 100 (CRITICAL)`

**Step 2: Validate BEFORE State (Playwright)**

**2a. Check-In Page BEFORE**
- Navigate: `/checkin.html`
- Fill form: username=`roryp`, repo=`roryp/burnout-app`
- Click "Check My Stress" button
- Wait for: `CRITICAL` text appears
- Screenshot: Capture full page (expect **100 STRESS SCORE, CRITICAL, 16 issues, Non-compliant**)

**2b. Flamegraph Page BEFORE**
- Navigate: `/flamegraph.html?repo=roryp/burnout-app&userId=roryp`
- Wait for: `Stress Score` text appears
- Screenshot: Capture full page (expect **100/100 stress, 10% Friday, 0 quick wins, 9 deferred**)

**Step 3: Reshape (Sync + API)**
```powershell
# Sync real GitHub issues (replaces chaotic 16 with real 32)
Invoke-RestMethod -Method POST -Uri "$BASE_URL/demo/api/sync?repo=roryp/burnout-app"

# Reshape the day plan
Invoke-RestMethod -Method POST -Uri "$BASE_URL/demo/api/reshape" `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"repo":"roryp/burnout-app","userId":"roryp"}' | ConvertTo-Json -Depth 5
```
- Expected: `llmUsed: true`, `afterScore: 10`, `afterLevel: LOW`, 3-3-3 structure (1 deep, 3 quick wins, 3 maintenance, 0 deferred)
- **Note:** If rate-limited, wait 186+ seconds, then retry

**Step 4: Validate AFTER State (Playwright)**

**4a. Check-In Page AFTER**
- Navigate: `/checkin.html`
- Fill form: username=`roryp`, repo=`roryp/burnout-app`
- Click "Check My Stress" button
- Wait for: `LOW` text appears
- Screenshot: Capture full page (expect **10 STRESS SCORE, LOW, 32 issues, 3-3-3 Compliant**)

**4b. Flamegraph Page AFTER**
- Navigate: `/flamegraph.html?repo=roryp/burnout-app&userId=roryp`
- Wait for: `Stress Score` text appears
- Screenshot: Capture full page (expect **10/100 stress, 90% Friday, 1-3-3-0 structure**)

**Step 5: Validate Study Dashboard (Playwright)**

- Navigate: `/study.html`
- Click "Load Data" button
- Wait for: `roryp` text appears (participant list loads)
- Screenshot: Capture full page (expect **118 snapshots, 5 participants, trend chart showing roryp 95→10 drop**)

**Final Output: Validation Results Table**

| Step | Page | Metric | Expected | Status |
|------|------|--------|----------|--------|
| BEFORE | Check-In | Stress Score | 100/CRITICAL | ✅ |
| BEFORE | Check-In | Issues | 16, Non-compliant | ✅ |
| BEFORE | Flamegraph | Stress | 100/100 | ✅ |
| BEFORE | Flamegraph | Friday % | 10% | ✅ |
| BEFORE | Flamegraph | Structure | 0-0-3-9 (deferred) | ✅ |
| API | Reshape | LLM Active | `llmUsed: true` | ✅ |
| API | Reshape | New Score | 10/LOW | ✅ |
| AFTER | Check-In | Stress Score | 10/LOW | ✅ |
| AFTER | Check-In | Issues | 32, 3-3-3 Compliant | ✅ |
| AFTER | Flamegraph | Stress | 10/100 | ✅ |
| AFTER | Flamegraph | Friday % | 90% | ✅ |
| AFTER | Flamegraph | Structure | 1-3-3-0 (compliant) | ✅ |
| Study | Dashboard | Snapshots | 118+ | ✅ |
| Study | Dashboard | Participants | 5 (alice, bob, carol, dave, roryp) | ✅ |
| Study | Dashboard | Trend | roryp drop visible | ✅ |

**Troubleshooting This Workflow**

| Issue | Cause | Solution |
|-------|-------|----------|
| Seed fails | Backend not running or wrong URL | Verify `azd env get-values` shows valid `SERVICE_BACKEND_URI` |
| Reshape returns `llmUsed: false` | LLM token expired (~1h after container start) | Run: `az containerapp revision restart --name <app> --resource-group <rg> --revision <rev>` then wait 30s |
| Sync rate-limited | Called within 5 minutes of previous sync | Wait for `retryAfterSeconds` value, then retry |
| Screenshots show blank/loading | Page didn't finish rendering | Increase wait time or add additional `wait_for` call |
| Wrong stress score in flamegraph | Missing `&userId=roryp` parameter | **CRITICAL:** Always include `&userId=roryp` — without it, stress calculated across ALL users |

## Agent Profile: Deploy App -> Verify (Deployment Flow)

**When to use:** User says "deploy the app", "run azd up", "deploy and validate", or similar.

**Goal:** Deploy burnout-app to Azure and validate the release end-to-end with health, seed, and smoke-test checks.

**Prerequisites:**
- Azure CLI + Azure Developer CLI installed
- Authenticated Azure session (`azd auth login`)
- Java/Maven and Node.js available for build validation

**Step 1: Pre-Deploy Build Validation**
```powershell
cd backend
mvn clean package -DskipTests
mvn test
cd ..\mcp-app
npm install
npm run build
cd ..
```
- Expected: backend compiles, tests pass, and MCP app builds without TypeScript errors

**Step 2: Deploy to Azure**
```powershell
azd up
```
- Expected: infrastructure and backend deployment complete successfully

**Step 3: Discover Deployment URL + Health Check**
```powershell
$envLine = azd env get-values | Select-String 'SERVICE_BACKEND_URI'
$baseUrl = ($envLine -split '"')[1]
Invoke-RestMethod -Method GET -Uri "$baseUrl/actuator/health"
```
- Expected: health endpoint returns `{"status":"UP"}`

**Step 4: Seed Demo Data After Deploy**
```powershell
.\scripts\seed-demo.ps1 -BaseUrl $baseUrl
```
- Expected: seed script reports issues/checkins/study snapshots created
- Note: cache is in-memory and resets on every restart/deploy, so re-seeding is required

**Step 5: Run Post-Deployment Smoke Test**
```powershell
.\scripts\smoke-test.ps1 -BaseUrl $baseUrl
```
- Expected: all assertions pass (exit code `0`)

**Final Output: Deployment Validation Table**

| Step | Check | Expected | Status |
|------|-------|----------|--------|
| Build | Backend package | `mvn clean package -DskipTests` succeeds | ✅ |
| Test | Backend tests | `mvn test` passes | ✅ |
| Build | MCP app | `npm run build` succeeds | ✅ |
| Deploy | Azure provision/deploy | `azd up` completes | ✅ |
| Verify | Health endpoint | `{"status":"UP"}` | ✅ |
| Seed | Demo data | Seed completes successfully | ✅ |
| Smoke | Regression checks | All smoke assertions pass | ✅ |

**Troubleshooting This Workflow**

| Issue | Cause | Solution |
|-------|-------|----------|
| `azd up` fails | Not authenticated or wrong subscription | Run `azd auth login`, verify account/subscription, retry |
| No `SERVICE_BACKEND_URI` | Environment not fully provisioned | Re-run `azd up`, then check `azd env get-values` again |
| Health check not `UP` | App still starting or failed startup | Wait, inspect container logs, fix config, retry |
| Smoke test fails | Seed incomplete or endpoint regression | Re-run `seed-demo.ps1`, then rerun `smoke-test.ps1` |
| Stress metrics show zero | Seed used snake_case or stale timestamps | Use camelCase (`createdAt`, `updatedAt`) and current timestamps |
| LLM fallback persists | Azure AD token expired in container | Restart container revision, then retry |
