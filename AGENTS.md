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

---

## Testing instructions

- Run backend tests: `cd backend && mvn test`
- Integration test: `backend/src/test/java/com/demo/burnout/IntegrationTest.java`
- MCP app has no test suite currently — validate by building (`npm run build`) and confirming no TypeScript errors.
- After making changes, always run `mvn clean package -DskipTests` to verify the backend compiles, then `mvn test` to run the test suite.
- To test the full flow locally: start the backend, build the MCP app, reload VS Code, then use the MCP tools in Copilot Chat.

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

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/issues/sync` | Sync issues from MCP app |
| GET | `/api/stress?repo=...&userId=...` | Get stress analysis |
| POST | `/api/reshape` | Run full reshape workflow |

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
| `mcp-app/src/index.ts` | MCP server with tool definitions |
| `mcp-app/src/ui/burnout-flamegraph.ts` | Flamegraph visualization |
| `mcp-app/src/ui/burnout-wheel.ts` | Wheel visualization |
