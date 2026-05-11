---
description: "Deploy burnout-app to Azure with azd, then validate with health, seed, and smoke tests. Re-seeds every deploy (cache is in-memory)."
tools: [vscode/getProjectSetupInfo, vscode/installExtension, vscode/memory, vscode/newWorkspace, vscode/resolveMemoryFileUri, vscode/runCommand, vscode/vscodeAPI, vscode/extensions, vscode/askQuestions, vscode/toolSearch, execute/runNotebookCell, execute/getTerminalOutput, execute/killTerminal, execute/sendToTerminal, execute/createAndRunTask, execute/runInTerminal, execute/runTests, read/getNotebookSummary, read/problems, read/readFile, read/viewImage, read/readNotebookCellOutput, read/terminalSelection, read/terminalLastCommand, agent/runSubagent, edit/createDirectory, edit/createFile, edit/editFiles, search/changes, search/codebase, search/fileSearch, search/listDirectory, search/textSearch, search/usages, web/fetch, browser/openBrowserPage, browser/readPage, browser/screenshotPage, browser/navigatePage, browser/clickElement, browser/typeInPage, browser/handleDialog, burnout-app/get_stress_score, burnout-app/reshape_day, burnout-app/show_burnout_wheel, burnout-app/sync_issues, todo]
---

# Deploy App: Build → `azd up` → Verify → Seed → Smoke Test

Single-pipeline Azure deployment for the burnout-as-a-service platform. Default repo `roryp/burnout-app`, default user `roryp`.

## Trigger phrases

- "deploy the app" / "deploy burnout-app to azure"
- "run azd up" / "provision infra and deploy backend"
- "deploy and validate" / "post-deployment verification"
- "publish latest changes"

## Hard rules

1. **Always re-seed after every deploy.** The `IssueCache` is an in-memory `ConcurrentHashMap` — it is wiped on every container restart / revision swap. A deployed-but-empty backend is the most common false failure.
2. **Discover the URL from `azd env get-values`** — never hardcode or guess. `SERVICE_BACKEND_URI` is the single source of truth.
3. **Health-check before seeding.** A non-`UP` backend means the container is still rolling out or crashed; seeding will give misleading errors.
4. **Run the smoke test last.** It auto-seeds if you forgot, but it expects the deployment to already be reachable.
5. **Report real results.** If a smoke assertion fails, surface it — do not retry silently to make the table green.

## Prerequisites

| Tool | Purpose | Install (Windows) | Install (macOS / Linux) |
|---|---|---|---|
| Java 21+ | Backend build | `winget install Microsoft.OpenJDK.21` | `brew install openjdk@21` / `sudo apt install openjdk-21-jdk` |
| Maven | Backend build | `winget install Apache.Maven` | `brew install maven` / `sudo apt install maven` |
| Node.js 18+ | MCP app build | `winget install OpenJS.NodeJS` | `brew install node` / `sudo apt install nodejs npm` |
| Azure CLI | Azure auth + container ops | `winget install Microsoft.AzureCLI` | `brew install azure-cli` / `curl -sL https://aka.ms/InstallAzureCLIDeb \| sudo bash` |
| Azure Developer CLI | One-command deploy | `winget install Microsoft.Azd` | `brew tap azure/azd && brew install azd` / `curl -fsSL https://aka.ms/install-azd.sh \| bash` |

Authenticate once per session: `azd auth login` (and `az login` if you'll restart container revisions for token refresh).

> **Pick the right shell.** Snippets are shown in **PowerShell** (Windows) and **bash** (macOS / Linux / WSL). Run the one that matches your environment — never mix.

## Pipeline

Run these steps in order. Stop and report on the first non-recoverable failure.

### 1. Pre-deploy build checks

**PowerShell:**

```powershell
Push-Location backend
mvn clean package -DskipTests
mvn test
Pop-Location

Push-Location mcp-app
npm install
npm run build
Pop-Location
```

**bash:**

```bash
(cd backend && mvn clean package -DskipTests && mvn test)
(cd mcp-app && npm install && npm run build)
```

- Backend must compile **and** `IntegrationTest` must pass (uses dummy OpenAI creds + deterministic fallback).
- MCP app must build without TypeScript errors.

### 2. Deploy to Azure

```bash
azd up
```

- Provisions infra from `infra/main.bicep` (Container Apps, ACR, Azure OpenAI, identity) and deploys the backend service.
- On first deploy, `azd` will prompt for environment name, subscription, and region.

### 3. Discover the backend URL

**PowerShell:**

```powershell
$envLine = azd env get-values | Select-String 'SERVICE_BACKEND_URI'
$baseUrl = ($envLine -split '"')[1]
$baseUrl
```

**bash:**

```bash
baseUrl=$(azd env get-values | grep '^SERVICE_BACKEND_URI' | cut -d'"' -f2)
echo "$baseUrl"
```

Expected: `https://<app-name>-backend.<env>.<region>.azurecontainerapps.io`.

### 4. Health check

**PowerShell:**

```powershell
Invoke-RestMethod -Method GET -Uri "$baseUrl/actuator/health"
```

**bash:**

```bash
curl -fsS "$baseUrl/actuator/health"
```

Expected: `{"status":"UP"}`. If `Down` or the call times out, wait ~30s for the revision to become ready and retry once before troubleshooting.

### 5. Seed demo data

**PowerShell:**

```powershell
.\scripts\seed-demo.ps1 -BaseUrl $baseUrl
```

**bash:**

```bash
bash scripts/seed-demo.sh "$baseUrl"
```

This fetches real GitHub issues for `roryp/burnout-app`, applies the chaos overlay, POSTs to `/demo/api/seed`, runs checkins, and seeds 14 days of study history for 5 participants. Expected result for `roryp`: **HIGH** stress.

### 6. Smoke test

**PowerShell:**

```powershell
.\scripts\smoke-test.ps1 -BaseUrl $baseUrl
```

**bash:** the smoke test is PowerShell-only today. On macOS / Linux, install PowerShell (`brew install --cask powershell` or `sudo apt install powershell`) and run:

```bash
pwsh ./scripts/smoke-test.ps1 -BaseUrl "$baseUrl"PowerShell: `Invoke-RestMethod -Method DELETE -Uri "$baseUrl/demo/api/study/reset"` · bash: `curl -fsS -X DELETE "$baseUrl/demo/api/study/reset"` —
```

26 assertions: health, seeding, all 6 stress breakdown metrics non-zero, breakdown hints, flamegraph day plan, study snapshots, all 4 static pages return HTTP 200. Exit code `0` = all pass. The script auto-seeds — pass `-SkipSeed` only if you just ran step 5 and want to save 10 seconds.

## Final summary table

Always report this when finished:

| Step | Check | Expected | Status |
|---|---|---|---|
| Build | `mvn clean package -DskipTests` | succeeds | |
| Test | `mvn test` | passes | |
| Build | `npm run build` (mcp-app) | succeeds | |
| Deploy | `azd up` | completes | |
| Discover | `SERVICE_BACKEND_URI` resolved | non-empty URL | |
| Health | `GET /actuator/health` | `{"status":"UP"}` | |
| Seed | `seed-demo.ps1` | reports HIGH stress for `roryp` | |
| Smoke | `smoke-test.ps1` | exit code `0`, all 26 assertions pass | |

Verification links to include in the summary:

- `$baseUrl/` — landing page
- `$baseUrl/checkin.html` (enter `roryp` / `roryp/burnout-app`) — expect HIGH after seed
- `$baseUrl/flamegraph.html?repo=roryp/burnout-app&userId=roryp` — `&userId=roryp` is **mandatory**
- `$baseUrl/study.html` — click **Load Data** → 5 participants

## Architecture constraints to respect

- **In-memory cache.** `IssueCache` resets on every restart/deploy. Re-seeding is part of the pipeline, not an optional step.
- **camelCase only in seed payloads.** Use `createdAt` / `updatedAt` — snake_case silently deserializes as `null` and zeroes out time-based metrics. The `/demo/api/sync` path maps snake_case via `@JsonProperty` on `GitHubIssue`; `/demo/api/seed` does not.
- **Timestamps must be relative to "now".** The seed script already does this — do not hand-edit static dates into payloads.
- **LLM uses Azure AD tokens fetched once at container startup (~1h lifetime).** A long-running revision will eventually return `llmUsed: false` from `/demo/api/reshape`. The deterministic pre-pass and 1-3-3-0 enforcer still run, but the supervisor is skipped. Restart the revision to refresh the token.
- **Friday score is recomputed in three places via `FridayScoreFormula`** — `/demo/api/flamegraph`, `/demo/api/reshape`, `/api/friday-score`. The smoke test verifies these agree.

## Troubleshooting

| Symptom | Root cause | Fix |
|---|---|---|
| `azd up` fails on auth | Not authenticated or wrong subscription | `azd auth login`; verify subscription with `az account show`; retry |
| `azd up` fails on quota / region | Azure OpenAI capacity exhausted in chosen region | Pick a different region (Sweden Central, East US 2 are common fallbacks) and re-run |
| `SERVICE_BACKEND_URI` is empty | Environment not fully provisioned | Re-run `azd up`, then `azd env get-values` again |
| Health returns non-`UP` | Container still rolling out, or startup crash | Wait 30s and retry; if still failing, `az containerapp logs show --name <app> --resource-group <rg> --tail 200` |
| Smoke test FAILs `breakdown metric non-zero` | Seed used snake_case or stale timestamps | Re-run `seed-demo.ps1`; do not hand-craft payloads |
| Smoke test FAILs `study snapshots` | Old/leftover study state | `Invoke-RestMethod -Method DELETE -Uri "$baseUrl/demo/api/study/reset"`, then re-seed |
| Reshape returns `llmUsed: false` | Azure AD token expired (~1h after container start) | `az containerapp revision restart --name <app> --resource-group <rg> --revision <rev>`; wait 30s; retry |
| Flamegraph shows wrong stress number | Missing `&userId=roryp` URL param | Add it — stress is per-user, not per-repo |
| `/api/**` returns 403 in tests | CSRF / GitHub token validation | The smoke test uses `/demo/api/**` (no auth) — if you're hitting `/api/**` manually, send a GitHub Bearer token or set `security.enabled=false` locally |