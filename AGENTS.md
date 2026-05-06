# Burnout-as-a-Service: Agent Architecture

This document describes the multi-agent architecture powering the burnout prevention system. The system uses **LangChain4j's Supervisor Pattern** (`langchain4j-agentic`) with Azure OpenAI to orchestrate specialized AI agents that analyze, classify, and rebalance developer workloads.

---

## Setup commands

### Backend (Java 21 + Maven)

```bash
cd backend
mvn clean package -DskipTests
java -jar target/burnout-backend-0.0.1-SNAPSHOT.jar
```

### MCP App (Node.js 18+)

```bash
cd mcp-app
npm install
npm run build
```

### Environment variables

Create a `.env` file in the project root:

```env
# For Azure deployment (after `azd up`):
BACKEND_URL=https://your-backend.wonderfulstone-xxxxx.swedencentral.azurecontainerapps.io

# For local development:
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_DEPLOYMENT=gpt-4o-mini
BACKEND_URL=http://localhost:8080
```

### Running the backend locally

The backend requires Azure OpenAI credentials and has security enabled by default. For local development without an Azure OpenAI resource, use dummy values — the system will use deterministic fallback responses instead of LLM calls:

```bash
cd backend
mvn clean package -DskipTests
java -Dsecurity.enabled=false \
     -Dazure.openai.endpoint=https://dummy.openai.azure.com \
     -Dazure.openai.api-key=dummy-key \
     -jar target/burnout-backend-0.0.1-SNAPSHOT.jar
```

The `security.enabled=false` flag skips GitHub token validation on `/api/**` endpoints. The `AZURE_OPENAI_DEPLOYMENT` defaults to `gpt-4o` (see `application.yml`).

### Azure deployment (one command)

```bash
azd auth login
azd up
```

---

## Dev environment tips

- The backend is a Spring Boot app in `backend/`. Configuration is in `backend/src/main/resources/application.yml` and `application-demo.yml`.
- The MCP app is a TypeScript project in `mcp-app/`. Use `npm run watch` for incremental TypeScript compilation during development.
- The `.vscode/mcp.json` is pre-configured. Restart the MCP server in VS Code after building to pick up changes.
- GitHub CLI (`gh`) must be installed and authenticated (`gh auth login`) — the MCP app uses `gh auth token` for GitHub API access.
- GitHub labels are set up with `scripts/setup-labels.sh` and seed issues with `scripts/seed-issues.sh`.
- Infrastructure is defined in `infra/` using Bicep templates. `azure.yaml` configures Azure Developer CLI.
- The `IssueCache` is an in-memory `ConcurrentHashMap`. Data is lost on restart — re-sync via MCP or re-seed via `/demo/api/seed`.
- The MCP app config is in `mcp-app/src/config.ts`. It reads `BACKEND_URL` from `.env` (defaults to `http://localhost:8080`).
- The MCP app should **not** be modified when adding demo/web features — it is the VS Code Copilot Chat integration layer only.

---

## Testing instructions

- Run backend tests: `cd backend && mvn test`
- Integration test: `backend/src/test/java/com/demo/burnout/IntegrationTest.java`
- MCP app has no test suite currently — validate by building (`npm run build`) and confirming no TypeScript errors.
- After making changes, always run `mvn clean package -DskipTests` to verify the backend compiles, then `mvn test` to run the test suite.
- To test the full flow locally: start the backend, build the MCP app, reload VS Code, then use the MCP tools in Copilot Chat.

### Post-deployment smoke test

After `azd up`, run the smoke test to verify all endpoints, metrics, and pages work:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl https://your-app.azurecontainerapps.io
```

This seeds fresh data, then tests 26 assertions covering:
- Health endpoint, issue seeding, all 6 stress breakdown metrics are non-zero
- Breakdown hints (tooltip data) present for all categories
- Flamegraph API returns day plan with correct issue count
- Study snapshots are persisted and queryable
- All 3 static pages serve HTTP 200

### Quick demo setup (one command)

The `scripts/seed-demo.sh` (bash) and `scripts/seed-demo.ps1` (PowerShell) scripts seed everything needed for a live demo — issues, checkin snapshots, and 14 days of study history for 5 participants (alice, bob, carol, dave, roryp). All timestamps are generated relative to "now" so every metric lights up.

```bash
# Local
bash scripts/seed-demo.sh

# Azure (after azd up)
bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io
```

```powershell
# Local
.\scripts\seed-demo.ps1

# Azure (after azd up)
.\scripts\seed-demo.ps1 -BaseUrl https://your-app.azurecontainerapps.io
```

After seeding, open:
- `/` → landing page with links to all pages
- `/checkin.html` → enter `roryp` + `roryp/burnout-app` to see the full stress breakdown with hover tooltips
- `/flamegraph.html?repo=roryp/burnout-app` → flamegraph visualization
- `/study.html` → researcher dashboard (click **Load Data**, then click any participant to drill into their stress details)

### Testing the demo flamegraph locally

1. Start the backend (see "Running the backend locally" above)
2. Seed test data — POST to `/demo/api/seed` (no auth needed).
   **CRITICAL: Use camelCase field names** (`createdAt`, `updatedAt`) — NOT snake_case (`created_at`, `updated_at`). The `Issue` Java record uses camelCase. Snake_case fields will deserialize as `null`, causing all time-based metrics (Context Switching, After Hours, Sustained Load) to show as 0.
   **CRITICAL: Use recent timestamps** — metrics like Context Switching and After Hours are calculated relative to the current time. Old/static dates will produce zero values. Always generate timestamps relative to "now".
   ```bash
   # Generate current timestamps
   NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
   RECENT=$(date -u -d '-15 minutes' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-15M +%Y-%m-%dT%H:%M:%SZ)
   AFTER_HOURS=$(date -u -d 'today 03:00' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u +%Y-%m-%dT03:00:00Z)
   WEEK_AGO=$(date -u -d '-7 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-7d +%Y-%m-%dT%H:%M:%SZ)

   curl -X POST http://localhost:8080/demo/api/seed \
     -H 'Content-Type: application/json' \
     -d '{"repo":"owner/repo","issues":[
       {"number":1,"title":"Critical bug","body":"Security issue","labels":[{"name":"priority:critical"},{"name":"bug"}],"assignees":[{"login":"user"}],"createdAt":"'$WEEK_AGO'","updatedAt":"'$RECENT'","state":"open"},
       {"number":2,"title":"URGENT: memory leak","body":"","labels":[{"name":"urgent"}],"assignees":[],"createdAt":"'$WEEK_AGO'","updatedAt":"'$AFTER_HOURS'","state":"open"}
     ]}'
   ```
3. Open `http://localhost:8080/flamegraph.html?repo=owner/repo` in a browser
4. Or check APIs directly: `GET /demo/api/repos` and `GET /demo/api/flamegraph?repo=owner/repo`

### Testing on Azure Container Apps

After `azd up`, the in-memory `IssueCache` is empty. To populate everything for a demo:
1. Run `bash scripts/seed-demo.sh https://your-app.azurecontainerapps.io` (or `.\scripts\seed-demo.ps1 -BaseUrl ...`)
2. Or seed manually via `POST /demo/api/seed` (see field reference below)
3. Or sync real issues via the MCP `sync_issues` tool in VS Code (authenticates with GitHub)
4. Verify: `GET /demo/api/repos` should list synced repos, `GET /actuator/health` should return UP

---

## Code style and conventions

- **Backend**: Java 21, Spring Boot 3, LangChain4j. Use `@Agent` annotations for sub-agent interfaces. Use `@Tool` annotations for mutation methods.
- **MCP App**: TypeScript strict mode, ES modules (`"type": "module"` in package.json). Dependencies: `@modelcontextprotocol/sdk`, `zod`, `dotenv`.
- **Configuration**: Use Spring `@Configuration` and `@Bean` annotations. Azure OpenAI config is in `AgentConfiguration.java`.
- **Key design principle**: Deterministic services calculate all metrics first. AI agents **only explain and support** — they never make decisions.
- **Graceful degradation**: Every agent must have a fallback path when the LLM is unavailable. If LLM fails, return deterministic responses.

---

## Architecture overview

The system has two main components:

1. **MCP App** (Node.js) — Exposes 4 tools to VS Code Copilot Chat via stdio transport. Calls the backend over HTTP with a GitHub Bearer token.
2. **Java Backend** (Spring Boot + LangChain4j) — Runs the AI agent orchestration and stress analysis.

### Agent hierarchy

- **AgentOrchestrator** — Central coordinator that dispatches to:
  - **BurnoutSupervisorService** — Three-phase reshape:
    1. **Deterministic pre-pass** (no LLM, ALWAYS runs even when LLM is unavailable): `mutationTool.triageUrgent(n)` is called for every unassigned-urgent issue, then `mutationTool.defuseChaosInputs(clock)` rewrites empty bodies and after-hours / recently-touched timestamps. This guarantees the chaos score drops regardless of which agents the LLM picks (or even when no LLM runs at all). The pre-pass counts are returned on `SupervisorResult` as `deterministicTriageCount` and `deterministicDefuseCount`, and the explanation text is prepended with a "🧹 Deterministic pre-pass:" line listing the triaged issue numbers.
    2. **LangChain4j Supervisor Pattern** with 6 sub-agents (TriageAgent, DeferAgent, DelegateAgent, ClassifyAgent, ScopeAgent, WellnessAgent) capped at `maxAgentsInvocations: 15`, `SupervisorResponseStrategy.SUMMARY`. The supervisor is told the unassigned-urgent issues are already triaged and to leave them alone, AND is forbidden from quoting absolute stress numbers in its summary (the prompt explains the system computes the AFTER score itself).
    3. **Deterministic 1-3-3-0 enforcer** (no LLM, only in `/demo/api/reshape`): after the LLM's mutations are applied, `enforce333Compliance(...)` promotes deferred-classified items into underfilled quickWin/maintenance slots and pushes true overflow off the user's plate (unassign + `deferred,next-sprint` + comment). Guarantees the day plan ends up exactly 1-3-3-0 even when the LLM under-fills. Surfaced as `complianceActionCount` in the response.
  - **ExplainerAiService** — Explains action plans in human-friendly language
  - **ProtectiveAiService** — Detects emotional signals and provides protective interventions
  - **FridayDeployAiService** — Assesses Friday deploy readiness

### Reshape response fields

Both `/api/reshape` and `/demo/api/reshape` surface deterministic-phase visibility on top of the LLM output:

| Field | Source | Meaning |
|---|---|---|
| `beforeScore` / `afterScore` | recomputed | Stress score before vs. after all mutations |
| `actionsApplied` | total | Pre-pass + LLM + (demo only) compliance actions, summed |
| `deterministicTriageCount` | pre-pass | Unassigned-urgent issues whose `urgent` / `priority:*` labels were stripped |
| `deterministicDefuseCount` | pre-pass | Issues whose body or `updatedAt` was normalised |
| `complianceActionCount` (demo only) | enforcer | Mutations emitted by the 1-3-3-0 enforcer (0 when the LLM lands compliance on its own) |
| `afterHoursBefore` / `afterHoursAfter` (demo) · `afterHoursIssues` (api) | WorldState | Issues with `updatedAt` outside 9 AM–6 PM in the active timezone |
| `llmUsed` | flag | `true` when the supervisor LLM ran; `false` means deterministic-only fallback (token expired or LLM down) |
| `explanation` | composed | LLM prose **bookended** by deterministic content: a "🧹 Deterministic pre-pass:" header listing triaged issue numbers, then the LLM summary, then a "📈 Outcome:" footer with the real measured stress drop. The supervisor is prompt-blocked from quoting absolute stress numbers, so the footer is the source of truth |

`/api/reshape` increments `SCHEMA_VERSION` to `3` for the new fields.

### GitHub mutation actions

[`GitHubAction`](backend/src/main/java/com/demo/burnout/goap/GitHubAction.java) is a sealed interface permitting six records:

| Action | Emitted by | Effect |
|--------|------------|--------|
| `AddLabels(issueNumber, labels)` | All `@Tool` methods | Add GitHub labels |
| `RemoveLabels(issueNumber, labels)` | `deferIssue`, `delegateIssue`, `triageUrgent` | Strip labels |
| `Comment(issueNumber, text)` | All `@Tool` methods | Post a comment |
| `Unassign(issueNumber, login)` | `deferIssue`, `delegateIssue` | Remove an assignee |
| `SetBody(issueNumber, body)` | `defuseChaosInputs` | Replace an empty body with a scope-pending placeholder (kills mystery-meat) |
| `SetUpdatedAt(issueNumber, instant)` | `defuseChaosInputs` | Normalise `updatedAt` to a stable mid-morning slot in the clock zone (kills after-hours + touched-today) |

[`DemoFlamegraphController.applyMutationsToIssues`](backend/src/main/java/com/demo/burnout/controller/DemoFlamegraphController.java) applies the full plan to the in-memory `IssueCache`.

### `BurnoutMutationTool` `@Tool` methods (10 total)

| Tool | Purpose |
|------|---------|
| `deferIssue` | Defer to next sprint (adds `deferred,next-sprint`, removes priority/urgent labels, unassigns) |
| `delegateIssue` | Reassign to balance load (adds `delegated,needs-owner`, unassigns) |
| `classifyAsQuickWin` | Mark as a quick-win in the 3-3-3 plan |
| `classifyAsMaintenance` | Mark as maintenance in the 3-3-3 plan |
| `markAsDeepWork` | Mark today's single deep-work item |
| `addScopeNeeded` | Flag an issue as needing scope (adds `needs-scope,blocked`) |
| `triageUrgent` | Strip `urgent` / `priority:critical` / `priority:high` from an unassigned issue (adds `triaged,backlog`) — **also called directly from the deterministic pre-pass** |
| `suggestBreak` | Recommend a 10–15 minute break |
| `slowIntake` | Recommend reducing new-issue intake |
| `blockCalendarTime` | Recommend blocking 2-hour focus time |

`defuseChaosInputs(Clock)` is a public method on `BurnoutMutationTool` but **not** annotated `@Tool` — it is only invoked by `BurnoutSupervisorService` from the deterministic pre-pass.

### MCP tools

| Tool | Description |
|------|-------------|
| `sync_issues` | Fetch GitHub issues via `gh` CLI and sync to backend |
| `show_burnout_wheel` | Display interactive flamegraph with 3-3-3 plan (dry run) |
| `reshape_day` | AI analysis + automatically apply labels to GitHub issues |
| `get_stress_score` | Quick stress check (0–100, LOW/MODERATE/HIGH) |

### API endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/issues/sync` | Yes | Sync issues from MCP app |
| GET | `/api/stress?repo=...&userId=...` | Yes | Get stress analysis |
| POST | `/api/reshape` | Yes | Run full reshape workflow |
| GET | `/demo/api/flamegraph?repo=...&userId=...` | No | Read-only flamegraph data for pre-synced repos |
| GET | `/demo/api/repos` | No | List repos currently synced in memory |
| POST | `/demo/api/sync?repo=owner/repo` | No | Sync issues from GitHub public API (rate-limited: 1 per repo per 5 min) |
| POST | `/demo/api/reshape` | No | Run reshape (deterministic pre-pass + LangChain4j supervisor + deterministic 1-3-3-0 enforcer) and apply mutations to IssueCache. Response includes `deterministicTriageCount`, `deterministicDefuseCount`, `complianceActionCount`, `afterHoursBefore`, `afterHoursAfter` |
| POST | `/demo/api/checkin` | No | Stress check-in — accepts optional `tz` param (e.g. `America/New_York`) for timezone-aware after-hours detection. Returns `breakdown`, `breakdownHints`, `breakdownIssues`, and `timezone` |

### Demo web app

A standalone flamegraph web page is served at `/flamegraph.html` for live demos outside VS Code. It has a **"Sync from GitHub"** button that fetches public repo issues directly — no MCP tool or GitHub token required.

**Live demo:** https://aka.ms/burnout-app

**Demo workflow:**
1. Share the URL with the audience: `https://aka.ms/burnout-app`
2. Enter a public repo (e.g. `roryp/burnout-app`) and click **Sync from GitHub**
3. The flamegraph renders automatically after sync

Alternatively, sync issues via MCP in VS Code first, then share the URL — the audience will see pre-synced repos as clickable buttons.

The demo endpoints never mutate GitHub issues or labels. Sync is rate-limited to 1 request per repo per 5 minutes to avoid exhausting GitHub's unauthenticated API limit (60 req/hour per IP).

The `POST /demo/api/seed` endpoint accepts `{"repo": "owner/repo", "issues": [...]}` and populates the `IssueCache` for testing without GitHub auth. Use this to test the flamegraph locally or on Azure without needing the full MCP sync flow.

**Seed data field reference** (Issue record — all fields use **camelCase**):

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `number` | int | Yes | Issue number |
| `title` | String | Yes | Issue title |
| `body` | String | Yes | Empty string = "mystery meat" (hurts Clarity score) |
| `labels` | `[{"name": "..."}]` | Yes | See label effects below |
| `assignees` | `[{"login": "..."}]` | Yes | Empty = unassigned (hurts Chaos if urgent) |
| `createdAt` | ISO 8601 | Yes | **camelCase! NOT `created_at`**. Must be recent for time-based metrics |
| `updatedAt` | ISO 8601 | Yes | **camelCase! NOT `updated_at`**. Must be recent for time-based metrics |
| `state` | String | Yes | `"open"` or `"closed"` |

**Labels that affect classification and stress:**
- Deep Work: `priority:critical`, `priority:high`, `architecture`, `security`, `deep-work`, `epic`, `feature`
- Quick Win: `good-first-issue`, `quick-win`, `low-hanging-fruit`, `trivial`
- Maintenance: `dependencies`, `documentation`, `triage`, `chore`, `refactor`, `tech-debt`, `ci`, `devops`, `maintenance`
- Chaos triggers: `urgent` (especially if unassigned or >24h old)
- After hours: set `updatedAt` to hours before 9 AM or after 6 PM in the user's timezone (defaults to server timezone if no `tz` param). Weekends also count as after-hours.
- Context switching: 6+ issues with `updatedAt` within the last 60 minutes

---

## Known issues and gotchas

- **POST to `/api/**` returns 403 even with `security.enabled=false`**: Spring Security's CSRF protection and filter chain ordering can block POST requests even when the `securityEnabled` flag is false. Workaround: use the `/demo/api/seed` endpoint (on the `permitAll` path) for testing. The MCP app works because it sends a GitHub Bearer token.
- **`favicon.ico` returns 403**: The security config permits `/favicon.ico` but no favicon file exists in static resources. This causes a harmless console error in browsers. Fix: add a favicon file to `backend/src/main/resources/static/`.
- **In-memory cache lost on restart**: The `IssueCache` uses `ConcurrentHashMap` — all synced data is lost when the backend restarts or the container is redeployed. Re-sync via MCP or re-seed via `/demo/api/seed`.
- **Azure OpenAI fallback**: When the LLM is unavailable (dummy credentials, network issues), all agents return deterministic fallback responses. The agent explanation will include `*LLM agents unavailable - using deterministic fallback*`.
- **Spring Boot version**: 3.5.10 with Spring Security 6.x. The `SecurityConfig` uses a single `SecurityFilterChain` bean with `permitAll` for demo/health paths and `authenticated` for `/api/**`.
- **Seed data uses camelCase, NOT snake_case**: The `Issue` Java record uses `createdAt`/`updatedAt` (camelCase). The GitHub REST API returns `created_at`/`updated_at` (snake_case), but the `GitHubIssue` record in `DemoFlamegraphController` uses `@JsonProperty` annotations to map snake_case to camelCase during `/demo/api/sync`. When seeding directly via `/demo/api/seed`, you must use camelCase — snake_case fields silently deserialize as `null`, causing all time-based breakdown metrics (Context Switching, After Hours, Sustained Load) to show as 0.
- **Seed data needs current timestamps**: Metrics like Context Switching (`issuesTouchedToday`) and After Hours are calculated relative to the server's current time. Using old/static dates (e.g. `2026-01-01`) will produce zero values. Always generate timestamps relative to "now" when seeding.
- **After-hours is timezone-aware**: The checkin endpoint accepts an optional `tz` field (IANA timezone, e.g. `America/New_York`). The checkin.html page auto-detects the browser timezone and sends it. Working hours are 9 AM–6 PM in the user's timezone; weekends always count as after-hours. If no `tz` is provided, the server's configured `demo.clock.zone` is used (default: `Africa/Johannesburg`).

---

## Security model

- **`SecurityConfig.java`** controls all auth. GitHub tokens are validated against the GitHub API and cached for 5 minutes.
- Paths that require auth: `/api/**` (all API endpoints)
- Paths that are public: `/actuator/**`, `/demo/**`, `/`, `/index.html`, `/flamegraph.html`, `/checkin.html`, `/study.html`, `/favicon.ico`, `OPTIONS /**`
- CORS allows: `*.azurecontainerapps.io`, `*.vscode-cdn.net`, `vscode-webview://*`, `localhost:*`
- For local dev, set `security.enabled=false` via system property or env var `SECURITY_ENABLED=false`
- On Azure, the backend uses managed identity (`AZURE_IDENTITY_CLIENT_ID`) for Azure OpenAI — no API keys needed

---

## PR instructions

- Always run `mvn test` (backend) and `npm run build` (mcp-app) before committing.
- If you change agent behavior, update the corresponding `@Agent` or `@SystemMessage` annotations.
- If you add a new `@Tool` method to `BurnoutMutationTool`, add the corresponding GitHub mutation (labels, comments) and update this file.
- If you add or modify MCP tools in `mcp-app/src/index.ts`, update the MCP tools table above.
- Keep graceful degradation in mind — every new AI feature must have a deterministic fallback.

---

## Key files

| File | Description |
|------|-------------|
| `backend/src/.../agent/AgentOrchestrator.java` | Central agent coordinator |
| `backend/src/.../agent/ExplainerAiService.java` | Plan explanation agent |
| `backend/src/.../agent/ProtectiveAiService.java` | Emotional support agent |
| `backend/src/.../agent/FridayDeployAiService.java` | Deploy readiness agent |
| `backend/src/.../agent/supervisor/BurnoutAgents.java` | 6 sub-agent interfaces with `@Agent` annotations (Triage, Defer, Delegate, Classify, Scope, Wellness) |
| `backend/src/.../agent/supervisor/BurnoutSupervisorService.java` | Three-phase reshape: deterministic pre-pass (`triageUrgent` + `defuseChaosInputs`) — always runs even when LLM is down — then LangChain4j supervisor (`maxAgentsInvocations: 15`, SUMMARY strategy, prompt-blocked from quoting absolute stress numbers). Returns `SupervisorResult(explanation, mutationPlan, estimatedStressScore, llmUsed, deterministicTriageCount, deterministicDefuseCount)` |
| `backend/src/.../agent/supervisor/BurnoutMutationTool.java` | 10 `@Tool` methods + `defuseChaosInputs(Clock)` (called only from the pre-pass) |
| `backend/src/.../goap/GitHubAction.java` | Sealed interface, 6 records: `AddLabels`, `RemoveLabels`, `Comment`, `Unassign`, `SetBody`, `SetUpdatedAt` |
| `backend/src/.../config/AgentConfiguration.java` | LangChain4j + Azure OpenAI wiring |
| `backend/src/.../config/SecurityConfig.java` | Spring Security: GitHub token validation, permitAll paths, CORS |
| `backend/src/.../service/IssueCache.java` | In-memory `ConcurrentHashMap` cache for synced issues |
| `backend/src/.../service/IssueClassifierService.java` | Classifies issues into DEEP_WORK, QUICK_WIN, MAINTENANCE, DEFERRED |
| `backend/src/.../service/ChaosMetricsService.java` | Calculates chaos score from issue patterns |
| `backend/src/.../service/ComplianceService.java` | Analyzes compliance (labels, assignees, SLA) |
| `backend/src/.../controller/DemoFlamegraphController.java` | Read-only demo endpoints + seed endpoint (no auth) + `enforce333Compliance` post-pass that guarantees the day plan ends 1-3-3-0 |
| `backend/src/.../controller/ReshapeController.java` | `/api/reshape` (auth) — `ReshapeResponse` (SCHEMA_VERSION=3) includes `deterministicTriageCount`, `deterministicDefuseCount`, `afterHoursIssues` |
| `backend/src/main/resources/static/index.html` | Landing page with links to all demo pages |
| `backend/src/main/resources/static/flamegraph.html` | Standalone flamegraph web app for live demos |
| `backend/src/main/resources/static/checkin.html` | Stress check-in page (supports URL params for deep-linking) |
| `backend/src/main/resources/static/study.html` | Researcher dashboard with clickthrough to checkin + tooltips |
| `backend/src/main/resources/application.yml` | Server, security, Azure OpenAI, and demo config |
| `scripts/seed-demo.sh` | Bash seed script: issues + checkins + study data in one command |
| `scripts/seed-demo.ps1` | PowerShell seed script (same as above, for Windows) |
| `scripts/smoke-test.ps1` | Post-deployment smoke test (26 assertions, seeds + verifies all endpoints) |
| `scripts/demo-screenshots.ps1` | Full before/after demo screenshot capture — 3 phases: seed BEFORE, sync real issues, capture 8 screenshots |
| `scripts/demo-screenshots.js` | Playwright screenshot logic (called by PS1 script, supports `before`/`after`/`study` modes) |
| `scripts/demo-screenshots.sh` | Bash version of demo screenshot script (same flow, for Linux/macOS/CI) |
| `scripts/record-demo.mjs` | Records ~30s demo video with scene title cards and issue drilldown |
| `scripts/seed-issues.sh` | Creates real GitHub issues via `gh` CLI (for live repos) |
| `mcp-app/src/index.ts` | MCP server with 4 tool definitions + 2 UI resources |
| `mcp-app/src/config.ts` | Backend URL config (reads from `.env`) |
| `mcp-app/src/backend-client.ts` | HTTP client for backend API calls |
| `mcp-app/src/demo-data.ts` | Fallback demo data when backend is unavailable |
| `mcp-app/src/ui/burnout-flamegraph.ts` | Flamegraph HTML/JS visualization for VS Code panel |
| `mcp-app/src/ui/burnout-wheel.ts` | Wheel visualization for VS Code panel |
| `infra/main.bicep` | Azure infrastructure: identity, OpenAI, ACR, Container Apps |
| `azure.yaml` | Azure Developer CLI config (backend service on containerapp host) |
