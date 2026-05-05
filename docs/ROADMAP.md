# Burnout-App Roadmap

Shipped milestones since project inception, grouped by theme.

---

## Completed Since Inception (Jan 2026 – Apr 2026)

### Core platform (Jan 2026)
- ✅ Multi-agent architecture on **LangChain4j Supervisor Pattern** with 6 sub-agents
  (Triage, Defer, Delegate, Classify, Scope, Wellness) plus a deterministic pre-pass
  (`triageUrgent` + `defuseChaosInputs`) and Explainer, Protective, and
  Friday-Deploy AI services
- ✅ Deterministic services (IssueClassifier, ChaosMetrics, Compliance) calculate
  all metrics first — agents only explain; every agent has a deterministic fallback
- ✅ GOAP planner (`goap/`) for action planning
- ✅ Azure OpenAI integration with managed identity (no API keys on Azure)
- ✅ GitHub CLI integration (`sync_issues`) + GitHub label mutations from
  `reshape_day`
- ✅ In-memory `IssueCache` + `Issue` record (camelCase) with
  `@JsonProperty` mapping for GitHub snake_case on sync

### MCP + VS Code integration (Jan–Feb 2026)
- ✅ 4 MCP tools exposed to Copilot Chat: `sync_issues`, `get_stress_score`,
  `show_burnout_wheel`, `reshape_day`
- ✅ Burnout 3-3-3 Wheel UI and Burnout Flamegraph UI rendered in VS Code
  panel via MCP UI resources
- ✅ `.vscode/mcp.json` pre-configured; `gh auth token` used for GitHub auth

### Azure deployment (Feb 2026)
- ✅ One-command deploy via `azd up` (Bicep: identity, OpenAI, ACR,
  Container Apps, Container Apps Env, PostgreSQL)
- ✅ Live demo at **https://aka.ms/burnout-app**
- ✅ Container App min replicas tuned (cold-start fix)
- ✅ Spring Security with GitHub token validation on `/api/**`, `permitAll`
  on `/demo/**` + static assets, CORS for `*.azurecontainerapps.io` and
  vscode-webview

### Demo web app (Feb–Mar 2026)
- ✅ Standalone pages: `/` (landing), `/flamegraph.html`, `/checkin.html`,
  `/study.html`
- ✅ Self-service GitHub sync button on the demo web app (no token needed)
- ✅ `/demo/api/*` read-only endpoints + `/demo/api/seed` for deterministic
  test data, rate-limited `/demo/api/sync` (1 req / repo / 5 min)
- ✅ Timezone-aware after-hours detection (browser tz → `tz` param; 9am–6pm
  + weekends rule consistent across ChaosMetricsService, WorldState,
  SyntheticTimeResolver)
- ✅ Stress check-in with self-reported score + free-text notes
- ✅ Stress breakdown with 6 metrics, hover tooltips, and per-metric
  drilldown into contributing issues (`breakdownIssues`)

### Persistence + research dashboard (Mar 2026)
- ✅ **PostgreSQL persistence** for stress snapshots
  (`StressSnapshotRepository`, `StressSnapshotService`) — partial delivery
  of Phase 1.1 (historical storage) and full delivery of Phase 2.1
  (historical stress graph) and Phase 1.4 (team-level insights, anonymized)
- ✅ Researcher / study dashboard with 5 seeded participants (alice, bob,
  carol, dave, roryp), 14-day trend chart, pagination, sticky headers,
  self-score + notes columns, clickthrough to per-participant drilldown
- ✅ `/demo/api/study/reset` + `/demo/api/study/seed` endpoints

### Ops tooling (Mar–Apr 2026)
- ✅ `scripts/seed-demo.{sh,ps1}` — one-command seed for issues + checkins
  + 113 study snapshots
- ✅ `scripts/smoke-test.ps1` — 26-assertion post-deployment regression
- ✅ `scripts/demo-screenshots.{ps1,sh,js}` — Playwright before/after
  screenshot pipeline (8 PNGs)
- ✅ `scripts/record-demo.mjs` — ~30s demo video with scene title cards
- ✅ Comprehensive testing guide in `.github/copilot-instructions.md`

### Documentation + narrative (Mar–Apr 2026)
- ✅ `AGENTS.md`, `README.md`, `PSYCHOLOGY.md`, per-phase infographics
  and slide HTML
- ✅ PowerPoint generators for foundations, pipeline, sequence, and
  swimlane diagrams
- ✅ Deploy-app and demo-seed-reshape-validate subagent profiles

---

*Last updated: April 2026*
