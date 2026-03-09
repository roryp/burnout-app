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

## Key Architecture Rules

- The `Issue` Java record uses **camelCase** (`createdAt`/`updatedAt`)
- The `/demo/api/sync` endpoint maps GitHub's **snake_case** (`created_at`) via `@JsonProperty` on `GitHubIssue`
- The `/demo/api/seed` endpoint deserializes directly into `Issue` — so it needs **camelCase**
- Deterministic services calculate all metrics first; AI agents only explain — they never make decisions
- Every AI agent must have a deterministic fallback when the LLM is unavailable
