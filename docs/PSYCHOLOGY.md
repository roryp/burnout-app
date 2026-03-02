# The Psychology & Science Behind Burnout-as-a-Service

Technical reference for every psychological model, algorithm, and constant in the system. Each section maps research-backed concepts to their implementation.

---

## Table of Contents

- [Algorithm Pipeline Overview](#algorithm-pipeline-overview)
- [A Developer's Journey: From Burnout to Balance](#a-developers-journey-from-burnout-to-balance)

1. [Theoretical Foundations](#1-theoretical-foundations)
2. [The 3-3-3 Day Structure](#2-the-3-3-3-day-structure)
3. [Issue Classification System](#3-issue-classification-system)
4. [Stress Score Algorithm](#4-stress-score-algorithm)
5. [Chaos Metrics](#5-chaos-metrics)
6. [Compliance Violation Detection](#6-compliance-violation-detection)
7. [Emotional Signal Detection (Plutchik Model)](#7-emotional-signal-detection-plutchik-model)
8. [Protective Intervention System](#8-protective-intervention-system)
9. [Friday Deploy Confidence](#9-friday-deploy-confidence)
10. [Calendar Fragmentation](#10-calendar-fragmentation)
11. [AI Agent Architecture](#11-ai-agent-architecture)
12. [Flamegraph Visualization Psychology](#12-flamegraph-visualization-psychology)
13. [Priority Weighting & Day Plan](#13-priority-weighting--day-plan)
14. [Graceful Degradation](#14-graceful-degradation)
15. [Complete Constants Reference](#15-complete-constants-reference)

---

## Algorithm Pipeline Overview

The system flows through 6 stages — from raw GitHub issues to actionable mutations. Every algorithm is deterministic except the AI agents, which always have a fallback path.

<img src="images/algorithm-pipeline.png" alt="Widescreen infographic showing the 6-stage algorithm pipeline: Ingestion, Classification, Metrics and Compliance, WorldState, AI Agents, and Output — with formulas, thresholds, and connections between all components" width="100%"/>

*The complete algorithm pipeline: (1) Issues are ingested from GitHub into a ConcurrentHashMap cache as typed Java records. (2) The classifier assigns each issue to one of 4 buckets (DEEP_WORK → QUICK_WIN → MAINTENANCE → DEFERRED) using priority-ordered first-match rules + label-based hour estimation. (3) Chaos score (0–10, five binary ≥ criteria × 2 pts) and compliance score (100 → 0, eight violation types at three severity levels) are calculated deterministically. (4) All metrics feed into WorldState's 18 capped variables, which produce the stress score (0–100, eight graduated components). (5) The LangChain4j Supervisor Pattern (gpt-5.2, SUMMARY strategy, max 10 invocations) coordinates 5 sub-agents with 9 @Tool methods, plus 3 support agents for explanation, emotional support, and deploy readiness. (6) Outputs include the mutation plan (applied to GitHub via MCP), a rebalanced 1+3+3 day structure, Friday deploy score (uses strictly-greater thresholds), and protective messages.*

---

## A Developer's Journey: From Burnout to Balance

The story of how the system works — told through a developer named Alex.

### Scene 1: The Breaking Point

Alex stares at their screen. 12 open issues, 3 critical bugs, Slack piling up, and it's 7 PM. No plan, no priorities — just an avalanche of work.

<img src="images/scene1-overwhelmed-developer.png" alt="Overwhelmed developer at desk with chaotic notifications and 12 unorganized issues" width="800"/>

*Starting point: a developer overwhelmed by unstructured work — all three Maslach Burnout Inventory dimensions in play (emotional exhaustion, depersonalization, reduced accomplishment).*

### Scene 2: The 3-3-3 Structure

The system's first intervention: impose structure. The 3-3-3 rule limits the day to **1 deep work, 3 quick wins, 3 maintenance** — matching the brain's capacity for different attention types.

<img src="images/scene2-333-day-structure.png" alt="3-3-3 Day Structure showing 1 deep work, 3 quick wins, 3 maintenance tasks with compliance rules" width="800"/>

*The 3-3-3 day structure with compliance rules. Max 7 active issues (Miller's 7±2), overflow routed to Deferred.*

### Scene 3: Classifying the Chaos

Deterministic services classify every issue into four categories based on labels. Pure pattern matching — no LLM required.

<img src="images/scene3-issue-classification.png" alt="Issue Classification Pipeline showing priority-ordered cascade from labels to four buckets" width="800"/>

*Priority-ordered cascade: labels enter from the left, each issue lands in the first matching bucket (Deep Work → Quick Win → Maintenance → Deferred).*

### Scene 4: Measuring the Stress

The system builds a **WorldState** from 18 discrete variables, then calculates a stress score (0–100) by summing weighted components.

<img src="images/scene4-stress-score.png" alt="Stress score calculation showing 8 graduated components with formulas and caps" width="800"/>

*8 graduated factors — workload, deep work imbalance, chaos bucket, context switching, clarity tax, sustained stress, after-hours — each with specific formulas and caps, summed to a total capped at 100.*

### Scene 5: The Supervisor Agent Steps In

The LangChain4j **Supervisor Pattern** takes over. A planner LLM coordinates 5 specialized sub-agents to rebalance the workload.

<img src="images/scene5-supervisor-pattern.png" alt="LangChain4j Supervisor Agent architecture with 5 sub-agents and mutation plan output" width="800"/>

*The Supervisor pattern: planner model (gpt-5.2, SUMMARY strategy, max 10 invocations) coordinates 5 sub-agents — Defer, Delegate, Classify, Scope, Wellness — each with `@Tool` methods from `BurnoutMutationTool`. Output: a mutation plan of label additions, removals, and comments.*

### Scene 6: The Flamegraph — Seeing Stress

The flamegraph transforms abstract numbers into visceral visualization. Fire = danger, height = depth, color = urgency.

<img src="images/flamegraph-landing.png" alt="Burnout Flamegraph landing page" width="800"/>

*Landing page: enter a public repo, click Sync from GitHub.*

Alex's BEFORE state — 12 issues with no labels, all in Deferred:

<img src="images/flamegraph-before.png" alt="Flamegraph showing 12 unclassified issues all in Deferred" width="800"/>

*All 12 issues in Deferred because none have classification labels. No structure at all.*

### Scene 7: Reshaping the Day

The `reshape_day` tool applies the mutation plan. Labels are added, comments posted, issues deferred. Chaos becomes structure:

<img src="images/flamegraph-after.png" alt="Flamegraph after reshaping showing 1 Deep Work, 3 Quick Wins, 3 Maintenance, 5 Deferred" width="800"/>

*AFTER: 1 Deep Work, 3 Quick Wins, 3 Maintenance, 5 Deferred. Same 12 issues, now with a plan.*

### Scene 8: The Balanced Developer

| Metric | Before | After |
|--------|--------|-------|
| Deep Work | 0 | 1 |
| Quick Wins | 0 | 3 |
| Maintenance | 0 | 3 |
| Deferred | 12 (all) | 5 (intentional) |
| Stress Score | 25/100 | 20/100 |
| 3-3-3 Compliant | No | Yes |

Same issues, same deadlines — but structured. That's the difference between burnout and balance.

---

## 1. Theoretical Foundations

| Framework | Key Concept | How We Use It |
|-----------|-------------|---------------|
| **Maslach Burnout Inventory** | 3 dimensions: exhaustion, depersonalization, reduced accomplishment | After-hours → exhaustion; mystery meat → depersonalization; no deep work → reduced accomplishment |
| **Cognitive Load Theory** (Sweller, 1988) | Working memory is limited; extraneous load must be minimized | 3-3-3 cap at 7 items; classification removes ambiguity load |
| **Yerkes-Dodson Law** (1908) | Performance peaks at moderate stress, drops at extremes | Stress score targets 30–50 (MODERATE) as optimal zone |
| **Deep Work** (Newport, 2016) | Sustained focused work requires 90+ min uninterrupted blocks | Calendar fragmentation check; exactly 1 deep work item per day |
| **Pomodoro / Time Boxing** | Short focused intervals with breaks maintain energy | Quick wins as natural break-points between deep work sessions |
| **Plutchik's Wheel** (2001) | 8 primary emotions with behavioral signatures | 4 emotions detected from GitHub signals (frustration, exhaustion, overwhelm, anxiety) |

---

## 2. The 3-3-3 Day Structure

The system enforces a daily structure that matches cognitive capacity:

| Slot | Count | Purpose | Psychology |
|------|-------|---------|-----------|
| Deep Work | 1 | Cognitively demanding task | Flow state requires singular focus (Csikszentmihalyi) |
| Quick Wins | 3 | Small, completable tasks | Dopamine from completion; momentum builders |
| Maintenance | 3 | Routine upkeep | Low cognitive overhead; batch-processable |
| **Total** | **7** | | **Miller's Law: 7 ± 2 working memory limit** |

Overflow beyond 7 active items → automatically deferred. Deep work gets a protected 90-minute block.

---

## 3. Issue Classification System

A priority-ordered first-match cascade. Each issue lands in the **first** matching bucket:

| Priority | Bucket | Label Triggers |
|----------|--------|---------------|
| 1st | DEEP_WORK | `priority:critical`, `architecture`, `security`, `deep-work`, `performance`, `RFC` |
| 2nd | QUICK_WIN | `good-first-issue`, `quick-win`, `size:S`, `typo`, `chore`, `CSS` |
| 3rd | MAINTENANCE | `dependencies`, `documentation`, `maintenance`, `tech-debt`, `refactor` |
| 4th | DEFERRED | Everything else (no matching labels) |

**Hour estimation** uses label-based lookup: `security`→8h, `architecture`→6h, `deep-work`→4h, `performance`→4h, `refactor`→3h, `documentation`→2h, `chore`→1h, default→2h.

**Clear scope detection** looks for: checkboxes (`- [ ]`), "acceptance criteria", "expected"/"actual", numbered steps, or "definition of done".

---

## 4. Stress Score Algorithm

Score 0–100, calculated from 6 dimensions (8 graduated components):

| Dimension | Max | Formula |
|-----------|-----|---------|
| **Workload** | 40 | `min(20, (assigned − 7) × 4)` if > 7 issues, + `(deepWork − 1) × 10` if > 1, + `5` if deepWork = 0 |
| **Chaos** | 30 | `chaosBucket.ordinal() × 10` (LOW=0, MEDIUM=10, HIGH=20, CRITICAL=30) |
| **Context Switching** | 15 | `min(15, (touchedToday − 5) × 3)` if > 5 |
| **Clarity** | 15 | `min(10, mysteryMeat × 2)` + `min(5, unclearQuickWins)` |
| **Sustained** | 15 | `min(15, consecutiveHighDays × 5)` |
| **After-Hours** | 10 | `min(10, afterHoursIssues × 5)` |

**Stress levels:** ≥ 70 CRITICAL, ≥ 50 HIGH, ≥ 30 MODERATE, < 30 LOW.

---

## 5. Chaos Metrics

Chaos score (0–10) measures environmental disorder. Five binary signals, each worth 2 points:

| Signal | Trigger | What It Reveals |
|--------|---------|-----------------|
| Mystery meat | ≥ 3 issues with blank body or no assignees | Team not investing in issue quality |
| Unresolved urgent | ≥ 3 urgent items > 24h old | Broken priority system |
| Issues touched today | ≥ 6 updated in 60 min | Reactive firefighting |
| After-hours | Any update outside 8am–6pm or weekend | Boundary erosion |
| Label explosion | ≥ 12 distinct labels | Taxonomy chaos → cognitive overhead |

**Chaos buckets:** ≤ 2 LOW, ≤ 5 MEDIUM, ≤ 8 HIGH, > 8 CRITICAL.

---

## 6. Compliance Violation Detection

8 violation types, organized by severity. Score starts at 100, deductions per violation:

| Violation | Severity | Trigger | Deduction |
|-----------|----------|---------|-----------|
| `MULTIPLE_DEEP_WORK` | CRITICAL | deepWork > 1 | −25 |
| `TOTAL_OVERLOAD` | CRITICAL | activeIssues > 7 | −25 |
| `EXCESSIVE_CONTEXT_SWITCHING` | CRITICAL | touchedToday > 5 | −25 |
| `QUICK_WIN_OVERLOAD` | WARNING | quickWins > 3 | −10 |
| `MAINTENANCE_OVERLOAD` | WARNING | maintenance > 3 | −10 |
| `UNCLEAR_QUICK_WINS` | WARNING | quick-wins with no body | −10 |
| `NO_DEEP_WORK` | INFO | deepWork = 0, has issues | −5 |
| `DEFERRED_BACKLOG_GROWING` | INFO | stale deferred > 5 (14d) | −5 |

CRITICAL = actively causes burnout. WARNING = accelerates burnout trajectory. INFO = predicts future burnout.

---

## 7. Emotional Signal Detection (Plutchik Model)

4 of Plutchik's 8 primary emotions, detected from behavioral signals — no self-reporting required:

| Emotion | Observable Signals | Thresholds |
|---------|-------------------|-----------|
| **Frustration** (Anger family) | Context switches, blocked items | touchedToday > 5, any `blocked` label |
| **Exhaustion** (Sadness family) | After-hours activity, sustained chaos | afterHoursIssues > 0, consecutiveHighDays ≥ 2 |
| **Overwhelm** (Surprise family) | Too many critical items, no priorities | deepWork > 1, totalAssigned > 10 |
| **Anxiety** (Fear family) | Stale urgent items, mystery meat | unresolvedUrgent > 0, mysteryMeat > 0 |

**AI response principles:** Validate without patronizing. Concrete suggestions only. Brevity (stressed people have reduced reading comprehension). No guilt or shame. One actionable item.

---

## 8. Protective Intervention System

Circuit breaker that activates when signals cross safety thresholds. **Any one** triggers protection:

| Trigger | Threshold |
|---------|-----------|
| Sustained stress | consecutiveHighDays ≥ 2 |
| Boundary erosion | hasAfterHoursActivity() |
| Acute overload | stressScore ≥ 70 |
| Cognitive capacity | totalAssigned > 10 |

**Fallback messages** (when LLM unavailable):

| Condition | Message |
|-----------|---------|
| ≥ 3 consecutive high days | "You've had elevated stress for N days. Consider taking a short break or delegating." |
| After-hours activity | "I noticed after-hours activity — try to protect your personal time." |
| Stress ≥ 70 | "Your stress level is high. Defer one non-critical item." |
| Heavy workload | "Your workload is heavy today. Sustainable pace > heroic effort." |
| No trigger | "You're doing well! Keep up the balanced approach. 💪" |

---

## 9. Friday Deploy Confidence

Score 0–100, based on the empirical observation that Friday deploys carry higher risk due to reduced recovery time.

| Condition | Deduction |
|-----------|-----------|
| chaos > 5 | −20 |
| chaos > 8 | −20 (cumulative: −40) |
| !isCompliant | −15 |
| urgentUnassigned > 0 | −15 |
| afterHoursSignal | −10 |
| mysteryMeatCount > 3 | −10 |

**Readiness:** ≥ 80 READY 🟢, 50–79 CAUTION 🟡, < 50 NOT_READY 🔴.

Addresses **optimism bias** (underestimating risk when tired) and **completion bias** (urge to "just finish it" before the weekend).

---

## 10. Calendar Fragmentation

Measures how fragmented a developer's day is. Key threshold:

**Deep work feasibility:** `largestFreeBlock ≥ 90 minutes` (23 min ramp-up + 60 min flow + 7 min buffer).

If no 90-minute contiguous block exists → `calendarBlocked = true` in WorldState → deep work item deferred.

---

## 11. AI Agent Architecture

LangChain4j **Supervisor Pattern** — deterministic services calculate all metrics first; AI agents only explain and act.

### Flow

1. **Sync** — GitHub issues → `IssueCache` (ConcurrentHashMap)
2. **Calculate** — `ChaosMetricsService`, `ComplianceService`, `IssueClassifierService` run independently
3. **Build WorldState** — 18 capped variables from issue data + metrics
4. **Invoke Supervisor** — planner model coordinates 5 sub-agents autonomously
5. **Accumulate Mutations** — sub-agents invoke `@Tool` methods → `pendingActions` list
6. **Return Response** — explanation, mutation plan, stress scores, protective messages

### 5 Sub-Agents

| Agent | Role | Tools |
|-------|------|-------|
| **DeferAgent** | Move non-critical work to next sprint | `deferIssue()` |
| **DelegateAgent** | Redistribute across team | `delegateIssue()` |
| **ClassifyAgent** | Organize into 3-3-3 | `markAsDeepWork()`, `classifyAsQuickWin()`, `classifyAsMaintenance()` |
| **ScopeAgent** | Flag unclear issues | `addScopeNeeded()` |
| **WellnessAgent** | Recommend self-care | `suggestBreak()`, `slowIntake()`, `blockCalendarTime()` |

### 3 AI Personas

| Service | Persona | Purpose |
|---------|---------|---------|
| **ExplainerAiService** | Supportive productivity coach | Explain the *why* behind the plan |
| **ProtectiveAiService** | Protective AI companion (Plutchik) | Emotional support and self-care |
| **FridayDeployAiService** | Calm release engineer | Deploy risk assessment |

### Dual Model Architecture

| Model | Role |
|-------|------|
| **plannerModel** | Supervisor — decides which sub-agents to invoke and in what order |
| **chatModel** | Sub-agents — execute tools and explain actions |

Both default to the Azure OpenAI deployment (`gpt-4o` configurable). Stress reduction estimate: each mutation reduces stress by **7 points** (heuristic).

---

## 12. Flamegraph Visualization Psychology

The flamegraph is a **stress communication tool** — fire metaphors trigger the brain's threat detection system.

| Design Element | Psychology |
|---------------|-----------|
| Fire = danger | Primal association → immediate emotional response |
| Height = depth | Taller bars feel "heavier" |
| Color = urgency | Red/amber/green maps to traffic light intuition |
| Width = proportion | Wider bars communicate relative impact |

### Per-Issue Stress

`stress = baseStress + (complexity × 3) + (globalStress × 0.3) + labelBonuses`

| Category | Base | Label Bonuses |
|----------|------|--------------|
| Deep Work | 60 | `urgent`/`critical`/`blocker`: +20 |
| Maintenance | 30 | `bug`: +10 |
| Quick Wins | 20 | |
| Deferred | 10 | |

**Global stress leak** (`× 0.3`): high-stress environments make *every* task feel harder.

**Color thresholds:** < 35% green 🟢, 35–64% amber 🟡, ≥ 65% red 🔴.

---

## 13. Priority Weighting & Day Plan

### Sort Key (multi-level)

1. Priority weight ascending: `priority:critical` → 0, `priority:high`/`urgent` → 1, default → 2
2. `updatedAt` descending (most recent first, nulls last)
3. Issue number ascending (stable tiebreaker)

### Day Plan Assembly

Each bucket fills its quota from the sorted list; overflow → Deferred:
- **Deep Work:** top 1 → today, remaining → deferred
- **Quick Wins:** top 3 → today, remaining → deferred
- **Maintenance:** top 3 → today, remaining → deferred

---

## 14. Graceful Degradation

Core principle: **every AI feature must work without AI**.

| Level | State | Behavior |
|-------|-------|----------|
| 0 | Full LLM available | AI explanations, emotional support, supervisor active |
| 1 | LLM call fails | Catch exception → fallback response; all metrics still accurate |
| 2 | LLM not configured | `llmEnabled = false` → full deterministic operation with pre-written messages |
| 3 | Backend partially available | Health returns UP, individual endpoints degrade independently |

### Demo Label System

| Label | Effect | Bypasses |
|-------|--------|----------|
| `demo:touched-today` | Appears recently updated | Real timestamps |
| `demo:after-hours` | Triggers after-hours signal | Real clock |
| `demo:stale-14d` | Appears stale (14+ days) | Real age |
| `demo:friday` | Forces Friday context | Real day-of-week |

**Golden Rule:** If *any* `demo:*` label exists, real timestamps are never consulted.

---

## 15. Complete Constants Reference

### Time & Scheduling

| Constant | Value | Source |
|----------|-------|--------|
| Deep work minimum block | 90 min | CalendarService |
| After-hours (Chaos) | 8am / 6pm | ChaosMetricsService |
| After-hours (WorldState) | 9am / 6pm | WorldState |
| Chaos "recent" window | 60 min | ChaosMetricsService |
| Urgent unresolved threshold | 24h | ChaosMetricsService |
| Stale issue age | 14 days | ComplianceService, WorldState |
| Demo rate limit | 5 min per repo | DemoFlamegraphController |
| GitHub fetch limit | 100 issues | DemoFlamegraphController |

### 3-3-3 Limits

| Constant | Value |
|----------|-------|
| MAX_DEEP_WORK | 1 |
| MAX_QUICK_WINS | 3 |
| MAX_MAINTENANCE | 3 |
| MAX_ACTIVE | 7 |

### Stress Scoring

| Component | Threshold | Penalty | Cap |
|-----------|-----------|---------|-----|
| Over-assignment | > 7 issues | 4 pts/issue | 20 |
| Extra deep-work | > 1 item | 10 pts/item | — |
| No deep-work | 0 items | 5 pts | — |
| Workload total | — | — | 40 |
| Context-switch | > 5 issues | 3 pts/issue | 15 |
| Mystery meat | per issue | 2 pts | 10 |
| Unclear quick-win | per issue | 1 pt | 5 |
| Sustained stress | per day | 5 pts | 15 |
| After-hours | per issue | 5 pts | 10 |

### Stress Levels

| Level | Backend (WorldState) | Frontend (flamegraph) |
|-------|---------------------|----------------------|
| LOW | < 30 | < 35 |
| MODERATE | 30–49 | 35–64 |
| HIGH | 50–69 | ≥ 65 |
| CRITICAL | ≥ 70 | — |

### Chaos Scoring

| Signal trigger | Value | Bucket thresholds |
|---------------|-------|-------------------|
| Each signal weight | 2 pts | LOW ≤ 2 |
| Score cap | 10 | MEDIUM ≤ 5 |
| Mystery meat trigger | ≥ 3 | HIGH ≤ 8 |
| Unresolved urgent | ≥ 3 | CRITICAL > 8 |
| Touched today | ≥ 6 | |
| Label explosion | ≥ 12 | |

### Friday Deploy

| Condition | Deduction | Readiness |
|-----------|-----------|-----------|
| chaos > 5 | −20 | ≥ 80: READY 🟢 |
| chaos > 8 | −20 | 50–79: CAUTION 🟡 |
| !isCompliant | −15 | < 50: NOT_READY 🔴 |
| urgentUnassigned > 0 | −15 | |
| afterHoursSignal | −10 | |
| mysteryMeat > 3 | −10 | |

### Agent System

| Constant | Value |
|----------|-------|
| Supervisor max invocations | 10 |
| Stress reduction per action | 7 pts |
| Protection: consecutive days | ≥ 2 |
| Protection: stress score | ≥ 70 |
| Protection: total assigned | > 10 |

### Flamegraph (Frontend)

| Constant | Value |
|----------|-------|
| Deep Work base stress | 60 |
| Quick Wins base stress | 20 |
| Maintenance base stress | 30 |
| Deferred base stress | 10 |
| Complexity multiplier | 3 per point |
| Global stress leak | 30% (× 0.3) |
| Urgency bonus | +20 |
| Bug bonus | +10 |

---

## References

- Maslach, C., & Leiter, M. P. (2016). *Understanding the burnout experience*. World Psychiatry.
- Sweller, J. (1988). *Cognitive load during problem solving*. Cognitive Science.
- Csikszentmihalyi, M. (1990). *Flow: The psychology of optimal experience*.
- Newport, C. (2016). *Deep Work: Rules for focused success in a distracted world*.
- Plutchik, R. (2001). *The nature of emotions*. American Scientist.
- Mark, G., Gudith, D., & Klocke, U. (2008). *The cost of interrupted work*. CHI Conference.
- Sonnentag, S. (2012). *Psychological detachment from work during leisure time*. Current Directions in Psychological Science.
- McEwen, B. S. (1998). *Protective and damaging effects of stress mediators*. NEJM.
- Easterbrook, J. A. (1959). *The effect of emotion on cue utilization*. Psychological Review.
- Miller, G. A. (1956). *The magical number seven, plus or minus two*. Psychological Review.
- Yerkes, R. M., & Dodson, J. D. (1908). *The relation of strength of stimulus to rapidity of habit-formation*.
