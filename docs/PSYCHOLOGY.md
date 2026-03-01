# The Psychology & Science Behind Burnout-as-a-Service

This document provides an exhaustive reference for every burnout analysis idea, psychological model, and algorithm implemented in the system. Each section maps research-backed concepts to their concrete implementation.

---

## Table of Contents

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
10. [Calendar Fragmentation Analysis](#10-calendar-fragmentation-analysis)
11. [AI Agent Architecture](#11-ai-agent-architecture)
12. [Flamegraph Visualization Psychology](#12-flamegraph-visualization-psychology)
13. [Priority Weighting & Day Plan Construction](#13-priority-weighting--day-plan-construction)
14. [Graceful Degradation Philosophy](#14-graceful-degradation-philosophy)
15. [Complete Constants Reference](#15-complete-constants-reference)

---

## A Developer's Journey: From Burnout to Balance

Before diving into the technical details, here's the story of how the system works — told through the experience of a developer named Alex.

---

### Scene 1: The Breaking Point

Alex stares at their screen. 12 open issues, 3 critical bugs, Slack notifications piling up, and it's already 7 PM. There's no plan, no priorities — just an avalanche of work that feels impossible to manage. This is where most developers hit the wall.

<img src="images/scene1-overwhelmed-developer.png" alt="Overwhelmed developer at desk with chaotic notifications and 12 unorganized issues" width="800"/>

*This illustration shows the starting point: a developer overwhelmed by unstructured work, unclear priorities, and boundary erosion — the classic conditions that lead to burnout.*

**What's happening psychologically:** Alex is experiencing all three dimensions of the Maslach Burnout Inventory — emotional exhaustion (working late), depersonalization (issues blur together), and reduced accomplishment (nothing feels "done"). The cognitive load from 12 context switches has depleted working memory.

---

### Scene 2: The 3-3-3 Structure

The system's first intervention: impose structure. Based on Cognitive Load Theory, the 3-3-3 rule limits the day to **1 deep work item, 3 quick wins, and 3 maintenance tasks** — matching the brain's capacity for different types of attention.

<img src="images/scene2-333-day-structure.png" alt="3-3-3 Day Structure showing 1 deep work, 3 quick wins, 3 maintenance tasks with compliance rules" width="800"/>

*The 3-3-3 day structure: one cognitively demanding deep work item (blue), three quick momentum-builders (green), and three routine maintenance tasks (amber). The compliance rules panel shows the exact limits — max 7 active issues per day, with overflow routed to the Deferred bucket.*

**Why it works:** By capping active work to 7 items (1+3+3), the system stays within Miller's 7±2 limit for working memory. Deep work gets a protected 90-minute block. Quick wins provide dopamine hits. Maintenance is routine and low-stress.

---

### Scene 3: Classifying the Chaos

Before the AI even runs, deterministic services classify every GitHub issue into one of four categories based on its labels. This is pure pattern matching — no LLM required.

<img src="images/scene3-issue-classification.png" alt="Issue Classification Pipeline showing priority-ordered cascade from labels to four buckets" width="800"/>

*The classification pipeline: GitHub labels enter from the left, and a priority-ordered cascade sorts each issue into the first matching bucket — Deep Work → Quick Win → Maintenance → Deferred. Each bucket lists the exact labels and criteria from `IssueClassifierService.classify()`.*

**The key insight:** Classification is deterministic, not AI-driven. Labels like `priority:critical` always map to DEEP_WORK. Labels like `good-first-issue` always map to QUICK_WIN. This ensures the system is predictable and auditable — the AI only explains and enhances, never decides.

---

### Scene 4: Measuring the Stress

The system builds a **WorldState** from 18 discrete variables extracted from GitHub issue fields, then calculates a stress score (0–100) by summing weighted components.

<img src="images/scene4-stress-score.png" alt="Stress score calculation showing 8 graduated components with formulas and caps feeding into a 0-100 thermometer" width="800"/>

*The stress score formula from `WorldState.calculateStressScore()`: 8 graduated factors — workload, deep work imbalance, no-deep-work penalty, chaos bucket, context switching, clarity tax, sustained stress, and after-hours activity — each with a specific formula and cap, summed to a total capped at 100.*

**Alex's score: 72/100 (CRITICAL).** The breakdown reveals the pain: 12 assigned issues contribute 20 points of workload stress, HIGH chaos adds 20 more, 8 context switches add 9, 4 mystery-meat issues add 8, and after-hours work adds 15. The system now has a precise, explainable number for what Alex feels intuitively.

---

### Scene 5: The Supervisor Agent Steps In

With the WorldState calculated, the LangChain4j **Supervisor Pattern** takes over. A planner LLM receives the full context and autonomously coordinates 5 specialized sub-agents to rebalance the workload.

<img src="images/scene5-supervisor-pattern.png" alt="LangChain4j Supervisor Agent architecture showing planner model, WorldState input, 5 sub-agents with descriptions and tool methods, and mutation plan output" width="800"/>

*The LangChain4j Supervisor pattern: the planner model (gpt-4o, SUMMARY strategy, max 10 invocations) receives 18 WorldState variables and coordinates 5 sub-agents — each with a specific role description and `@Tool` methods from `BurnoutMutationTool`. **Defer** protects by deferring non-critical issues; **Delegate** balances team workload; **Classify** organizes into 3-3-3 structure (1 deep work, max 3 quick wins, max 3 maintenance); **Scope** flags mystery-meat issues lacking clear "done" criteria; **Wellness** recommends stress reduction based on score and after-hours signals. The output is a mutation plan of label additions, removals, and comments.*

**What the Supervisor decides for Alex:**
1. **DeferAgent** — defers 3 non-critical issues to next sprint (reduces active set from 12 to 9)
2. **ClassifyAgent** — reclassifies 2 excess quick wins as maintenance to achieve 3-3-3
3. **ScopeAgent** — flags 4 mystery-meat issues as `needs-scope`
4. **WellnessAgent** — suggests a break (stress ≥ 70 threshold)

The Supervisor builds a **mutation plan** of 8 GitHub actions — but doesn't execute them yet.

---

### Scene 6: The Flamegraph — Seeing Stress

The flamegraph transforms abstract numbers into a visceral visualization. Each issue becomes a colored bar — red (danger), amber (caution), or green (calm). The primal association of fire = danger creates an immediate emotional response that raw numbers can't match.

Here's the landing page where a developer enters their GitHub repo:

<img src="images/flamegraph-landing.png" alt="Burnout Flamegraph landing page with repo input and Sync from GitHub button" width="800"/>

*The flamegraph landing page: enter a public repo, click Sync from GitHub, and the system fetches all open issues for analysis.*

And here's what Alex sees — 12 issues with **no labels**, all dumped into a single "Deferred" pile. Deep Work: 0. Quick Wins: 0. Maintenance: 0. No structure at all:

<img src="images/flamegraph-before.png" alt="Flamegraph showing 12 unclassified issues all in Deferred with stress score 25" width="800"/>

*The BEFORE state: all 12 issues land in Deferred because none have classification labels. Stress score: 25/100. The empty Deep Work, Quick Wins, and Maintenance sections visually scream "no plan."*

**The psychology:** Fire metaphors work because they trigger the brain's threat detection system. A dashboard that says "Stress: 25" is abstract. A screen showing zero structured work and everything deferred is visceral. The flamegraph makes the case for change without needing a word of explanation.

---

### Scene 7: Reshaping the Day

The `reshape_day` tool applies the mutation plan to Alex's GitHub issues. Labels are added, comments posted, issues deferred. The before-and-after tells the story: chaos becomes structure.

<img src="images/flamegraph-after.png" alt="Flamegraph after reshaping showing 1 Deep Work, 3 Quick Wins, 3 Maintenance, 5 Deferred with stress score 20" width="800"/>

*The AFTER state: the same 12 issues, now structured into a 3-3-3 plan. 1 Deep Work (Refactor auth module at 86%), 3 Quick Wins (CSS fix, lodash update, README typo), 3 Maintenance (API docs, Spring Boot upgrade, CORS headers), and 5 Deferred. Stress score: 20/100.*

**What the Supervisor did:** The agent classified each issue and applied labels — `deep-work` for the auth refactor, `quick-win` for small fixes, `maintenance` for routine tasks, and `deferred` for everything that can wait. The flamegraph now shows a clear visual hierarchy: green at the top (deep work), warm colors for quick wins and maintenance, and cool purple for deferred items.

**The contrast:** Compare this screenshot to the BEFORE image above. Same 12 issues, same deadlines — but now the developer knows exactly what to work on first, and the flamegraph visually confirms the plan is balanced.

---

### Scene 8: The Balanced Developer

After reshaping, Alex's world looks different. One focused deep work item. Three satisfying quick wins. Three predictable maintenance tasks. Everything else is deferred. The day has structure.

**What changed:** The same 12 issues still exist. The same deadlines haven't moved. But the cognitive load is managed, priorities are explicit, and the developer knows exactly what to do next. That's the difference between burnout and balance — not less work, but structured work.

| Metric | Before (No Labels) | After (Reshaped) |
|--------|-------------------|------------------|
| Deep Work | 0 | 1 |
| Quick Wins | 0 | 3 |
| Maintenance | 0 | 3 |
| Deferred | 12 (all) | 5 (intentional) |
| Stress Score | 25/100 | 20/100 |
| 3-3-3 Compliant | No | Yes |

---

## 1. Theoretical Foundations

The system draws on several established psychological and productivity frameworks:

### Maslach Burnout Inventory (MBI)
The gold standard for measuring occupational burnout identifies three dimensions:
- **Emotional exhaustion** — feeling drained by work → mapped to our *after-hours signals* and *sustained stress* metrics
- **Depersonalization** — cynical detachment → mapped to *mystery meat issues* (issues with no description suggest disengagement)
- **Reduced personal accomplishment** — feeling ineffective → mapped to *stale backlog* and *deferred pile-up*

### Cognitive Load Theory (Sweller, 1988)
Working memory is finite. Context switching imposes a measurable cognitive tax:
- Each additional active task fragment increases *extraneous cognitive load*
- The system measures this via `issuesTouchedToday` (issues updated within 8 hours)
- Threshold: more than 5 context switches triggers a CRITICAL violation

### Yerkes-Dodson Law
Performance peaks at moderate stress and declines at both extremes:
- **Too low** (no deep work) → boredom, drift → the system flags `NO_DEEP_WORK`
- **Optimal** (3-3-3 compliant) → flow state accessible → 🟢 LOW stress
- **Too high** (overloaded) → impaired judgment, errors → 🔴 CRITICAL stress

### Cal Newport's Deep Work
Sustained, distraction-free concentration produces the highest-value output:
- The system enforces **exactly 1 deep work item per day** (not 0, not 2+)
- Calendar fragmentation checks require a **90-minute contiguous block** for deep work
- Multiple deep work items trigger `MULTIPLE_DEEP_WORK` (CRITICAL severity)

### Pomodoro / Time-Boxing Principles
Quick wins and maintenance tasks use bounded time slots:
- Quick wins: estimated < 30 minutes each → 3 per day = ~90 minutes total
- Maintenance: routine tasks → 3 per day to prevent accumulation
- Total active work: 1 + 3 + 3 = 7 items maximum

### Plutchik's Wheel of Emotions
Robert Plutchik's psychoevolutionary theory of emotion provides the emotional classification model:
- 8 primary emotions arranged in opposing pairs
- The system monitors 4 burnout-relevant emotions: **Frustration**, **Exhaustion**, **Overwhelm**, **Anxiety**
- Each maps to observable signals in GitHub issue patterns (see [Section 7](#7-emotional-signal-detection-plutchik-model))

---

## 2. The 3-3-3 Day Structure

The core scheduling philosophy, inspired by Oliver Burkeman's time management research and adapted for software engineering:

```
┌──────────────────────────────────────────┐
│            THE 3-3-3 DAY                 │
├──────────────────────────────────────────┤
│  🎯  1 × DEEP WORK                      │
│      Architecture, security, features    │
│      Requires 90+ min focus block        │
│                                          │
│  ⚡  3 × QUICK WINS                      │
│      Small fixes, reviews, responses     │
│      < 30 min each                       │
│                                          │
│  🔧  3 × MAINTENANCE                     │
│      Dependencies, docs, CI/CD, triage   │
│      Routine but necessary               │
│                                          │
│  📋  Everything else → DEFERRED          │
│      Next sprint, backlog, someday       │
└──────────────────────────────────────────┘

MAX ACTIVE = 1 + 3 + 3 = 7 items
```

### Why These Numbers?

| Slot | Count | Rationale |
|------|-------|-----------|
| Deep Work | 1 | Cognitive capacity for sustained focus is limited; multiple deep tasks fragment attention |
| Quick Wins | 3 | Provides momentum and dopamine hits; keeps stakeholders unblocked |
| Maintenance | 3 | Prevents tech debt accumulation without consuming the whole day |
| Total | 7 | Aligns with Miller's Law (7 ± 2 items in working memory) |

### Implementation

```java
// ComplianceService.java
MAX_DEEP_WORK  = 1
MAX_QUICK_WINS = 3
MAX_MAINTENANCE = 3
MAX_ACTIVE     = 7   // 1 + 3 + 3

// Compliance check
compliant = deepWorkCount <= 1
         && quickWinCount <= 3
         && maintenanceCount <= 3
```

---

## 3. Issue Classification System

Every GitHub issue is deterministically classified into one of four categories. The classifier uses a priority-ordered decision tree — the first matching rule wins.

### Classification Enum

```
DEEP_WORK → QUICK_WIN → MAINTENANCE → DEFERRED (catch-all)
```

### Decision Tree

```
                    ┌─────────────┐
                    │  New Issue   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐     Yes
                    │ Deep Work?  ├──────────► DEEP_WORK
                    └──────┬──────┘
                           │ No
                    ┌──────▼──────┐     Yes
                    │ Quick Win?  ├──────────► QUICK_WIN
                    └──────┬──────┘
                           │ No
                    ┌──────▼──────┐     Yes
                    │ Maintenance?├──────────► MAINTENANCE
                    └──────┬──────┘
                           │ No
                           ▼
                       DEFERRED
```

### Deep Work Detection

An issue is deep work if **any** of these conditions hold:

| Condition | Rationale |
|-----------|-----------|
| Label: `priority:critical`, `priority:high`, `architecture`, `security`, `deep-work` | Explicitly marked as high-impact |
| Estimated hours > 2 | Large tasks require sustained concentration |
| Label pattern: `epic.*` or `feature.*` | Epics and features are multi-session efforts |

### Quick Win Detection

An issue is a quick win if it has a non-blank body **and any** of:

| Condition | Rationale |
|-----------|-----------|
| Label: `good-first-issue`, `quick-win`, `low-hanging-fruit`, `trivial` | Explicitly marked as fast |
| Estimated hours < 0.5 **and** clear scope detected | Small + well-defined = completable in one sitting |
| Label: `enhancement` **and** body < 500 chars | Brief enhancements are usually quick |

### Maintenance Detection

| Label | What It Represents |
|-------|-------------------|
| `dependencies` | Dependency updates (Dependabot, Renovate) |
| `documentation` | README, docs, comments |
| `triage` | Issue grooming and sorting |
| `chore` | Housekeeping tasks |
| `refactor` | Code restructuring |
| `tech-debt` | Accumulated shortcuts to address |
| `ci`, `devops` | Pipeline and infrastructure tasks |
| `maintenance` | General upkeep |

### Hour Estimation Algorithm

When no explicit estimate label exists, the system infers effort from issue body length — a proxy for complexity and scope:

| Condition | Hours | Psychology |
|-----------|-------|-----------|
| Label `estimate:Xh` | X | Explicit human estimate (highest trust) |
| Label `size:s` or `small` | 0.5 | T-shirt sizing |
| Label `size:m` or `medium` | 2.0 | T-shirt sizing |
| Label `size:l` or `large` | 4.0 | T-shirt sizing |
| Label `size:xl` | 8.0 | T-shirt sizing |
| No labels, body < 100 chars | 0.5 | Short description → small task |
| No labels, body < 500 chars | 2.0 | Medium description → medium task |
| No labels, body ≥ 500 chars | 4.0 | Long description → complex task |
| No labels, no body | 2.0 | Unknown → default to medium |

### Clear Scope Detection

An issue has "clear scope" if the body contains **any** of:
- `- [ ]` — task checklist
- `acceptance criteria` — explicit completion criteria
- `done when` — definition of done
- `steps:` — numbered procedure
- `expected:` — expected behavior / outcome

**Psychology:** Issues without clear scope cause *ambiguity stress* — the developer doesn't know when they're "done". The system penalizes these as `unclearQuickWins` or `mysteryMeat` (no body at all).

---

## 4. Stress Score Algorithm

The stress score (0–100) is the system's central metric. It quantifies developer cognitive load using six additive dimensions, each grounded in occupational psychology research.

### Formula

```
stress = workload + chaos + contextSwitching + clarity + sustained + afterHours
stress = min(100, stress)
```

### Dimension Breakdown

#### 4.1 Workload Stress (max 40 points)

Measures raw assignment volume against cognitive capacity:

```java
int workload = 0;

// Over-assignment: each issue beyond 7 adds 4 points
if (totalAssigned > 7)
    workload += min(20, (totalAssigned - 7) * 4);

// Multiple deep work: parallelizing deep work fragments focus
if (deepWorkCount > 1)
    workload += (deepWorkCount - 1) * 10;

// No deep work: lack of meaningful work causes drift
if (deepWorkCount == 0 && totalAssigned > 0)
    workload += 5;

workload = min(40, workload);
```

| Scenario | Points | Why It Matters |
|----------|--------|----------------|
| 8 issues assigned (1 over) | +4 | Each extra item competes for attention |
| 12 issues assigned (5 over) | +20 (capped) | At 5+ over, you're drowning |
| 3 deep work items | +20 | Two extra deep items = two extra context switches per day |
| 0 deep work, but issues open | +5 | "Busy but unproductive" — Sisyphean treadmill |

#### 4.2 Chaos (max 30 points)

Environmental disorder signal derived from the Chaos Metrics service:

```java
chaos = chaosBucket.ordinalValue * 10
```

| ChaosBucket | Ordinal | Points | Score Range |
|-------------|---------|--------|-------------|
| LOW | 0 | 0 | ≤ 2.0 |
| MEDIUM | 1 | 10 | ≤ 5.0 |
| HIGH | 2 | 20 | ≤ 8.0 |
| CRITICAL | 3 | 30 | > 8.0 |

#### 4.3 Context Switching (max 15 points)

Rapid task-hopping taxes working memory. Every switch incurs a *resumption lag* of 10–25 minutes (Mark, Gonzalez & Harris, 2005):

```java
if (issuesTouchedToday > 5)
    contextSwitching = min(15, (issuesTouchedToday - 5) * 3);
```

| Issues Touched | Points | Impact |
|----------------|--------|--------|
| ≤ 5 | 0 | Normal multi-tasking |
| 6 | 3 | Slightly scattered |
| 8 | 9 | Attention fragments |
| 10+ | 15 (capped) | Near-constant context switching |

#### 4.4 Clarity (max 15 points)

Ambiguity creates decision fatigue and *analysis paralysis*:

```java
clarity = min(10, mysteryMeatCount * 2)     // issues with blank body
        + min(5, unclearQuickWins);          // quick wins lacking scope
```

| Signal | Points Each | Psychology |
|--------|-------------|-----------|
| Mystery meat issue (no body) | 2 | "What even is this?" — forces developer to investigate |
| Unclear quick win (quick-win label + no body) | 1 | Quick wins should be *immediately* actionable |

#### 4.5 Sustained Stress (max 15 points)

Chronic stress is more dangerous than acute. The allostatic load model (McEwen, 1998) shows that sustained elevated cortisol causes burnout even at "manageable" daily levels:

```java
sustained = min(15, consecutiveHighChaosDays * 5);
```

| Consecutive High Days | Points | Clinical Analogy |
|-----------------------|--------|-----------------|
| 0 | 0 | No accumulation |
| 1 | 5 | "Tough day" |
| 2 | 10 | "Rough week" — intervention triggered |
| 3+ | 15 (capped) | Burnout risk — protective response activates |

#### 4.6 After-Hours Activity (max 10 points)

Work-life boundary violations predict burnout with high reliability (Sonnentag, 2012):

```java
afterHours = min(10, issuesUpdatedAfterHours * 5);
```

After-hours is defined as: **before 9 AM, after 6 PM, or weekends**

| Issues Updated After Hours | Points | Signal |
|----------------------------|--------|--------|
| 0 | 0 | Healthy boundaries |
| 1 | 5 | Occasional late push |
| 2+ | 10 (capped) | Pattern of overwork |

### Stress Level Classification

```
score ≥ 70 → CRITICAL  🔴  "Burnout imminent — immediate intervention"
score ≥ 50 → HIGH      🟠  "Unsustainable — needs rebalancing"  
score ≥ 30 → MODERATE  🟡  "Elevated — monitor closely"
score <  30 → LOW       🟢  "Healthy — sustainable pace"
```

### Worked Example

**Developer with 10 issues, 2 deep work, chaos HIGH, 7 issues touched today, 3 mystery meat, 2 consecutive high days, 1 after-hours update:**

| Dimension | Calculation | Points |
|-----------|-------------|--------|
| Workload (over-assigned) | min(20, (10 - 7) × 4) = 12 | 12 |
| Workload (extra deep work) | (2 - 1) × 10 = 10 | 10 |
| Chaos | HIGH ordinal (2) × 10 | 20 |
| Context switching | min(15, (7 - 5) × 3) = 6 | 6 |
| Clarity (mystery meat) | min(10, 3 × 2) = 6 | 6 |
| Sustained | min(15, 2 × 5) = 10 | 10 |
| After hours | min(10, 1 × 5) = 5 | 5 |
| **Total** | min(100, 12+10+20+6+6+10+5) | **69** 🟠 HIGH |

One more point and this developer enters CRITICAL territory.

---

## 5. Chaos Metrics

Chaos is an environmental quality — it measures disorder in the *system* rather than in the individual. Based on entropy concepts from information theory applied to project management.

### Chaos Score (0–10)

Five binary signals, each worth 2 points:

```
score = 0
if mysteryMeatCount ≥ 3:       score += 2   → "Nobody describes their issues"
if unresolvedUrgent ≥ 3:        score += 2   → "Urgent items ignored for 24h+"
if issuesTouchedToday ≥ 6:      score += 2   → "Everything's on fire"
if afterHoursSignal == true:    score += 2   → "Someone's working at midnight"
if distinctLabelCount ≥ 12:     score += 2   → "Label taxonomy explosion"
score = min(10, score)
```

### ChaosMetrics Record

```java
record ChaosMetrics(
    long issuesTouchedRecently,   // updated within 60 minutes
    long unresolvedUrgent,       // label "urgent" + created >24h ago
    int  distinctLabelCount,     // unique label names across all issues
    boolean afterHoursSignal,    // any update before 8am, after 6pm, or weekend
    int  mysteryMeatCount,       // issues with blank body OR no assignees
    double score                 // 0–10 composite
)
```

### Signal Interpretation

| Signal | Score ≥ 2 Trigger | What It Reveals |
|--------|-------------------|-----------------|
| Mystery meat | 3+ issues with no body | Team not investing in issue quality → decision fatigue downstream |
| Unresolved urgent | 3+ urgent items > 24h old | Broken priority system → everything's "urgent" so nothing is |
| Issues touched today | 6+ updated in 60 min | Reactive firefighting → no space for proactive work |
| After-hours | Any update outside 8am–6pm or weekend | Boundary erosion → recovery time deficit |
| Label explosion | 12+ distinct labels | Taxonomy chaos → cognitive overhead classifying work |

### Contradictory Labels

```java
contradictoryLabels() → distinctLabelCount > 10 ? distinctLabelCount - 10 : 0
```

When a single issue carries both `bug` and `enhancement`, it signals unclear problem definition. The system counts issues matching this pattern in WorldState.

---

## 6. Compliance Violation Detection

The compliance engine monitors 8 violation types organized by severity. Each violation type maps to a specific anti-pattern observed in developer workload management.

### Violation Types

| Violation | Severity | Trigger | What's Wrong | Recommendation |
|-----------|----------|---------|--------------|----------------|
| `MULTIPLE_DEEP_WORK` | 🔴 CRITICAL | deepWork > 1 | Parallel deep work destroys flow state | "Pick ONE. Move others to next sprint." |
| `TOTAL_OVERLOAD` | 🔴 CRITICAL | activeIssues > 7 | Beyond working memory capacity | "Defer N issues to protect focus." |
| `EXCESSIVE_CONTEXT_SWITCHING` | 🔴 CRITICAL | touchedToday > 5 (8h) | Constant switching = zero deep progress | "Focus on completing one issue first." |
| `QUICK_WIN_OVERLOAD` | 🟡 WARNING | quickWins > 3 | Quick win addiction — feels productive but isn't | "Defer N quick wins to tomorrow." |
| `MAINTENANCE_OVERLOAD` | 🟡 WARNING | maintenance > 3 | Yak-shaving displaces real work | "Batch for a dedicated maintenance day." |
| `UNCLEAR_QUICK_WINS` | 🟡 WARNING | any quick-wins with no body | "Quick wins" that require investigation aren't quick | "Add scope or reclassify as deferred." |
| `NO_DEEP_WORK` | ℹ️ INFO | deepWork = 0, has issues | Treading water — busy but no meaningful progress | "Identify one priority:critical issue." |
| `DEFERRED_BACKLOG_GROWING` | ℹ️ INFO | stale deferred > 5 (14d) | "Later" became "never" | "Schedule backlog grooming." |

### Compliance Score Formula

```
score = 100
for each violation:
    CRITICAL → score -= 25
    WARNING  → score -= 10
    INFO     → score -= 5
score = max(0, score)
```

### Psychology: Severity Levels

- **CRITICAL** = Violations that *actively cause* burnout (overwork, fragmentation). These directly impair performance today.
- **WARNING** = Violations that *accelerate* burnout trajectory. Sustainable for a day, destructive over a week.
- **INFO** = Violations that *predict* future burnout if uncorrected. Early warning signals.

---

## 7. Emotional Signal Detection (Plutchik Model)

The system uses Robert Plutchik's psychoevolutionary model to classify emotional states from observable behavioral signals — no self-reporting required.

### Monitored Emotions (4 of Plutchik's 8 Primary Emotions)

```
        ┌─────────────────────────────────────────────┐
        │        PLUTCHIK'S WHEEL (subset)            │
        ├───────────────┬─────────────────────────────┤
        │  FRUSTRATION  │  Rapid context switching,   │
        │   (Anger      │  many blocked items, stuck  │
        │    family)    │  on same issue for days     │
        ├───────────────┼─────────────────────────────┤
        │  EXHAUSTION   │  After-hours activity,      │
        │   (Sadness    │  sustained high chaos,      │
        │    family)    │  multi-day elevated stress   │
        ├───────────────┼─────────────────────────────┤
        │  OVERWHELM    │  Too many critical items,   │
        │   (Surprise   │  no clear priorities, label  │
        │    family)    │  taxonomy explosion          │
        ├───────────────┼─────────────────────────────┤
        │  ANXIETY      │  Approaching deadlines      │
        │   (Fear       │  without progress, stale    │
        │    family)    │  urgent items, 24h+ stuck   │
        └───────────────┴─────────────────────────────┘
```

### Behavioral Mapping

| Emotion | Observable Signal | Data Source | Threshold |
|---------|------------------|-------------|-----------|
| Frustration | Context switches per day | `issuesTouchedToday` | > 5 |
| Frustration | Blocked item count | Issues with `blocked` label | > 0 |
| Exhaustion | After-hours updates | `issuesUpdatedAfterHours` | > 0 |
| Exhaustion | Consecutive bad days | `consecutiveHighChaosDays` | ≥ 2 |
| Overwhelm | Active deep work items | `deepWorkCount` | > 1 |
| Overwhelm | Total assignments | `totalAssigned` | > 10 |
| Anxiety | Stale urgent items | `unresolvedUrgent` | > 0 |
| Anxiety | Mystery meat issues | `mysteryMeatCount` | > 0 |

### AI Response Principles

The protective AI follows these evidence-based communication principles:

1. **Validate without patronizing** — "I can see things are intense right now" not "you poor thing"
2. **Concrete suggestions only** — "Defer issue #42" not "take it easy"
3. **Brevity** — stressed people have reduced reading comprehension (Stress narrows attentional focus — Easterbrook, 1959)
4. **No guilt or shame** — focus on self-care, never on failure
5. **One actionable item** — decision fatigue is real; one clear action > a menu of options

---

## 8. Protective Intervention System

The protection system operates as a circuit breaker — it activates when signals cross safety thresholds and provides immediate support.

### Trigger Conditions (OR logic — any one triggers)

```java
shouldTriggerProtection =
    consecutiveHighDays >= 2          // sustained stress
    || hasAfterHoursActivity()        // boundary erosion
    || calculateStressScore() >= 70   // acute overload
    || totalAssigned() > 10           // beyond cognitive capacity
```

### Response Generation

```
┌────────────────────┐    ┌─────────────────────┐
│  Check Triggers    │───►│  LLM Available?     │
└────────────────────┘    └──────────┬──────────┘
                              │           │
                           Yes│           │No
                              ▼           ▼
                    ┌────────────┐  ┌────────────────┐
                    │ AI Response│  │ Fallback Rules  │
                    │ (Plutchik  │  │ (Deterministic) │
                    │  model)    │  │                 │
                    └────────────┘  └────────────────┘
```

### Fallback Messages (when LLM is unavailable)

| Condition | Message |
|-----------|---------|
| consecutiveHighDays ≥ 3 | "You've had elevated stress for N days. Consider taking a short break or delegating." |
| After-hours activity | "I noticed after-hours activity — try to protect your personal time." |
| Stress ≥ 70 | "Your stress level is high. Defer one non-critical item." |
| Default (heavy workload) | "Your workload is heavy today. Sustainable pace > heroic effort." |
| No trigger | "You're doing well! Keep up the balanced approach. 💪" |

---

## 9. Friday Deploy Confidence

Based on the empirical observation that Friday deploys carry higher risk due to reduced recovery time (no on-call coverage over weekends in many teams).

### Friday Score Formula (0–100)

```
score = 100
if chaos.score > 5:           score -= 20    // environmental disorder
if chaos.score > 8:           score -= 20    // cumulative: -40; near-critical chaos
if !compliance.isCompliant:   score -= 15    // not 3-3-3 → poor focus hygiene
if urgentUnassigned > 0:      score -= 15    // unowned urgent = ticking bomb
if chaos.afterHoursSignal:    score -= 10    // team already stretched
if mysteryMeatCount > 3:      score -= 10    // unclear items = hidden risk
score = max(0, score)
```

### Deploy Readiness Classification

| Score | Status | Emoji | Guidance |
|-------|--------|-------|----------|
| ≥ 80 | READY | 🟢 | "Deploy with confidence — controlled environment" |
| 50–79 | CAUTION | 🟡 | "Proceed with caution — address specific concerns first" |
| < 50 | NOT_READY | 🔴 | "Defer to Monday — multiple risk factors compound over the weekend" |

### Psychological Basis

The Friday deploy check addresses two cognitive biases:
1. **Optimism bias** — developers underestimate deployment risk when tired
2. **Completion bias** — the urge to "just finish it" before the weekend overrides caution

The score provides an objective counterweight to subjective "it'll be fine" feelings.

---

## 10. Calendar Fragmentation Analysis

Measures how fragmented a developer's day is — many short meetings create "Swiss cheese" schedules that eliminate deep work time.

### Fragmentation Metrics

```java
record CalendarFragmentation(
    int meetingsToday,          // e.g., 3
    int totalMeetingMinutes,    // e.g., 45
    int largestFreeBlock,       // e.g., 120 minutes
    int contextSwitches,        // e.g., 2
    double fragmentationScore   // 0.0 = contiguous, 1.0 = maximally fragmented
)
```

### Deep Work Feasibility Threshold

```java
isDeepWorkPossible() → largestFreeBlock >= 90 minutes
```

**Why 90 minutes?** Research on flow states (Csikszentmihalyi, 1990) shows:
- It takes ~23 minutes to reach flow state after an interruption (Mark, Gudith & Klocke, 2008)
- Productive deep work requires sustaining flow for at least 60 minutes
- 23 min ramp-up + 60 min productive + 7 min buffer ≈ **90 minutes minimum block**

If the calendar doesn't have a 90-minute contiguous free block, the system flags `calendarBlocked = true` in WorldState and recommends rescheduling or deferring the deep work item.

---

## 11. AI Agent Architecture

The system uses LangChain4j's **Supervisor Pattern** (`langchain4j-agentic`) — a hierarchical multi-agent system where a planner LLM coordinates specialized sub-agents. The key design principle is: **deterministic services calculate all metrics first; AI agents only explain and act — they never make decisions.**

### End-to-End Flow

The Supervisor pattern is invoked via two API endpoints (`/api/reshape` and `/api/stress`). The flow is:

```
  1. Issues synced         2. Deterministic metrics        3. WorldState built
  ┌──────────────┐        ┌──────────────────────┐        ┌──────────────────┐
  │ GitHub → MCP │───────▶│ ChaosMetricsService  │───────▶│ WorldState.from( │
  │ → IssueCache │        │ ComplianceService    │        │   issues, chaos, │
  │              │        │ IssueClassifierService│        │   compliance)    │
  └──────────────┘        └──────────────────────┘        └────────┬─────────┘
                                                                   │
  6. Response returned     5. Mutations accumulated        4. Supervisor invoked
  ┌──────────────────┐    ┌──────────────────────┐        ┌────────▼─────────┐
  │ ReshapeResponse{ │◀───│ BurnoutMutationTool  │◀───────│ preventBurnout(  │
  │   explanation,   │    │ .getMutationPlan()   │        │   state, issues, │
  │   mutationPlan,  │    │                      │        │   userId, repo,  │
  │   stressScores } │    └──────────────────────┘        │   chaos)         │
  └──────────────────┘                                    └──────────────────┘
```

**Step by step:**
1. **Sync** — Issues fetched from GitHub via the MCP `sync_issues` tool and cached in `IssueCache` (in-memory `ConcurrentHashMap`)
2. **Calculate** — Three deterministic services run independently: `ChaosMetricsService` (chaos score 0–10), `ComplianceService` (compliance report), `IssueClassifierService` (DEEP_WORK / QUICK_WIN / MAINTENANCE / DEFERRED)
3. **Build WorldState** — `WorldState.from(issues, userId, chaos, compliance, clock)` produces 18 discrete variables (see table below) from the raw issue data and metric outputs
4. **Invoke Supervisor** — `BurnoutSupervisorService.preventBurnout()` builds 5 sub-agents with the `BurnoutMutationTool`, then the Supervisor (plannerModel) autonomously decides which sub-agents to call based on the WorldState
5. **Accumulate Mutations** — Each sub-agent invokes `@Tool` methods on `BurnoutMutationTool`, which appends label/comment mutations to a `pendingActions` list. After the Supervisor completes, `getMutationPlan()` returns the full `GitHubMutationPlan`
6. **Return Response** — The response includes: supervisor explanation, mutation plan, current stress score, estimated post-action stress, chaos metrics, compliance report, protective intervention, and day structure

### WorldState — 18 Discrete Variables

The `WorldState` record captures all measurable burnout indicators from GitHub issue fields. Values are capped to prevent outliers from dominating the stress calculation.

| # | Variable | Type | Cap | Source | Purpose |
|---|----------|------|-----|--------|---------|
| 1 | `deepWorkCount` | int | 5 | Issues with `priority:critical`, `architecture`, or `deep-work` labels | How many cognitively demanding tasks are assigned |
| 2 | `quickWinCount` | int | 5 | Issues with `good-first-issue`, `quick-win`, or `size:S` labels | Small tasks that provide momentum |
| 3 | `maintenanceCount` | int | 5 | Issues with `dependencies`, `documentation`, `maintenance`, or `tech-debt` labels | Routine upkeep work |
| 4 | `deferredCount` | int | 10 | Issues with `deferred`, `next-sprint`, or `backlog` labels | Work parked for later |
| 5 | `delegatedCount` | int | — | (Reserved for future use) | Work distributed to others |
| 6 | `urgentUnassigned` | int | 10 | Issues with `urgent`/`priority:critical` labels but no assignee | Unowned fires — stress amplifier |
| 7 | `contradictoryLabels` | int | 5 | Issues tagged both `bug` and `enhancement` | Label hygiene — signals process chaos |
| 8 | `issuesTouchedToday` | int | 10 | Issues updated in last 8 hours (proxy for context switches) | More switches = less deep work |
| 9 | `issuesUpdatedAfterHours` | int | 5 | Issues updated before 9 AM or after 6 PM | After-hours work = boundary erosion |
| 10 | `staleIssueCount` | int | 10 | Issues not updated in 14+ days | Neglected work creates background guilt |
| 11 | `mysteryMeatCount` | int | 10 | Issues with null or blank body | Ambiguous work = cognitive tax |
| 12 | `unclearQuickWins` | int | 5 | Quick-win labeled issues with no description | Misleading labels — promise simplicity, deliver anxiety |
| 13 | `totalAssigned` | int | 15 | All issues assigned to the user | Raw workload volume |
| 14 | `chaosBucket` | enum | — | `LOW` (≤2) / `MEDIUM` (≤5) / `HIGH` (≤8) / `CRITICAL` (>8) | Discretized chaos score |
| 15 | `complianceScore` | int | 100 | Rounded to nearest 5 from `ComplianceReport` | How well the workload follows 3-3-3 |
| 16 | `is333Compliant` | bool | — | `deepWork ≤ 1 AND quickWins ≤ 3 AND maintenance ≤ 3` | Binary compliance check |
| 17 | `calendarBlocked` | bool | — | Whether a 90-min contiguous block exists | Can the dev actually do deep work? |
| 18 | `consecutiveHighChaosDays` | int | — | Days in a row with chaos > 5 | Sustained stress amplifier |

### Dual Model Architecture

The Supervisor pattern uses **two separate LLM models**:

| Model | Config Key | Role | Why Separate? |
|-------|-----------|------|---------------|
| **plannerModel** | `@Qualifier("plannerModel")` | The Supervisor — decides which sub-agents to invoke and in what order | Needs strong reasoning and planning capability |
| **chatModel** | Default `ChatModel` | The sub-agents — execute specific tools and explain their actions | Needs tool-calling accuracy and concise responses |

Both default to the same Azure OpenAI deployment (`gpt-4o`) but can be configured independently. The planner sees the full WorldState and goals; sub-agents see only the specific issue context and their available tools.

### Supervisor Request Prompt

The Supervisor receives a structured prompt containing the WorldState metrics and explicit goals:

```
Analyze and rebalance this developer's workload to reduce stress.

Current State:
- Stress Score: 72/100 (CRITICAL)
- Total Assigned: 12 issues
- Deep Work: 3 (need exactly 1)
- Quick Wins: 5 (max 3)
- Maintenance: 4 (max 3)
- 3-3-3 Compliant: false
- Chaos Score: 6.5/10
- After Hours Activity: true
- Mystery Meat Issues: 4

Available Issues:
- #1: Fix auth module [priority:critical, deep-work]
- #2: Update README [documentation]
- ...

Goals:
1. Reduce stress score below 50
2. Achieve 3-3-3 compliance (1 deep work, 3 quick wins, 3 maintenance)
3. Protect the developer's focus time
4. Flag unclear issues for scope clarification
5. Recommend wellness actions if stress is high
```

The Supervisor autonomously plans which sub-agents to invoke. For the example above, it might:
1. Call **DeferAgent** to defer 2 of the 3 deep-work items
2. Call **ClassifyAgent** to reclassify excess quick wins as maintenance
3. Call **ScopeAgent** to flag the 4 mystery-meat issues
4. Call **WellnessAgent** to suggest a break (stress ≥ 70)

### Agent Hierarchy

```
                ┌─────────────────────┐
                │  AgentOrchestrator  │
                │  (Coordinator)      │
                └──────────┬──────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │  Burnout    │ │  Explainer  │ │  Protective │
    │  Supervisor │ │  AI Service │ │  AI Service │
    │  Service    │ │             │ │  (Plutchik) │
    └──────┬──────┘ └─────────────┘ └─────────────┘
           │
    ┌──────┴──────────────────────────────────────┐
    │              SUPERVISOR AGENT                │
    │  (plannerModel → coordinates sub-agents)     │
    └──────┬──────────────────────────────────────┘
           │
    ┌──────┼──────┬──────┬──────┬──────┐
    │      │      │      │      │      │
    ▼      ▼      ▼      ▼      ▼      ▼
  Defer  Delegate Classify Scope Wellness
  Agent  Agent    Agent   Agent  Agent
```

### 5 Sub-Agents

Each sub-agent is an autonomous LLM-powered agent with access to specific tools. Sub-agents are built using `AgenticServices.agentBuilder()` with the `chatModel` and a shared `BurnoutMutationTool` instance:

| Agent | Role | Tools | Psychology |
|-------|------|-------|-----------|
| **DeferAgent** | Moves non-critical work to next sprint | `deferIssue()` | Reduces cognitive load by shrinking the active set |
| **DelegateAgent** | Redistributes work across the team | `delegateIssue()` | Addresses learned helplessness — "I don't have to do everything" |
| **ClassifyAgent** | Organizes issues into 3-3-3 structure | `markAsDeepWork()`, `classifyAsQuickWin()`, `classifyAsMaintenance()` | Imposes order on chaos — reduces ambiguity stress |
| **ScopeAgent** | Flags unclear issues | `addScopeNeeded()` | Eliminates "mystery meat" — makes commitments concrete |
| **WellnessAgent** | Recommends self-care actions | `suggestBreak()`, `slowIntake()`, `blockCalendarTime()` | Direct burnout prevention — rest and boundaries |

### 9 Mutation Tools

All tools live on a single `BurnoutMutationTool` instance shared across sub-agents. Each `@Tool` method appends `GitHubAction` objects to a `pendingActions` list. The Supervisor never mutates GitHub directly — it builds a **mutation plan** that the controller can apply or discard (dry run).

| Tool | Labels Added | Labels Removed | Comment | Purpose |
|------|-------------|----------------|---------|---------|
| `deferIssue` | `deferred`, `next-sprint` | `priority:critical` | "🛡️ Deferred to protect your focus" | Lighten the load |
| `delegateIssue` | `delegated`, `needs-owner` | — | "🤝 Marked for delegation" | Share the burden |
| `classifyAsQuickWin` | `quick-win`, `size:S` | — | "⚡ Reclassified as quick win" | Build momentum |
| `classifyAsMaintenance` | `maintenance`, `3-3-3` | — | "🔧 Classified as maintenance" | Organize the day |
| `markAsDeepWork` | `deep-work`, `focus` | — | "🎯 Marked as today's deep work focus" | Protect attention |
| `addScopeNeeded` | `needs-scope`, `blocked` | — | "📋 Needs clearer scope" | Reduce ambiguity |
| `suggestBreak` | — | — | "🧘 Step away for 10–15 minutes" | Cognitive recovery |
| `slowIntake` | — | — | "⏸️ Reduce intake rate" | Prevent accumulation |
| `blockCalendarTime` | — | — | "📅 Block 2-hour focus time" | Protect deep work |

### Stress Reduction Estimation

After the Supervisor completes, the system estimates the new stress score based on the number of mutations planned:

```java
int estimateReducedStress(WorldState state, GitHubMutationPlan plan) {
    int currentStress = state.calculateStressScore();   // e.g. 72
    int actionCount   = plan.actions().size();           // e.g. 8
    int reduction     = actionCount * 7;                 // 8 × 7 = 56
    return Math.max(0, currentStress - reduction);       // max(0, 72 - 56) = 16
}
```

**Why `× 7`?** Each mutation (label change, comment, deferral) is estimated to reduce stress by 7 points. This is a rough heuristic — not a precise calculation. The constant balances:
- **Too low** (e.g. ×3): Supervisor appears ineffective, undermining user trust
- **Too high** (e.g. ×15): Overpromises stress relief, creating disillusionment
- **×7**: Each action feels meaningfully impactful while remaining conservative

The response returns both `initialStressScore` and `expectedStressScore`, so the flamegraph can show a before/after delta.

### Supervisor Configuration

```java
SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
    .chatModel(plannerModel)            // LLM that decides which agents to call
    .subAgents(defer, delegate, classify, scope, wellness)
    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
    .maxAgentsInvocations(10)           // with 5 sub-agents, allows full coverage
    .build();
```

**Configuration details:**
- `SupervisorResponseStrategy.SUMMARY` — the Supervisor produces a narrative summary of all sub-agent actions rather than returning raw tool outputs
- `maxAgentsInvocations(10)` — with 5 sub-agents, this allows each agent to be called twice or a subset to be called more often for complex workloads
- Sub-agents are built per-request with a fresh `BurnoutMutationTool`, ensuring mutation plans don't leak between requests

### Graceful Degradation

Every Supervisor invocation is wrapped in a try/catch. When the LLM is unavailable (dummy credentials, network failure, rate limiting), the system falls back to a deterministic response:

```
🔴 Critical stress detected. Current stress score: 72/100

⚠️ Your workload exceeds the 3-3-3 structure. You have 3 deep work items
(max 1), 5 quick wins (max 3), and 4 maintenance tasks (max 3).

*LLM agents unavailable - using deterministic fallback*
```

The fallback uses the same `WorldState` calculated in Step 3 — no AI is required for the numbers. The user always gets their stress score, compliance status, and day structure even without the LLM. The AI only adds the *how* (mutation plan) and *why* (natural-language explanation).

### Three AI Personas

| Service | Persona | Tone | Purpose |
|---------|---------|------|---------|
| **ExplainerAiService** | "Supportive productivity coach" | Warm, encouraging, concise | Explain the *why* behind the plan |
| **ProtectiveAiService** | "Protective AI companion" | Gentle, validating, brief | Emotional support and self-care |
| **FridayDeployAiService** | "Calm, experienced release engineer" | Direct, professional, measured | Deploy risk assessment |

---

## 12. Flamegraph Visualization Psychology

The flamegraph is not just a data visualization — it's a **stress communication tool** designed to make abstract workload concepts viscerally understandable.

### Why a Flamegraph?

Traditional burnout metrics are numbers on a dashboard. The flamegraph metaphor works because:
1. **Fire = danger** — primal association creates immediate emotional response
2. **Height = depth** — issues stacked taller feel "heavier"
3. **Color = urgency** — red/amber/green maps to traffic light intuition
4. **Width = proportion** — wider bars communicate relative impact

### Per-Issue Stress Calculation

```javascript
function calcStress(issue, bucket, globalStress) {
    let s = bucket.baseStress;                            // category baseline
    if (issue.complexity) s += issue.complexity * 3;       // complexity multiplier
    s += (globalStress || 30) * 0.3;                      // global stress leak
    if (label includes 'urgent'|'critical'|'blocker')     s += 20;   // urgency bonus
    if (label includes 'bug')                             s += 10;   // bug bonus
    return min(100, round(s));
}
```

### Base Stress by Category

| Category | Base Stress | Rationale |
|----------|-------------|-----------|
| Deep Work | 60 | Highest cognitive demand — naturally stressful |
| Maintenance | 30 | Routine but tedious — moderate drain |
| Quick Wins | 20 | Low effort, high satisfaction — energizing |
| Deferred | 10 | Parked — minimal active burden |

### Color Psychology

| Color | Threshold | Hex | Emotional Response |
|-------|-----------|-----|--------------------|
| 🟢 Green | < 35% | `--stress-low` | "This is fine" — calm, under control |
| 🟡 Amber | 35–64% | `--stress-moderate` | "Needs attention" — cautious, alert |
| 🔴 Red | ≥ 65% | `--stress-high` | "Danger" — urgent, fight-or-flight |

### Label Bonuses

| Label | Bonus | Why |
|-------|-------|-----|
| `urgent`, `critical`, `blocker` | +20 | These labels carry implicit time pressure |
| `bug` | +10 | Bugs disrupt planned work and cause reactive firefighting |

### Global Stress Leak

```
s += globalStress * 0.3
```

Every issue absorbs 30% of the developer's overall stress level. This models the psychological reality that stress is **non-local** — a high-stress environment makes *every* task feel harder, even easy ones.

### Worked Example

**Issue #26: "Refactor authentication module" (Deep Work, global stress = 40)**

| Component | Value | Explanation |
|-----------|-------|-------------|
| Base Stress | 60 | Deep Work category |
| + Complexity × 3 | +0 | No complexity field set |
| + Global Stress × 0.3 | +12 | 40 × 0.3 = 12 |
| + Label bonuses | +0 | `deep-work` is not a bonus label |
| **= Total** | **72%** 🔴 | `min(100, 60 + 0 + 12 + 0)` = 72% High |

Adding a `critical` label would push it to **92%**. Reducing global stress below 17 would drop it into 🟡 Moderate.

---

## 13. Priority Weighting & Day Plan Construction

The day plan construction algorithm determines which issues to work on and in what order, balancing urgency, recency, and stability.

### Priority Weights

```java
"priority:critical" → 0   // highest priority (sorted first)
"priority:high"     → 1
"urgent"            → 1
default             → 2   // lowest priority (sorted last)
```

### Sort Key (multi-level)

```
1. Priority weight (ascending — critical first)
2. updatedAt (descending — most recently updated first, nulls last)
3. Issue number (ascending — stable tiebreaker for reproducibility)
```

### Day Plan Assembly

```
┌────────────────────────────────────────────┐
│  All issues classified by IssueClassifier  │
│                                            │
│  DEEP_WORK bucket ─── take top 1 ─────►  Today's Deep Work
│                   └── remaining ──────►  Deferred
│                                            │
│  QUICK_WIN bucket ─── take top 3 ─────►  Today's Quick Wins
│                   └── remaining ──────►  Deferred
│                                            │
│  MAINTENANCE bucket── take top 3 ─────►  Today's Maintenance
│                   └── remaining ──────►  Deferred
│                                            │
│  DEFERRED bucket ─── all items ───────►  Deferred
└────────────────────────────────────────────┘
```

---

## 14. Graceful Degradation Philosophy

A core design principle: **every AI feature must work without AI**. This isn't just engineering robustness — it's psychological safety for the user.

### Why This Matters

If a burnout prevention tool fails when you need it most (during an outage, when the LLM is down), it *increases* stress instead of reducing it. The system is designed so that:

1. **All metrics are deterministic** — stress scores, chaos metrics, compliance reports, classification, and day plans all compute without any LLM
2. **AI agents only explain and support** — they never make decisions the deterministic layer hasn't already made
3. **Every agent has a fallback path** — when the LLM fails, return deterministic responses
4. **Fallback responses acknowledge the limitation** — e.g., "*LLM agents unavailable — using deterministic fallback*"

### Degradation Cascade

```
Level 0: Full LLM available
    → AI-powered explanations, emotional support, supervisor pattern active

Level 1: LLM call fails (exception)
    → Catch exception, log warning, return fallback response
    → All metrics still accurate, just no AI narration

Level 2: LLM not configured (dummy credentials)
    → @Autowired(required = false) → null services
    → llmEnabled = false, skip all AI calls
    → Full deterministic operation with pre-written messages

Level 3: Backend partially available
    → Health endpoint returns UP
    → Individual endpoints degrade independently
```

### Demo Label System

For demonstrations without real GitHub data, the system supports synthetic time control via labels:

| Demo Label | Effect | Bypasses |
|------------|--------|----------|
| `demo:touched-today` | Issue appears recently updated | Real timestamp check |
| `demo:after-hours` | Issue triggers after-hours signal | Real clock check |
| `demo:stale-14d` | Issue appears stale (14+ days) | Real age calculation |
| `demo:friday` | Forces Friday context | Real day-of-week |

**Golden Rule:** If *any* `demo:*` label exists on an issue, real timestamps are **never** consulted.

---

## 15. Complete Constants Reference

Every magic number in the system, organized by subsystem:

### Time & Scheduling

| Constant | Value | Where Used |
|----------|-------|------------|
| Deep work minimum block | 90 minutes | CalendarService |
| After-hours start (Chaos) | 8am / 6pm | ChaosMetricsService |
| After-hours start (WorldState) | 9am / 6pm | WorldState |
| Chaos "recent" window | 60 minutes | ChaosMetricsService |
| Urgent unresolved threshold | 24 hours | ChaosMetricsService |
| Stale issue age | 14 days | ComplianceService, WorldState |
| Demo rate limit | 5 minutes per repo | DemoFlamegraphController |
| GitHub fetch limit | 100 issues per sync | DemoFlamegraphController |

### 3-3-3 Structure

| Constant | Value | Where Used |
|----------|-------|------------|
| MAX_DEEP_WORK | 1 | ComplianceService |
| MAX_QUICK_WINS | 3 | ComplianceService |
| MAX_MAINTENANCE | 3 | ComplianceService |
| MAX_ACTIVE | 7 | ComplianceService |
| Miller's Law alignment | 7 ± 2 | Design rationale |

### Stress Scoring

| Constant | Value | Where Used |
|----------|-------|------------|
| Over-assignment threshold | > 7 issues | WorldState.calculateStressScore() |
| Over-assignment penalty per issue | 4 points | WorldState.calculateStressScore() |
| Over-assignment cap | 20 points | WorldState.calculateStressScore() |
| Extra deep-work penalty per item | 10 points | WorldState.calculateStressScore() |
| No deep-work penalty | 5 points | WorldState.calculateStressScore() |
| Workload total cap | 40 points | WorldState.calculateStressScore() |
| Context-switch threshold | > 5 issues | WorldState.calculateStressScore() |
| Context-switch penalty per issue | 3 points | WorldState.calculateStressScore() |
| Context-switch cap | 15 points | WorldState.calculateStressScore() |
| Mystery meat penalty per issue | 2 points | WorldState.calculateStressScore() |
| Mystery meat cap | 10 points | WorldState.calculateStressScore() |
| Unclear quick-win penalty | 1 point each | WorldState.calculateStressScore() |
| Unclear quick-win cap | 5 points | WorldState.calculateStressScore() |
| Sustained stress penalty per day | 5 points | WorldState.calculateStressScore() |
| Sustained stress cap | 15 points | WorldState.calculateStressScore() |
| After-hours penalty per issue | 5 points | WorldState.calculateStressScore() |
| After-hours cap | 10 points | WorldState.calculateStressScore() |
| Theoretical max score | 100 (clamped) | WorldState.calculateStressScore() |

### Stress Level Thresholds

| Level | Backend (WorldState) | Frontend (flamegraph.html) |
|-------|---------------------|---------------------------|
| LOW | < 30 | < 35 |
| MODERATE | 30–49 | 35–64 |
| HIGH | 50–69 | ≥ 65 |
| CRITICAL | ≥ 70 | — |

### Chaos Scoring

| Constant | Value | Where Used |
|----------|-------|------------|
| Mystery meat trigger | ≥ 3 issues | ChaosMetricsService |
| Unresolved urgent trigger | ≥ 3 items | ChaosMetricsService |
| Touched today trigger | ≥ 6 issues | ChaosMetricsService |
| Label explosion trigger | ≥ 12 labels | ChaosMetricsService |
| Each signal weight | 2 points | ChaosMetricsService |
| Score cap | 10 | ChaosMetricsService |
| ChaosBucket: LOW | ≤ 2 | WorldState |
| ChaosBucket: MEDIUM | ≤ 5 | WorldState |
| ChaosBucket: HIGH | ≤ 8 | WorldState |
| ChaosBucket: CRITICAL | > 8 | WorldState |

### Friday Deploy

| Constant | Value | Where Used |
|----------|-------|------------|
| Chaos penalty tier 1 | -20 at chaos > 5 | FridayScoreController |
| Chaos penalty tier 2 | -20 at chaos > 8 | FridayScoreController |
| Non-compliant penalty | -15 | FridayScoreController |
| Urgent unassigned penalty | -15 | FridayScoreController |
| After-hours penalty | -10 | FridayScoreController |
| Mystery meat penalty | -10 at count > 3 | FridayScoreController |
| Ready threshold | ≥ 80 | FridayScoreController |
| Caution threshold | ≥ 50 | FridayScoreController |
| Not-ready threshold | < 50 | FridayScoreController |

### Agent System

| Constant | Value | Where Used |
|----------|-------|------------|
| Supervisor max invocations | 10 | BurnoutSupervisorService |
| Stress reduction per action | 7 points | BurnoutSupervisorService |
| Protection trigger: consecutive days | ≥ 2 | AgentOrchestrator |
| Protection trigger: stress score | ≥ 70 | AgentOrchestrator |
| Protection trigger: total assigned | > 10 | AgentOrchestrator |

### Flamegraph (Frontend)

| Constant | Value | Where Used |
|----------|-------|------------|
| Deep Work base stress | 60 | flamegraph.html |
| Quick Wins base stress | 20 | flamegraph.html |
| Maintenance base stress | 30 | flamegraph.html |
| Deferred base stress | 10 | flamegraph.html |
| Complexity multiplier | 3 per point | flamegraph.html |
| Global stress leak | 30% (× 0.3) | flamegraph.html |
| Urgency bonus | +20 | flamegraph.html |
| Bug bonus | +10 | flamegraph.html |

---

## References

- Maslach, C., & Leiter, M. P. (2016). *Understanding the burnout experience*. World Psychiatry.
- Sweller, J. (1988). *Cognitive load during problem solving*. Cognitive Science.
- Csikszentmihalyi, M. (1990). *Flow: The psychology of optimal experience*.
- Newport, C. (2016). *Deep Work: Rules for focused success in a distracted world*.
- Plutchik, R. (2001). *The nature of emotions*. American Scientist.
- Mark, G., Gudith, D., & Klocke, U. (2008). *The cost of interrupted work*. CHI Conference.
- Sonnentag, S. (2012). *Psychological detachment from work during leisure time*. Current Directions in Psychological Science.
- McEwen, B. S. (1998). *Protective and damaging effects of stress mediators*. New England Journal of Medicine.
- Easterbrook, J. A. (1959). *The effect of emotion on cue utilization*. Psychological Review.
- Miller, G. A. (1956). *The magical number seven, plus or minus two*. Psychological Review.
- Burkeman, O. (2021). *Four Thousand Weeks: Time management for mortals*.
- Yerkes, R. M., & Dodson, J. D. (1908). *The relation of strength of stimulus to rapidity of habit-formation*. Journal of Comparative Neurology and Psychology.
