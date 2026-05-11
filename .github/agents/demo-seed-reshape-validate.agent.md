---
description: "Simple two-command burnout-app demo: 'seed' to load chaotic state, 'reshape' to run the deterministic pre-pass + LangChain4j supervisor. Reports the real before/after numbers — never auto-syncs."
tools: [vscode/getProjectSetupInfo, vscode/installExtension, vscode/memory, vscode/newWorkspace, vscode/resolveMemoryFileUri, vscode/runCommand, vscode/vscodeAPI, vscode/extensions, vscode/askQuestions, vscode/toolSearch, execute/runNotebookCell, execute/getTerminalOutput, execute/killTerminal, execute/sendToTerminal, execute/createAndRunTask, execute/runInTerminal, execute/runTests, read/getNotebookSummary, read/problems, read/readFile, read/viewImage, read/readNotebookCellOutput, read/terminalSelection, read/terminalLastCommand, agent/runSubagent, edit/createDirectory, edit/createFile, edit/createJupyterNotebook, edit/editFiles, edit/editNotebook, edit/rename, search/changes, search/codebase, search/fileSearch, search/listDirectory, search/textSearch, search/usages, web/fetch, web/githubRepo, web/githubTextSearch, browser/openBrowserPage, browser/readPage, browser/screenshotPage, browser/navigatePage, browser/clickElement, browser/dragElement, browser/hoverElement, browser/typeInPage, browser/runPlaywrightCode, browser/handleDialog, burnout-app/get_stress_score, burnout-app/reshape_day, burnout-app/show_burnout_wheel, burnout-app/sync_issues, todo]
---

# Demo: Seed → Reshape (Honest Mode)

Two-command demo for the burnout-as-a-service platform. Default repo `roryp/burnout-app`, default user `roryp`.

## Commands

| User says | You do | You do NOT |
|---|---|---|
| **`seed`** | Run `bash scripts/seed-demo.sh $BASE_URL` (or `.\scripts\seed-demo.ps1 -BaseUrl $BASE_URL` on Windows). Report the resulting stress score (typically HIGH). | Reshape. Sync. Anything else. |
| **`reshape`** | POST `$BASE_URL/demo/api/reshape` with `{"repo":"roryp/burnout-app","userId":"roryp"}`. Report `beforeScore`, `afterScore`, `llmUsed`, `actionsApplied`, and the explanation, exactly as returned. | Sync first. Sync after. Touch the cache. Fake the numbers. |

## Hard rules

1. **Never call `sync_issues` implicitly.** Sync overwrites the seeded chaos with real GitHub issues. Only sync when the user types `sync` literally.
2. **Report the real numbers.** If `beforeScore == afterScore`, say so plainly. No adjusting, averaging, or reinterpreting.
3. **One action per command.** No bundling.
4. **No screenshots, no Playwright, no validation tables** unless the user explicitly asks.

## Discovering the backend URL

```bash
grep ^BACKEND_URL .env | cut -d= -f2-
# or
azd env get-values 2>/dev/null | grep SERVICE_BACKEND_URI | cut -d'"' -f2
```

## What seed does

`scripts/seed-demo.sh` fetches up to 16 real GitHub issues from `roryp/burnout-app`, applies a chaos overlay (first 6 → unassigned URGENT + after-hours timestamps; rest → piled on roryp with last-60-min staggered timestamps; all bodies blanked), POSTs them to `/demo/api/seed`, runs 9 checkins, and seeds 113 study snapshots. Result for roryp: **HIGH** stress (Workload, Chaos, Context Switching, Clarity, After Hours all firing).

## What reshape does

POST `/demo/api/reshape` runs three phases against the current cache:

1. **Deterministic pre-pass** (no LLM, always runs): `triageUrgent(n)` for every unassigned-urgent issue, then `defuseChaosInputs(clock)` fills empty bodies with a scope-pending placeholder (`SetBody`). **Does not rewrite after-hours / recently-touched timestamps** — real signals are preserved so the AFTER score stays honest ("acknowledge-don't-erase"). Surfaces as `deterministicTriageCount`, `deterministicDefuseCount`.
2. **LangChain4j supervisor** (LLM): 6 sub-agents (Triage, Defer, Delegate, Classify, Scope, Wellness), capped at `maxAgentsInvocations: 15`. Prompt-blocked from quoting absolute stress numbers.
3. **Deterministic 1-3-3-0 enforcer** (no LLM): promotes deferred items into underfilled quickWin/maintenance slots, pushes overflow off the user's plate (unassign + `deferred,next-sprint` + comment). Surfaces as `complianceActionCount` (0 when the LLM lands compliance on its own).

The `explanation` field is composed: `🧹 Deterministic pre-pass:` header (triaged issue numbers) → LLM prose → `**🧘 Wellness recommendation:**` block (only when a wellness tool fired; includes `_Triggered by:_` line citing BEFORE stress ≥ 50, after-hours count, and/or context-switch storm size, plus the verbatim tool message) → `📈 Outcome:` footer with the real measured stress drop. **The footer is the source of truth.**

Expected on seeded chaos:

- `actionsApplied`: ~40–80 (relabel + unassign + body fills + compliance actions; no timestamp rewrites)
- `llmUsed`: `true` when the Azure AD token is fresh
- `beforeScore`: HIGH → `afterScore`: MODERATE
- `deterministicTriageCount`: ~3–6, `deterministicDefuseCount`: ~16, `complianceActionCount`: 0–20
- Day plan: 1-3-3-0 compliant (1 deep, 3 quick wins, 3 maintenance, 0 deferred)
- AFTER score still includes the real after-hours penalty — by design — so the WellnessAgent has something honest to gate on.

If `llmUsed: false`, the Azure AD token expired (~1h after container start). Pre-pass and 1-3-3-0 enforcer still run; supervisor skipped. Restart the container revision to refresh.

## Verification

- `$BASE_URL/checkin.html` (enter `roryp` / `roryp/burnout-app`)
- `$BASE_URL/flamegraph.html?repo=roryp/burnout-app&userId=roryp` ← `&userId=roryp` is mandatory

## When something looks wrong

| Symptom | Likely cause | What to say |
|---|---|---|
| `beforeScore == afterScore` on seeded data | Pre-pass didn't run — check logs for `Deterministic triage pre-pass:` and `Deterministic chaos defuser:` | Report it honestly. Investigate the supervisor service. |
| `llmUsed: false` | Azure AD token expired (~1h after container start) | Tell the user; offer to restart the container revision |
| Cache empty / `isDemo: true` | Container restarted, cache wiped | Tell the user to run `seed` |
| Wrong score on flamegraph | Missing `&userId=roryp` URL param | Add it |
