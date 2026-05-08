# The Psychology & Science Behind Burnout-as-a-Service

> **Not less work — structured work.** Every algorithm in this system traces back to published research in cognitive psychology, occupational health, and emotional theory. This document walks through all 16 sections: the science behind them, the formulas that implement them, and the constants that tune them.

---

## How to Read This Document

**Want the big picture?** Start with the [System Sequence Diagram](#system-sequence-diagram), [Algorithm Pipeline](#algorithm-pipeline-overview), and [Developer Journey](#a-developers-journey-from-burnout-to-balance) — they tell the full story visually.

**Want to understand the work-design context?** Section [2 (Work Design Context)](#2-work-design-context) explains the broader pressures this system is designed to respond to, and how those pressures show up in the architecture.

**Want to understand the scoring?** Jump to sections [5 (Stress)](#5-stress-score), [6 (Chaos)](#6-chaos-metrics), and [7 (Compliance)](#7-compliance-violations).

**Interested in the human side?** Sections [8 (Emotions)](#8-emotional-detection) and [9 (Protection)](#9-protective-intervention) cover how the system reads developer mood and intervenes.

**Looking up a specific number?** Section [16 (Constants)](#16-constants-reference) has every threshold, cap, and weight in one place.

### The 16 Sections

| | Section | One-line summary |
|---|---------|-----------------|
| 1 | [Theoretical Foundations](#1-theoretical-foundations) | 6 research frameworks behind every design decision |
| 2 | [Work Design Context](#2-work-design-context) | The broader conditions that make unstructured technical work cognitively expensive — and why this system emphasizes structure |
| 3 | [The 3-3-3 Day](#3-the-3-3-3-day) | Cap the day at 7 items: 1 deep + 3 quick + 3 maintenance |
| 4 | [Issue Classification](#4-issue-classification) | Sort issues into 4 buckets by label — no AI needed |
| 5 | [Stress Score](#5-stress-score) | Single 0–100 number capturing developer health |
| 6 | [Chaos Metrics](#6-chaos-metrics) | 5 signals measuring team/process disorder (0–10) |
| 7 | [Compliance Violations](#7-compliance-violations) | 8 violations checking if the 3-3-3 structure holds |
| 8 | [Emotional Detection](#8-emotional-detection) | 4 Plutchik emotions inferred from GitHub behavior |
| 9 | [Protective Intervention](#9-protective-intervention) | Circuit breaker — safety net when thresholds are crossed |
| 10 | [Friday Deploy](#10-friday-deploy) | Is it safe to ship today? (counters optimism bias) |
| 11 | [Calendar Fragmentation](#11-calendar-fragmentation) | No 90-min block? Deep work gets deferred automatically |
| 12 | [AI Agent Architecture](#12-ai-agent-architecture) | Deterministic pre-pass + 6 sub-agents, 10 tools, 3 personas — the Supervisor Pattern |
| 13 | [Flamegraph Psychology](#13-flamegraph-psychology) | Why fire metaphors work + per-issue stress formula |
| 14 | [Priority & Day Plan](#14-priority--day-plan) | How the system picks *which* issues you work on today |
| 15 | [Graceful Degradation](#15-graceful-degradation) | 4 fallback levels — works fully without AI |
| 16 | [Constants Reference](#16-constants-reference) | Every magic number in one lookup table |

---

## System Sequence Diagram

Four phases — **sync**, **analyze**, **reshape** (deterministic pre-pass + AI), and **apply / recalculate / output** — across six actors. Deterministic services do all the measuring; the pre-pass guarantees the chaos drop; AI only rebalances and explains.

<img src="images/sequence-diagram.png" alt="Sequence diagram showing 4 phases across 6 actors (GitHub, MCP App, Backend, Deterministic Services, WorldState, AI Supervisor): 1·Sync (GitHub → MCP → Backend → IssueCache), 2·Analyze (classify + measure → 12 capped variables → Stress 0–100 BEFORE), 3·Reshape (deterministic pre-pass: triageUrgent + defuseChaosInputs, then Supervisor → 6 sub-agents → @Tool mutations), 4·Apply, Recalculate, Output (apply mutations to IssueCache → recalculate stress AFTER → protective check + Friday score → MCP applies labels & comments to GitHub)" width="100%"/>

---

## Algorithm Pipeline Overview

The system flows through 6 stages — from raw GitHub issues to actionable mutations. Every algorithm is deterministic except the AI agents, which always have a fallback path.

<img src="images/algorithm-pipeline.png" alt="Widescreen infographic showing the 6-stage algorithm pipeline: Ingestion, Chaos Metrics, Classification & Compliance, WorldState (12 capped variables → stress 0–100), Reshape (deterministic pre-pass with triageUrgent + defuseChaosInputs, then LangChain4j Supervisor with 6 sub-agents and 10 @Tool methods, max 15 invocations), and Output (apply mutations → recalculate → Friday score → persist) — with formulas, thresholds, and connections between all components" width="100%"/>

**The six stages:**

1. **Ingestion** — GitHub issues are pulled into an in-memory cache as typed Java records.
2. **Classification** — Each issue is sorted into one of 4 buckets (Deep Work → Quick Win → Maintenance → Deferred) using a priority-ordered, first-match label cascade.
3. **Metrics & Compliance** — Chaos score (0–10) and compliance score (100 → 0) are calculated deterministically. No AI involved.
4. **WorldState** — 18 capped variables feed into the stress score (0–100), computed from its weighted components.
5. **AI Agents** — Reshape runs as a deterministic pre-pass (`triageUrgent` + `defuseChaosInputs`) followed by the LangChain4j Supervisor (Azure OpenAI `gpt-4o` by default, SUMMARY strategy, max 15 invocations) coordinating 6 sub-agents with 10 `@Tool` methods, plus 3 support agents for explanation, emotional care, and deploy readiness.
6. **Output** — A mutation plan applied to GitHub via MCP: label changes, comments, a rebalanced 1+3+3 day, and protective messages when needed.

---

## A Developer's Journey: From Burnout to Balance

The story of how the system works — told through a developer named Alex.

### Scene 1: The Breaking Point

Alex stares at their screen. 12 open issues, 3 critical bugs, Slack piling up, and it's 7 PM. No plan, no priorities — just an avalanche of work.

<img src="images/scene1-overwhelmed-developer.png" alt="Overwhelmed developer at desk with chaotic notifications and 12 unorganized issues" width="800"/>

*Starting point: a developer overwhelmed by unstructured work — all three Maslach Burnout Inventory [[1]](#ref-1) dimensions in play (emotional exhaustion, depersonalization, reduced accomplishment).*

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

<img src="images/scene4-stress-score.png" alt="Stress score calculation showing weighted components with formulas and caps" width="800"/>

*Workload, chaos, context switching, clarity, sustained pressure, and after-hours activity are combined into a single capped score that represents the current level of strain.*

### Scene 5: The Supervisor Agent Steps In

Reshape runs in two phases. A **deterministic pre-pass** strips chaos-inducing labels off every unassigned-urgent issue and rewrites empty bodies and after-hours timestamps so the chaos score is guaranteed to drop. Then the LangChain4j **Supervisor Pattern** takes over: a planner LLM coordinates 6 specialized sub-agents to finish the rebalancing.

<img src="images/scene5-supervisor-pattern.png" alt="LangChain4j Supervisor Agent architecture with deterministic pre-pass and 6 sub-agents producing a mutation plan" width="800"/>

*Two-phase reshape: a deterministic pre-pass calls `triageUrgent` for every unassigned-urgent issue and `defuseChaosInputs(clock)` to neutralise mystery-meat bodies and after-hours timestamps, then the supervisor (Azure OpenAI `gpt-4o` by default, SUMMARY strategy, max 15 invocations) coordinates 6 sub-agents — Triage, Defer, Delegate, Classify, Scope, Wellness — each with `@Tool` methods from `BurnoutMutationTool`. Output: a mutation plan of label changes, comments, unassignments, body rewrites, and timestamp normalisations.*

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

This system is built on six published research frameworks. Each one maps directly to a specific algorithm or design decision in the codebase.

<img src="images/theoretical-foundations.png" alt="Six theoretical foundations: Maslach Burnout Inventory, Cognitive Load Theory, Yerkes-Dodson Law, Deep Work, Pomodoro/Time Boxing, and Plutchik's Wheel of Emotions — each mapped to system algorithms" width="800"/>

| Framework | Key Concept | How We Use It |
|-----------|-------------|---------------|
| **Maslach Burnout Inventory** [[1]](#ref-1) | 3 dimensions: exhaustion, depersonalization, reduced accomplishment | After-hours → exhaustion; mystery meat → depersonalization; no deep work → reduced accomplishment |
| **Cognitive Load Theory** [[2]](#ref-2) | Working memory is limited; extraneous load must be minimized | 3-3-3 cap at 7 items; classification removes ambiguity load |
| **Yerkes-Dodson Law** [[11]](#ref-11) | Performance peaks at moderate stress, drops at extremes | Stress score targets 30–49 (MODERATE) as optimal zone |
| **Deep Work** [[4]](#ref-4) | Sustained focused work requires 90+ min uninterrupted blocks | Calendar fragmentation check; exactly 1 deep work item per day |
| **Pomodoro / Time Boxing** | Short focused intervals with breaks maintain energy | Quick wins as natural break-points between deep work sessions |
| **Plutchik's Wheel** [[5]](#ref-5) | 8 primary emotions with behavioral signatures | 4 emotions detected from GitHub signals (frustration, exhaustion, overwhelm, anxiety) |

## 2. Work Design Context

Burnout in technical work rarely comes from a single dramatic event. More often, it builds through the accumulation of ambiguity, interruption, reactive priorities, and the mismatch between human cognitive limits and the way modern work is often organized. Developers are asked to shift rapidly between planning and execution, respond to urgent work while preserving quality, and maintain focus in environments filled with context switching, unclear scope, and social as well as technical demands. This system is designed around that broader reality: not by claiming to diagnose burnout, but by reducing the conditions that commonly make work feel scattered, heavy, and harder to recover from over time.

The design choice throughout this project is therefore simple: create more structure where work tends to become noisy. The 3-3-3 day limits active cognitive load, deterministic classification reduces ambiguity, chaos and compliance metrics make hidden process problems visible, calendar checks protect conditions for deep work, and protective interventions encourage sustainable pacing before overload becomes normalized. The goal is not to eliminate hard work, but to make demanding work more legible, bounded, and humane.

In that sense, Burnout-as-a-Service should be read as a work-reshaping system rather than a mood-reading system. Its algorithms focus on observable patterns in task structure, issue quality, context switching, urgency, and after-hours behavior. These are practical signals that teams can act on without over-claiming what the system knows. The architecture favors measurable workload design, operational clarity, and recoverability — because structured work is easier to sustain than constant reactivity.

### Work Design → System Mapping

| Work Design Pressure | What the System Measures | Where It Responds |
|----------------------|--------------------------|-------------------|
| Too much active work at once | Assigned load, day-structure overflow, compliance violations | 3-3-3 planning, deferred overflow, workload scoring |
| Reactive and noisy execution | Chaos score, touched-today spikes, stale urgents | ChaosMetricsService, Friday deploy checks, supervisor reshaping |
| Ambiguous or underspecified work | Mystery meat count, unclear quick wins, issue scope gaps | ScopeAgent, compliance checks, clarity scoring |
| Loss of protected focus time | Calendar fragmentation, deep-work feasibility | CalendarService, deep-work slot protection |
| Sustained pressure over time | Consecutive high-stress days, after-hours activity | Stress score, protective intervention, supportive messaging |

### Cognitive Accessibility Alignment

The work-design pressures above overlap substantially with the conditions that inclusive-design and cognitive-accessibility guidance (WCAG 2.2 [[12]](#ref-12), W3C COGA Task Force [[13]](#ref-13)) treat as barriers — working-memory limits, unpredictable state, fragile error recovery, and inflexible pacing. Although this system was designed for burnout rather than for any specific cognitive profile, its algorithms end up expressing the same four principles that cognitive-accessibility work tends to emphasize. The mapping below is descriptive, not a conformance claim.

| Cognitive accessibility principle | How the algorithm expresses it |
|-----------------------------------|--------------------------------|
| **Reduce cognitive load** | 3-3-3 day caps active work at 7 items (Miller); 4-bucket classification chunks work; Clarity dimension penalizes mystery meat and unclear scope; label-explosion signal flags taxonomy overhead. |
| **Predictable & consistent** | All metrics are deterministic (same input → same output); priority-ordered first-match classification is explainable; `breakdownHints` surface *why* each number is what it is; day shape is the same every day. |
| **Error-resistant & forgiving** | Four graceful-degradation levels keep the system working without AI; deferred work is rescheduled, not lost; mutations are plans, not force-pushes; protective intervention acts as a safety net at stress ≥ 70. |
| **Pace control & personalization** | Calendar fragmentation auto-defers deep work rather than forcing it; after-hours detection is timezone-aware; `WellnessAgent` can slow intake and block calendar time; responses stay short to match narrowed attention under load. |

The system does not diagnose, label, or accommodate any specific condition. It reshapes the shared environment — task structure, clarity, pacing, recoverability — in ways that tend to reduce friction for developers working under stress, fatigue, or other temporary or persistent cognitive load. Where this framing stops short is UI-level sensory and motor accessibility (contrast, keyboard navigation, reduced motion, ARIA semantics); those are not currently part of the algorithm or the static pages.

---

## 3. The 3-3-3 Day

Instead of an unbounded task list, the system caps each day at exactly **7 items** across three attention types. This comes from Miller's Law [[10]](#ref-10) (working memory holds 7 ± 2 items) and flow-state research [[3]](#ref-3) (deep work needs singular focus). Everything beyond 7 is automatically deferred — not lost, just scheduled for later.

| Slot | Count | Purpose | Psychology |
|------|-------|---------|-----------|
| Deep Work | 1 | Cognitively demanding task | Flow state requires singular focus [[3]](#ref-3) |
| Quick Wins | 3 | Small, completable tasks | Dopamine from completion; momentum builders |
| Maintenance | 3 | Routine upkeep | Low cognitive overhead; batch-processable |
| **Total** | **7** | | **Miller's Law [[10]](#ref-10): 7 ± 2 working memory limit** |

Overflow beyond 7 active items → automatically deferred. Deep work gets a protected 90-minute block.

## 4. Issue Classification

Before the system can structure a day, it needs to understand what kind of work each issue represents. [IssueClassifierService.java](../backend/src/main/java/com/demo/burnout/service/IssueClassifierService.java) examines GitHub labels and assigns every issue to one of four [Classification](../backend/src/main/java/com/demo/burnout/model/Classification.java) buckets — deterministic, priority-ordered, no LLM involved. Each issue lands in the **first** matching bucket:

| Priority | Bucket | Label Triggers |
|----------|--------|---------------|
| 1st | DEEP_WORK | `priority:critical`, `architecture`, `security`, `deep-work`, `performance`, `RFC` |
| 2nd | QUICK_WIN | `good-first-issue`, `quick-win`, `size:S`, `typo`, `chore`, `CSS` |
| 3rd | MAINTENANCE | `dependencies`, `documentation`, `maintenance`, `tech-debt`, `refactor` |
| 4th | DEFERRED | Everything else (no matching labels) |

**Hour estimation** uses label-based lookup: `security`→8h, `architecture`→6h, `deep-work`→4h, `performance`→4h, `refactor`→3h, `documentation`→2h, `chore`→1h, default→2h.

**Clear scope detection** looks for: checkboxes (`- [ ]`), "acceptance criteria", "expected"/"actual", numbered steps, or "definition of done".

## 5. Stress Score

The system's central metric — a single number (0–100) that captures how much pressure a developer is under right now, grounded in Yerkes-Dodson's [[11]](#ref-11) inverted-U model (too little stress = disengaged, too much = breakdown). [WorldState.java](../backend/src/main/java/com/demo/burnout/model/WorldState.java) holds the capped variables and runs `calculateStressScore()` to produce it; [StressLevel.java](../backend/src/main/java/com/demo/burnout/model/StressLevel.java) defines the thresholds that drive protective interventions, the supervisor agent's priorities, and the flamegraph visualization. Calculated from 6 dimensions:

| Dimension | Typical Max | Formula |
|-----------|-------------|---------|
| **Workload** | ~25 | `min(20, (assigned − 7) × 4)` if > 7 issues, + `(deepWork − 1) × 10` if > 1, + `5` if deepWork = 0 |
| **Chaos** | 30 | `chaosBucket.ordinal() × 10` (LOW=0, MEDIUM=10, HIGH=20, CRITICAL=30) |
| **Context Switching** [[6]](#ref-6) | 15 | `min(15, (touchedToday − 5) × 3)` if > 5 |
| **Clarity** | 15 | `min(10, mysteryMeat × 2)` + `min(5, unclearQuickWins)` |
| **Sustained** [[8]](#ref-8) | 15 | `min(15, consecutiveHighDays × 5)` |
| **After-Hours** | 10 | `min(10, afterHoursIssues × 5)` |

Only the overall score is capped (at 100). The `(deepWork − 1) × 10` term has no inner cap, but deep-work counts stay small in practice, so workload rarely exceeds ~25.

**Stress levels:** ≥ 70 CRITICAL, ≥ 50 HIGH, ≥ 30 MODERATE, < 30 LOW.

## 6. Chaos Metrics

While the stress score measures the individual, the chaos score (0–10) measures the *environment* — problems with the team's process. [ChaosMetricsService.java](../backend/src/main/java/com/demo/burnout/service/ChaosMetricsService.java) evaluates five binary signals, each worth 2 points, and packs the result into a [ChaosMetrics](../backend/src/main/java/com/demo/burnout/model/ChaosMetrics.java) record (score, bucket, and individual signal flags). Each reveals a different kind of dysfunction:

| Signal | Trigger | What It Reveals |
|--------|---------|-----------------|
| Mystery meat | ≥ 3 issues with blank body or no assignees | Team not investing in issue quality |
| Unresolved urgent | ≥ 3 urgent items > 24h old | Broken priority system |
| Issues touched today | ≥ 6 updated in 60 min | Reactive firefighting [[6]](#ref-6) |
| After-hours | Any update outside 9am–6pm or weekend | Boundary erosion [[7]](#ref-7) |
| Label explosion | ≥ 12 distinct labels | Taxonomy chaos → cognitive overhead [[2]](#ref-2) |

**Chaos buckets:** ≤ 2 LOW, ≤ 5 MEDIUM, ≤ 8 HIGH, > 8 CRITICAL.

## 7. Compliance Violations

Is the 3-3-3 structure actually being followed? [ComplianceService.java](../backend/src/main/java/com/demo/burnout/service/ComplianceService.java) audits the day structure — the compliance score starts at 100 and drops for each [ViolationType](../backend/src/main/java/com/demo/burnout/model/ViolationType.java), from critical issues like multiple deep-work items down to informational warnings like a growing backlog:

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

## 8. Emotional Detection

Developers don't fill out mood surveys — but their GitHub activity tells a story. [AgentOrchestrator.java](../backend/src/main/java/com/demo/burnout/agent/AgentOrchestrator.java) detects four emotions from observable signals — context-switch frequency, after-hours commits, stale urgent issues, and workload size — mapping each to a Plutchik [[5]](#ref-5) primary family. Under stress, attention narrows [[9]](#ref-9), so the system keeps responses brief and actionable:

| Emotion | Observable Signals | Thresholds |
|---------|-------------------|-----------|
| **Frustration** (Anger family) | Context switches, blocked items | touchedToday > 5, any `blocked` label |
| **Exhaustion** (Sadness family) | After-hours activity, sustained chaos | afterHoursIssues > 0, consecutiveHighDays ≥ 2 [[1]](#ref-1) [[7]](#ref-7) |
| **Overwhelm** (Surprise family) | Too many critical items, no priorities | deepWork > 1, totalAssigned > 10 |
| **Anxiety** (Fear family) | Stale urgent items, mystery meat | unresolvedUrgent > 0, mysteryMeat > 0 |

**AI response principles:** Validate without patronizing. Concrete suggestions only. Brevity — Easterbrook [[9]](#ref-9) showed stressed people have narrowed attention, so short messages land better. No guilt or shame. One actionable item.

## 9. Protective Intervention

The system's safety net, informed by McEwen's [[8]](#ref-8) research on allostatic load — sustained stress causes cumulative physiological damage, so early intervention matters. When **any single** signal crosses a critical threshold, [ProtectiveAiService.java](../backend/src/main/java/com/demo/burnout/agent/ProtectiveAiService.java) — a LangChain4j `@AiService` with a Plutchik-informed system prompt — generates an empathetic intervention. If the LLM is unavailable, pre-written fallbacks ensure protection never silently fails.

Protection is triggered when **any** of these conditions holds:

| Trigger | Threshold |
|---------|-----------|
| Sustained stress | consecutiveHighDays ≥ 2 |
| Boundary erosion [[7]](#ref-7) | hasAfterHoursActivity() |
| Acute overload | stressScore ≥ 70 |
| Cognitive capacity | totalAssigned > 10 |

Note: protection fires at **2** consecutive high days, but the fallback message that names the day count only appears at **3+** — at 2 days, the after-hours or stress-score messages below are shown instead.

**Fallback messages** (when LLM unavailable):

| Condition | Message |
|-----------|---------|
| ≥ 3 consecutive high days | "You've had elevated stress for N days. Consider taking a short break or delegating." |
| After-hours activity | "I noticed after-hours activity — try to protect your personal time." |
| Stress ≥ 70 | "Your stress level is high. Defer one non-critical item." |
| Heavy workload | "Your workload is heavy today. Sustainable pace > heroic effort." |
| No trigger | "You're doing well! Keep up the balanced approach. 💪" |

## 10. Friday Deploy

Should you deploy on Friday? The system uses a **Friday deploy readiness** score (0–100) to make that decision legible, and [FridayDeployAiService.java](../backend/src/main/java/com/demo/burnout/agent/FridayDeployAiService.java) turns that score into a human-readable recommendation. In other words, the service explains the readiness state rather than acting as the source of truth for the calculation itself. The value of this step is less about prediction and more about discipline: it helps counter **optimism bias** (“it’ll be fine”) and **completion bias** (“just ship it before the weekend”) by making operational risk explicit.

A lower readiness score reflects accumulating delivery risk signals such as high chaos, structural non-compliance, unassigned urgent work, after-hours activity, and poor issue quality.

| Condition | Deduction |
|-----------|-----------|
| chaos > 5 | −20 |
| chaos > 8 | −20 (cumulative: −40) |
| !isCompliant | −15 |
| urgentUnassigned > 0 | −15 |
| afterHoursSignal | −10 |
| mysteryMeatCount > 3 | −10 |

**Readiness bands:** ≥ 80 READY 🟢, 50–79 CAUTION 🟡, < 50 NOT_READY 🔴.

## 11. Calendar Fragmentation

Deep work requires sustained focus [[4]](#ref-4) — but a day full of meetings makes that impossible. Context-switching has a 23-minute recovery cost [[6]](#ref-6), so [CalendarService.java](../backend/src/main/java/com/demo/burnout/service/CalendarService.java) scans the day for a contiguous **90-minute** block (23 min ramp-up + 60 min flow + 7 min buffer). No block? Deep work gets automatically deferred rather than setting the developer up for a frustrating, interrupted attempt.

`largestFreeBlock ≥ 90 min` → deep work feasible. Otherwise → `calendarBlocked = true` → deep work deferred.

## 12. AI Agent Architecture

Deterministic services calculate all metrics first. AI agents only explain, recommend, and act — they never make the initial measurements. This is the LangChain4j **Supervisor Pattern** in action: [AgentOrchestrator.java](../backend/src/main/java/com/demo/burnout/agent/AgentOrchestrator.java) coordinates the full pipeline from metrics through supervisor to protective response, [BurnoutAgents.java](../backend/src/main/java/com/demo/burnout/agent/supervisor/BurnoutAgents.java) declares the six sub-agent `@Agent` interfaces (Triage, Defer, Delegate, Classify, Scope, Wellness), and [AgentConfiguration.java](../backend/src/main/java/com/demo/burnout/config/AgentConfiguration.java) wires the Spring beans for both LLM models and all agent instances.

### Flow

1. **Sync** — GitHub issues → [`IssueCache`](../backend/src/main/java/com/demo/burnout/service/IssueCache.java) (ConcurrentHashMap)
2. **Calculate** — `ChaosMetricsService`, `ComplianceService`, `IssueClassifierService` run independently
3. **Build WorldState** — capped variables from issue data + metrics
4. **Deterministic Pre-pass** — [`BurnoutSupervisorService`](../backend/src/main/java/com/demo/burnout/agent/supervisor/BurnoutSupervisorService.java) calls `mutationTool.triageUrgent(n)` for every unassigned-urgent issue, then `mutationTool.defuseChaosInputs(clock)` to neutralise empty bodies and after-hours / recently-touched timestamps. **Always runs**, even when the LLM is dummy/down — so the chaos score drops in every fallback path. Counts surface on `SupervisorResult` as `deterministicTriageCount` and `deterministicDefuseCount`, and the explanation is prepended with a "🧹 Deterministic pre-pass:" line listing the triaged issue numbers.
5. **Invoke Supervisor** — planner model coordinates 6 sub-agents autonomously (max 15 invocations); the supervisor is told the unassigned-urgent issues are already triaged and should be left alone, AND is **prompt-blocked from quoting absolute stress numbers** in its summary (the prompt explains that the system computes the AFTER score itself)
6. **Accumulate Mutations** — sub-agents invoke `@Tool` methods → [`pendingActions`](../backend/src/main/java/com/demo/burnout/agent/supervisor/BurnoutMutationTool.java) list (six action types: AddLabels, RemoveLabels, Comment, Unassign, SetBody, SetUpdatedAt)
7. **Apply + Recalculate** — controller applies the mutation plan to the in-memory cache and recomputes `afterScore` from the mutated state
8. **Deterministic 1-3-3-0 enforcer** *(`/demo/api/reshape` only)* — [`enforce333Compliance`](../backend/src/main/java/com/demo/burnout/controller/DemoFlamegraphController.java) promotes deferred-classified items into underfilled quickWin/maintenance slots, then defers true overflow off the user's plate (unassign + `deferred,next-sprint` + comment). Surfaces as `complianceActionCount` (0 when the LLM lands compliance on its own).
9. **Compose explanation** — controllers append a "📈 Outcome:" footer with the real measured `beforeScore → afterScore` drop and per-phase action counts. The prose surfaced to the user is bookended by deterministic content, so even if the LLM regresses the footer is the source of truth.
10. **Return Response** — explanation, [mutation plan](../backend/src/main/java/com/demo/burnout/goap/GitHubMutationPlan.java), stress scores, deterministic counts, protective messages

### 6 Sub-Agents

| Agent | Role | Tools |
|-------|------|-------|
| **TriageAgent** | Strip chaos-inducing urgent flags from unassigned issues | `triageUrgent()` |
| **DeferAgent** | Move non-critical work to next sprint | `deferIssue()` |
| **DelegateAgent** | Redistribute across team | `delegateIssue()` |
| **ClassifyAgent** | Organize into 3-3-3 | `markAsDeepWork()`, `classifyAsQuickWin()`, `classifyAsMaintenance()` |
| **ScopeAgent** | Flag unclear issues | `addScopeNeeded()` |
| **WellnessAgent** | Recommend self-care (only when stress ≥ 50, i.e. HIGH or CRITICAL) | `suggestBreak()`, `slowIntake()`, `blockCalendarTime()` |

`triageUrgent()` is also invoked directly from the deterministic pre-pass, so the chaos signal disappears even if the LLM never calls TriageAgent. `defuseChaosInputs(Clock)` is a non-`@Tool` method on `BurnoutMutationTool` that the supervisor service runs before the LLM — it emits `SetBody` and `SetUpdatedAt` actions to kill the mystery-meat, after-hours, and touched-today factors. The supervisor prompt only routes work to `WellnessAgent` when stress ≥ 50, so MODERATE/LOW reshapes stay focused on classification and deferral and don't burn LLM invocations on advisory wellness comments. Because the three wellness tools return advisory strings (no `GitHubAction`s), `BurnoutMutationTool` increments a `wellnessInvocationCount` on every call and surfaces it on `SupervisorResult` and the reshape API responses — that counter is the only way to verify the gating actually fired.

### 3 AI Personas

| Service | Persona | Purpose |
|---------|---------|---------|
| [**ExplainerAiService**](../backend/src/main/java/com/demo/burnout/agent/ExplainerAiService.java) | Supportive productivity coach | Explain the *why* behind the plan |
| **ProtectiveAiService** | Protective AI companion (Plutchik) | Emotional support and self-care |
| **FridayDeployAiService** | Calm release engineer | Deploy risk assessment |

### Dual Model Architecture

| Model | Role |
|-------|------|
| **plannerModel** | Supervisor — decides which sub-agents to invoke and in what order |
| **chatModel** | Sub-agents — execute tools and explain actions |

Both default to the Azure OpenAI deployment (`gpt-4o` configurable). Stress reduction estimate: each mutation reduces stress by **7 points** (heuristic).

## 13. Flamegraph Psychology

The flamegraph isn't just a chart — it's designed to exploit how the brain processes threats. [flamegraph.html](../backend/src/main/resources/static/flamegraph.html) renders the interactive visualization as a standalone page, computing per-issue heat and applying green/amber/red color mapping. Fire metaphors activate threat detection. Height conveys cognitive weight. Color maps to the universal traffic-light instinct.

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

## 14. Priority & Day Plan

Once issues are classified, they still need ranking. [IssueClassifierService.java](../backend/src/main/java/com/demo/burnout/service/IssueClassifierService.java) applies a multi-level sort key to decide *which* issues fill today's 3-3-3 slots, packing the result into a [DayStructure](../backend/src/main/java/com/demo/burnout/model/DayStructure.java) record; the rest overflow to Deferred.

### Sort Key (multi-level)

1. Priority weight ascending: `priority:critical` → 0, `priority:high`/`urgent` → 1, default → 2
2. `updatedAt` descending (most recent first, nulls last)
3. Issue number ascending (stable tiebreaker)

### Day Plan Assembly

Each bucket fills its quota from the sorted list; overflow → Deferred:
- **Deep Work:** top 1 → today, remaining → deferred
- **Quick Wins:** top 3 → today, remaining → deferred
- **Maintenance:** top 3 → today, remaining → deferred

## 15. Graceful Degradation

A burnout tool that crashes when you're stressed would *increase* burnout. So every AI feature works without AI — [AgentConfiguration.java](../backend/src/main/java/com/demo/burnout/config/AgentConfiguration.java) detects whether Azure OpenAI credentials are real or dummy and sets the `llmEnabled` flag, letting all metrics run fully deterministic. For live demos, [SyntheticTimeResolver.java](../backend/src/main/java/com/demo/burnout/util/SyntheticTimeResolver.java) lets the `demo:*` labels defined in [DemoLabels.java](../backend/src/main/java/com/demo/burnout/util/DemoLabels.java) override the real clock so demos work any day of the week. The AI layers add explanation and mutation planning, but the system is complete without them.

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

## 16. Constants Reference

If you're reading the code and wondering "why that number?", this section explains every threshold in plain language.

### How long does deep work need?

[CalendarService.java](../backend/src/main/java/com/demo/burnout/service/CalendarService.java) won't schedule deep work unless your calendar has a free block of at least **90 minutes** — that's 23 minutes to ramp up (based on Mark's context-switching research), a full hour of flow, and a 7-minute buffer. Anything shorter and you'd just be getting started when the next meeting hits.

### What counts as "after hours"?

[ChaosMetricsService.java](../backend/src/main/java/com/demo/burnout/service/ChaosMetricsService.java) flags activity outside **9am–6pm** (or weekends) as a chaos signal. [WorldState.java](../backend/src/main/java/com/demo/burnout/model/WorldState.java) uses the same **9am–6pm** window for protective interventions. The checkin endpoint accepts an optional `tz` parameter (IANA timezone, e.g. `America/New_York`) so after-hours detection uses the user's local time. If no timezone is provided, the server's configured `demo.clock.zone` is used (default: `Africa/Johannesburg`).

### When is something "stale" or "urgent"?

An issue becomes stale after **14 days** without updates — that's when [ComplianceService.java](../backend/src/main/java/com/demo/burnout/service/ComplianceService.java) triggers the deferred-backlog-growing violation. [ChaosMetricsService.java](../backend/src/main/java/com/demo/burnout/service/ChaosMetricsService.java) flags urgent items unresolved past **24 hours** and counts anything updated in the last **60 minutes** as “touched today” for context-switching detection.

### The 3-3-3 shape

One deep-work item, three quick wins, three maintenance tasks — [IssueClassifierService.java](../backend/src/main/java/com/demo/burnout/service/IssueClassifierService.java) enforces this daily quota and overflows the rest to deferred. No more than seven active issues total. These aren't arbitrary: one deep-work item protects focus, three of each lighter category gives variety without overwhelm, and the seven-item cap aligns with Miller's cognitive limit.

### How stress adds up

Each of the six stress dimensions has a cap so no single problem can blow up the score on its own — [WorldState.java](../backend/src/main/java/com/demo/burnout/model/WorldState.java)'s `calculateStressScore()` sums them into a single 0–100 number. **Workload** dominates (capped at 40) — it ramps up fast once you exceed seven assigned issues and penalizes both too many deep-work items and having none at all. **Chaos** can contribute up to 30 points, mapping directly from the environment score. **Context-switching** and **clarity** each cap at 15 — the system notices when you're juggling too many issues or when issues lack clear descriptions. **Sustained stress** adds up over consecutive bad days (capped at 15), and **after-hours activity** caps at 10.

Below 30 is LOW (you're fine). 30–49 is MODERATE (watch it). 50–69 is HIGH (take action). 70 or above is CRITICAL (the system intervenes). The flamegraph uses slightly softer thresholds for its color coding — green below 35%, amber up to 65%, red above that.

### How chaos adds up

Five binary signals, each worth two points, capping at 10 — [ChaosMetricsService.java](../backend/src/main/java/com/demo/burnout/service/ChaosMetricsService.java) asks: are there too many empty issues? Are urgent items being ignored? Is the team context-switching like crazy? Is anyone working after hours? Has the label taxonomy exploded? Two or fewer points is LOW. Five or fewer is MEDIUM. Eight or fewer is HIGH. Above that is CRITICAL.

### Friday deploy scoring

The Friday deploy readiness score is intentionally conservative: it starts from an ideal state and drops as operational risk signals accumulate. High chaos has the biggest impact, while non-compliance, unassigned urgent work, after-hours activity, and poor issue quality further reduce confidence. [FridayDeployAiService.java](../backend/src/main/java/com/demo/burnout/agent/FridayDeployAiService.java) uses that score to generate a calm release recommendation in plain language, making the decision easier to communicate and harder to rationalize emotionally.

Above 80 you're generally in a good position to ship 🟢. Between 50 and 79, caution is warranted 🟡. Below 50, the system treats the situation as too risky for an end-of-week deploy 🔴.

### Agent guardrails

[BurnoutSupervisorService.java](../backend/src/main/java/com/demo/burnout/agent/supervisor/BurnoutSupervisorService.java) caps the supervisor at **15 invocations** per reshape — enough to reorganize a messy day without running forever. Each action the agents take (defer, delegate, classify, triage) is estimated to reduce stress by about **7 points**, a heuristic that keeps the system from over-intervening. Before the LLM is invoked at all, a deterministic pre-pass calls `triageUrgent` for every unassigned-urgent issue and runs `defuseChaosInputs(clock)` to rewrite mystery-meat bodies and after-hours timestamps — so the chaos score drops regardless of what the planner decides. [AgentOrchestrator.java](../backend/src/main/java/com/demo/burnout/agent/AgentOrchestrator.java) triggers protective interventions after **two consecutive high-stress days**, when stress hits **70**, or when someone has more than **10 assigned issues**.

### Flamegraph heat

[flamegraph.html](../backend/src/main/resources/static/flamegraph.html) assigns a base “heat” to each category — deep work runs hottest because it demands the most cognitive investment, followed by maintenance, quick wins, and deferred items at the coolest. Complexity multiplies on top, and high global stress bleeds into every issue (at 30%), making even small tasks look hotter when the environment is chaotic. Urgent or critical labels add a significant heat bonus; bugs add a smaller one.

---

## References

<a id="ref-1"></a>**[1]** Maslach, C., & Leiter, M. P. (2016). *Understanding the burnout experience*. World Psychiatry.

<a id="ref-2"></a>**[2]** Sweller, J. (1988). *Cognitive load during problem solving*. Cognitive Science.

<a id="ref-3"></a>**[3]** Csikszentmihalyi, M. (1990). *Flow: The psychology of optimal experience*.

<a id="ref-4"></a>**[4]** Newport, C. (2016). *Deep Work: Rules for focused success in a distracted world*.

<a id="ref-5"></a>**[5]** Plutchik, R. (2001). *The nature of emotions*. American Scientist.

<a id="ref-6"></a>**[6]** Mark, G., Gudith, D., & Klocke, U. (2008). *The cost of interrupted work: More speed and stress*. CHI Conference.

<a id="ref-7"></a>**[7]** Sonnentag, S. (2012). *Psychological detachment from work during leisure time*. Current Directions in Psychological Science.

<a id="ref-8"></a>**[8]** McEwen, B. S. (1998). *Protective and damaging effects of stress mediators*. NEJM.

<a id="ref-9"></a>**[9]** Easterbrook, J. A. (1959). *The effect of emotion on cue utilization and the organization of behavior*. Psychological Review.

<a id="ref-10"></a>**[10]** Miller, G. A. (1956). *The magical number seven, plus or minus two*. Psychological Review.

<a id="ref-11"></a>**[11]** Yerkes, R. M., & Dodson, J. D. (1908). *The relation of strength of stimulus to rapidity of habit-formation*.

<a id="ref-12"></a>**[12]** W3C (2023). *Web Content Accessibility Guidelines (WCAG) 2.2*. https://www.w3.org/TR/WCAG22/

<a id="ref-13"></a>**[13]** W3C Cognitive and Learning Disabilities Accessibility Task Force (COGA) (2021). *Making Content Usable for People with Cognitive and Learning Disabilities*. https://www.w3.org/TR/coga-usable/