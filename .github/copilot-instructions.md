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
- **After Hours**: set `updatedAt` to before 9 AM or after 6 PM UTC
- **Context Switching**: 6+ issues with `updatedAt` in the last 60 minutes

## Demo Pages

| Page | URL | What to enter |
|------|-----|---------------|
| Stress Check-In | `/checkin.html` | Username: `roryp`, Repo: `roryp/burnout-app` |
| Flamegraph | `/flamegraph.html?repo=roryp/burnout-app` | Auto-loads |
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
   # Option A: Automated script (captures 7 screenshots via Playwright)
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

3. **Screenshot checklist (7 screenshots):**
   - `checkin-before.png` — Stress 100, CRITICAL, all bars red, issue toggles visible
   - `stress-before.png` — Stress 100, CRITICAL, with Workload issue drilldown expanded
   - `flamegraph-before.png` — 100/100 stress, 0 quick wins, 9 deferred
   - `checkin-after.png` — Stress ~14, LOW, most bars zeroed
   - `stress-after.png` — Stress ~14, LOW, with Workload issue drilldown expanded
   - `flamegraph-after.png` — ~14/100 stress, 75% Friday Score, 3-3-3 structure
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
| Check-In | `/checkin.html` | Enter `roryp` + `roryp/burnout-app` → Score 100, CRITICAL |
| Flamegraph | `/flamegraph.html?repo=roryp/burnout-app&userId=roryp` | 100/100 stress, 16 issues, 0 quick wins |
| Study | `/study.html` | Click Load Data → 5 participants, trend chart, 113+ snapshots |
| Health | `/actuator/health` | `{"status":"UP"}` |

**CRITICAL:** Always use `&userId=roryp` on the flamegraph URL — without it, stress is calculated across ALL users (higher score). With it, stress filters to roryp's assigned issues only (matches reference screenshots: 14/100).

### 5. Post-Deployment Smoke Test (26 Assertions)

```powershell
.\scripts\smoke-test.ps1 -BaseUrl https://your-app.azurecontainerapps.io
```

Tests: health, seeding, all 6 stress metrics non-zero, breakdown hints/tooltips, flamegraph day plan, study snapshots, all 3 static pages return HTTP 200.

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

Expected: `sync_issues` fetches 32 issues, `get_stress_score` returns 14/LOW, `show_burnout_wheel` shows 3-3-3 flamegraph, `reshape_day` applies labels and returns AI explanation.

### 7. Test the Before/After Demo Flow

This is the **live demo flow** — captures the full 100→14 stress reduction:

```powershell
# Step 1: Seed chaotic state
.\scripts\seed-demo.ps1 -BaseUrl <url>

# Step 2: Verify BEFORE (100/CRITICAL)
# Open /checkin.html → roryp → 100
# Open /flamegraph.html?repo=roryp/burnout-app&userId=roryp → 100/100

# Step 3: Sync real issues (replaces chaotic data with actual repo issues)
# Either via MCP: "Sync issues for roryp/burnout-app"
# Or via API: POST /demo/api/sync?repo=roryp/burnout-app

# Step 4: Verify AFTER (14/LOW)
# Open /checkin.html → roryp → 14
# Open /flamegraph.html?repo=roryp/burnout-app&userId=roryp → 14/100, 75% Friday

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
