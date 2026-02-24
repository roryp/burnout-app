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
AZURE_OPENAI_DEPLOYMENT=gpt-5-mini
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

### Testing the demo flamegraph locally

1. Start the backend (see "Running the backend locally" above)
2. Seed test data — POST to `/demo/api/seed` (no auth needed):
   ```bash
   curl -X POST http://localhost:8080/demo/api/seed \
     -H 'Content-Type: application/json' \
     -d '{"repo":"owner/repo","issues":[{"number":1,"title":"Test issue","body":"Body","labels":[{"name":"bug"}],"assignees":[{"login":"user"}],"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-02T00:00:00Z","state":"open","pull_request":null}]}'
   ```
3. Open `http://localhost:8080/flamegraph.html?repo=owner/repo` in a browser
4. Or check APIs directly: `GET /demo/api/repos` and `GET /demo/api/flamegraph?repo=owner/repo`

### Testing on Azure Container Apps

After `azd up`, the in-memory `IssueCache` is empty. To test the deployed flamegraph:
1. Seed data via `POST /demo/api/seed` (same payload as above, use the Azure URL)
2. Or sync real issues via the MCP `sync_issues` tool in VS Code (this authenticates with GitHub)
3. Navigate to `https://<your-app>.azurecontainerapps.io/flamegraph.html?repo=owner/repo`
4. Verify: `GET /demo/api/repos` should list synced repos, `GET /actuator/health` should return UP

---

## Code style and conventions

- **Backend**: Java 21, Spring Boot 3, LangChain4j. Use `@Agent` annotations for sub-agent interfaces. Use `@Tool` annotations for mutation methods.
- **MCP App**: TypeScript strict mode, ES modules (`"type": "module"` in package.json). Dependencies: `@modelcontextprotocol/sdk`, `zod`, `dotenv`.
- **Configuration**: Use Spring `@Configuration` and `@Bean` annotations. Azure OpenAI config is in `AgentConfiguration.java`.
- **Key design principle**: Deterministic services calculate all metrics and GOAP plans first. AI agents **only explain and support** — they never make decisions.
- **Graceful degradation**: Every agent must have a fallback path when the LLM is unavailable. If LLM fails, return deterministic responses.

---

## Architecture overview

The system has two main components:

1. **MCP App** (Node.js) — Exposes 4 tools to VS Code Copilot Chat via stdio transport. Calls the backend over HTTP with a GitHub Bearer token.
2. **Java Backend** (Spring Boot + LangChain4j) — Runs the AI agent orchestration, stress analysis, and GOAP planning.

### Agent hierarchy

- **AgentOrchestrator** — Central coordinator that dispatches to:
  - **BurnoutSupervisorService** — Supervisor pattern with 5 sub-agents (DeferAgent, DelegateAgent, ClassifyAgent, ScopeAgent, WellnessAgent)
  - **ExplainerAiService** — Explains GOAP action plans in human-friendly language
  - **ProtectiveAiService** — Detects emotional signals and provides protective interventions
  - **FridayDeployAiService** — Assesses Friday deploy readiness

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

---

## Known issues and gotchas

- **POST to `/api/**` returns 403 even with `security.enabled=false`**: Spring Security's CSRF protection and filter chain ordering can block POST requests even when the `securityEnabled` flag is false. Workaround: use the `/demo/api/seed` endpoint (on the `permitAll` path) for testing. The MCP app works because it sends a GitHub Bearer token.
- **`favicon.ico` returns 403**: The security config permits `/favicon.ico` but no favicon file exists in static resources. This causes a harmless console error in browsers. Fix: add a favicon file to `backend/src/main/resources/static/`.
- **In-memory cache lost on restart**: The `IssueCache` uses `ConcurrentHashMap` — all synced data is lost when the backend restarts or the container is redeployed. Re-sync via MCP or re-seed via `/demo/api/seed`.
- **Azure OpenAI fallback**: When the LLM is unavailable (dummy credentials, network issues), all agents return deterministic fallback responses. The agent explanation will include `*LLM agents unavailable - using deterministic fallback*`.
- **Spring Boot version**: 3.5.10 with Spring Security 6.x. The `SecurityConfig` uses a single `SecurityFilterChain` bean with `permitAll` for demo/health paths and `authenticated` for `/api/**`.

---

## Security model

- **`SecurityConfig.java`** controls all auth. GitHub tokens are validated against the GitHub API and cached for 5 minutes.
- Paths that require auth: `/api/**` (all API endpoints)
- Paths that are public: `/actuator/**`, `/demo/**`, `/flamegraph.html`, `/favicon.ico`, `OPTIONS /**`
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
| `backend/src/.../agent/supervisor/BurnoutAgents.java` | 5 sub-agent interfaces with `@Agent` annotations |
| `backend/src/.../agent/supervisor/BurnoutSupervisorService.java` | Supervisor pattern orchestration |
| `backend/src/.../agent/supervisor/BurnoutMutationTool.java` | GitHub mutation tools (`@Tool` methods) |
| `backend/src/.../config/AgentConfiguration.java` | LangChain4j + Azure OpenAI wiring |
| `backend/src/.../config/SecurityConfig.java` | Spring Security: GitHub token validation, permitAll paths, CORS |
| `backend/src/.../service/IssueCache.java` | In-memory `ConcurrentHashMap` cache for synced issues |
| `backend/src/.../service/IssueClassifierService.java` | Classifies issues into DEEP_WORK, QUICK_WIN, MAINTENANCE, DEFERRED |
| `backend/src/.../service/ChaosMetricsService.java` | Calculates chaos score from issue patterns |
| `backend/src/.../service/ComplianceService.java` | Analyzes compliance (labels, assignees, SLA) |
| `backend/src/.../controller/DemoFlamegraphController.java` | Read-only demo endpoints + seed endpoint (no auth) |
| `backend/src/main/resources/static/flamegraph.html` | Standalone flamegraph web app for live demos |
| `backend/src/main/resources/application.yml` | Server, security, Azure OpenAI, and demo config |
| `mcp-app/src/index.ts` | MCP server with 4 tool definitions + 2 UI resources |
| `mcp-app/src/config.ts` | Backend URL config (reads from `.env`) |
| `mcp-app/src/backend-client.ts` | HTTP client for backend API calls |
| `mcp-app/src/demo-data.ts` | Fallback demo data when backend is unavailable |
| `mcp-app/src/ui/burnout-flamegraph.ts` | Flamegraph HTML/JS visualization for VS Code panel |
| `mcp-app/src/ui/burnout-wheel.ts` | Wheel visualization for VS Code panel |
| `infra/main.bicep` | Azure infrastructure: identity, OpenAI, ACR, Container Apps |
| `azure.yaml` | Azure Developer CLI config (backend service on containerapp host) |
