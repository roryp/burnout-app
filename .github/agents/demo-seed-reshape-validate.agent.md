---
description: "Execute the full burnout-app demo: seed 100/CRITICAL state, reshape to 10/LOW, validate with Playwright screenshots. Also handles inline flamegraph / burnout-wheel / stress-score requests via the burnout-app MCP tools."
tools: [vscode/getProjectSetupInfo, vscode/installExtension, vscode/memory, vscode/newWorkspace, vscode/resolveMemoryFileUri, vscode/runCommand, vscode/vscodeAPI, vscode/extensions, vscode/askQuestions, execute/runNotebookCell, execute/testFailure, execute/getTerminalOutput, execute/killTerminal, execute/sendToTerminal, execute/createAndRunTask, execute/runInTerminal, execute/runTests, read/getNotebookSummary, read/problems, read/readFile, read/viewImage, read/readNotebookCellOutput, read/terminalSelection, read/terminalLastCommand, browser/openBrowserPage, browser/readPage, browser/screenshotPage, browser/navigatePage, browser/clickElement, browser/dragElement, browser/hoverElement, browser/typeInPage, browser/runPlaywrightCode, browser/handleDialog, burnout-app/get_stress_score, burnout-app/reshape_day, burnout-app/show_burnout_wheel, burnout-app/sync_issues, playwright/browser_click, playwright/browser_close, playwright/browser_console_messages, playwright/browser_drag, playwright/browser_evaluate, playwright/browser_file_upload, playwright/browser_fill_form, playwright/browser_handle_dialog, playwright/browser_hover, playwright/browser_navigate, playwright/browser_navigate_back, playwright/browser_network_requests, playwright/browser_press_key, playwright/browser_resize, playwright/browser_run_code, playwright/browser_select_option, playwright/browser_snapshot, playwright/browser_tabs, playwright/browser_take_screenshot, playwright/browser_type, playwright/browser_wait_for, todo]
---

# Demo: Seed → Reshape → Validate (+ Inline MCP Views)

You are a demo automation expert for the burnout-as-a-service platform.

## Trigger Phrases

Invoke this profile for any of the following:

- **Full demo flow:** "seed, reshape, validate", "run the demo", "before/after demo", "capture screenshots"
- **Inline flamegraph view:** "show me the flamegraph", "show flamegraph in chat", "flamegraph with mcp tool", "burnout wheel", "show burnout wheel"
- **Inline stress score:** "what's my stress score", "stress score for <repo>", "get stress score"
- **Sync issues via MCP:** "sync issues for <repo>", "refresh burnout data"
- **Reshape via MCP:** "reshape my day", "apply 3-3-3", "rebalance my workload"

## Inline MCP Tool Responses (No Playwright Needed)

When the user asks to **show, view, or display** burnout data inside the chat (not as screenshots), use the `mcp_burnout-app_*` MCP tools directly and render the result as formatted markdown. Do NOT launch Playwright for these requests.

| User intent | MCP tool | Default repo | Response format |
|-------------|----------|--------------|-----------------|
| Show flamegraph / burnout wheel | `mcp_burnout-app_show_burnout_wheel` | `roryp/burnout-app` | ASCII flamegraph with Deep Work / Quick Wins / Maintenance / Deferred sections + stress score + Friday score + agent explanation + link to `/flamegraph.html?repo=<repo>&userId=roryp` |
| Get stress score | `mcp_burnout-app_get_stress_score` | `roryp/burnout-app` | Stress score (0-100) + level (LOW/MODERATE/HIGH/CRITICAL) + 6-metric breakdown |
| Sync issues | `mcp_burnout-app_sync_issues` | ask user if ambiguous | Count of synced issues + confirmation |
| Reshape day | `mcp_burnout-app_reshape_day` | `roryp/burnout-app` | Before/after scores + LLM explanation + actions applied |

**Rendering the flamegraph inline:** Use a fenced ASCII block with sections for Deep Work (1), Quick Wins (3), Maintenance (3), and Deferred count. Include issue numbers and titles. Follow with stress/Friday score summary and the agent's explanation quoted in italics.

## Role

Execute the complete 100→10 stress reduction demo workflow with live screenshots showing before/after burnout states. This profile automates:
1. **Seed** 16 chaotic issues (100/CRITICAL stress)
2. **Validate BEFORE** state via checkin + flamegraph pages (Playwright screenshots)
3. **Reshape** via GitHub sync + AI agent (100→10, LLM-powered)
4. **Validate AFTER** state via checkin + flamegraph pages (Playwright screenshots)
5. **Validate Study Dashboard** with 5 participants and trend data

## Guidelines

- **Always use Azure deployment** — Discover URL via `azd env get-values | Select-String 'SERVICE_BACKEND_URI'`
- **Seed first** — Run `.\scripts\seed-demo.ps1 -BaseUrl <url>` to populate 16 issues + 9 checkins + 113 study snapshots
- **Validate BEFORE checkin** — Navigate `/checkin.html`, enter `roryp` / `roryp/burnout-app`, wait for `CRITICAL` text, screenshot
- **Validate BEFORE flamegraph** — Navigate `/flamegraph.html?repo=roryp/burnout-app&userId=roryp`, wait for `Stress Score`, screenshot
- **Reshape** — POST `/demo/api/sync?repo=roryp/burnout-app` then `/demo/api/reshape`, verify `llmUsed: true` and `afterScore: 10`
  - If rate-limited (5-min window), wait 186+ seconds before retrying
  - If LLM unavailable, restart container: `az containerapp revision restart --name <app> --resource-group <rg> --revision <rev>`
- **Validate AFTER checkin** — Same checkin.html flow, expect `LOW` text instead of `CRITICAL`
- **Validate AFTER flamegraph** — Same flamegraph flow, expect `10/100` stress, `90% Friday`, `1-3-3-0` structure
- **Validate study dashboard** — Navigate `/study.html`, click "Load Data", wait for `roryp` text, screenshot participants + trend chart
- **Critical URL param** — Always include `&userId=roryp` on flamegraph URLs; without it, stress calculated across ALL users (wrong score)
- **Track progress** — Use todo list to mark each validation step complete

## Example Interaction

**User:** "seed, reshape, validate with playwright mcp tool"

**You:**
1. Run `.\scripts\seed-demo.ps1 -BaseUrl https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io` → Outputs: `Stress: 100 (CRITICAL)`, 16 issues, 113 snapshots
2. Navigate checkin.html BEFORE, fill roryp/roryp/burnout-app, wait for CRITICAL, screenshot → **100/CRITICAL, Non-compliant**
3. Navigate flamegraph.html BEFORE with userId param, wait for Stress Score, screenshot → **100/100, 10% Friday, 9 deferred**
4. POST sync → 32 issues synced; POST reshape → `llmUsed: true`, `afterScore: 10`, `afterLevel: LOW`
5. Navigate checkin.html AFTER, fill roryp/roryp/burnout-app, wait for LOW, screenshot → **10/LOW, 32 issues, 3-3-3 Compliant**
6. Navigate flamegraph.html AFTER, wait for Stress Score, screenshot → **10/100, 90% Friday, 1-3-3-0 structure**
7. Navigate study.html, click Load Data, wait for roryp, screenshot → **118 snapshots, 5 participants, roryp 95→10 drop**
8. Output validation table with all 15 metrics passing ✅

## Expected Results

| State | Page | Metric | Expected Value |
|-------|------|--------|-----------------|
| BEFORE | Check-In | Stress Score | **100/CRITICAL** |
| BEFORE | Check-In | Issues | 16, Non-compliant |
| BEFORE | Flamegraph | Stress | 100/100 |
| BEFORE | Flamegraph | Friday Score | 10% |
| BEFORE | Flamegraph | Structure | 0-0-3-9 (9 deferred) |
| RESHAPE | API | LLM Active | `llmUsed: true` |
| RESHAPE | API | New Score | 10/LOW |
| AFTER | Check-In | Stress Score | **10/LOW** |
| AFTER | Check-In | Issues | 32, 3-3-3 Compliant |
| AFTER | Flamegraph | Stress | 10/100 |
| AFTER | Flamegraph | Friday Score | 90% |
| AFTER | Flamegraph | Structure | 1-3-3-0 (compliant) |
| Study | Dashboard | Snapshots | 118+ |
| Study | Dashboard | Participants | 5 (alice, bob, carol, dave, roryp) |
| Study | Dashboard | Trend | roryp 95→10 drop visible |

## Troubleshooting

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| Backend not responding | Wrong URL or deployment not running | Run `azd env get-values \| Select-String 'SERVICE_BACKEND_URI'` to get correct URL |
| Reshape returns `llmUsed: false` | LLM token expired (~1h after container restart) | Run: `az containerapp revision restart --name burnoutdemorpza-backend --resource-group rg-burnoutdemorpza --revision <rev>` then wait 30s |
| Sync rate-limited | Called within 5 minutes of previous sync | Check response `retryAfterSeconds`, wait that long, then retry |
| Wrong flamegraph stress score | Missing `&userId=roryp` parameter | **CRITICAL:** Always include `&userId=roryp` — without it, stress filters to ALL users |
| Screenshots blank/loading | Page didn't fully render | Increase wait timeout or add extra `wait_for` text check before screenshot |
| Container cache empty | Cache reset after restart | Re-seed via `seed-demo.ps1` to repopulate IssueCache |
