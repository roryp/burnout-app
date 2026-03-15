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
   # Option A: Automated script (captures screenshots via Playwright)
   .\scripts\demo-screenshots.ps1

   # Option B: Manual with Playwright MCP tool in Copilot Chat
   # Step 1: Seed BEFORE state
   .\scripts\seed-demo.ps1 -BaseUrl <azure-url>
   # Step 2: Take BEFORE screenshots (checkin + flamegraph)
   # Step 3: Seed AFTER state
   .\scripts\seed-demo.ps1 -BaseUrl <azure-url> -Mode after
   # Step 4: Take AFTER screenshots (checkin + flamegraph)
   # Step 5: Take study dashboard screenshot
   ```

3. **Screenshot checklist (5 screenshots minimum):**
   - `checkin-before.png` — Stress 100, CRITICAL, all bars red
   - `flamegraph-before.png` — 100/100 stress, 0 quick wins, 12 deferred
   - `checkin-after.png` — Stress ~26, LOW, most bars zeroed
   - `flamegraph-after.png` — ~65/100 stress, 70% Friday Score, 3-3-3 structure
   - `study-dashboard.png` — trend chart, participant cards, raw snapshots

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
