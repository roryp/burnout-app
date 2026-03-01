# The Psychology & Science Behind Burnout-as-a-Service

This document provides an exhaustive reference for every burnout analysis idea, psychological model, and algorithm implemented in the system. Each section maps research-backed concepts to their concrete implementation.

---

## Table of Contents

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
11. [GOAP Action Planning](#11-goap-action-planning)
12. [AI Agent Architecture](#12-ai-agent-architecture)
13. [Flamegraph Visualization Psychology](#13-flamegraph-visualization-psychology)
14. [Priority Weighting & Day Plan Construction](#14-priority-weighting--day-plan-construction)
15. [Graceful Degradation Philosophy](#15-graceful-degradation-philosophy)
16. [Complete Constants Reference](#16-complete-constants-reference)

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

## 11. GOAP Action Planning

The system adapts Goal-Oriented Action Planning (GOAP) from game AI to workload management. Originally developed for NPC behavior in games (Orkin, 2003), GOAP works by backward-chaining from a goal state to find the optimal sequence of actions.

### The Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  WorldState  │────►│  GOAP        │────►│  Mutation    │
│  (18 vars)   │     │  Planner     │     │  Plan        │
│              │     │              │     │              │
│  Current     │     │  Finds path  │     │  GitHub      │
│  metrics     │     │  from state  │     │  Actions     │
│              │     │  to goal     │     │  (labels,    │
│              │     │              │     │   comments)  │
└──────────────┘     └──────────────┘     └──────────────┘
```

### WorldState: 18 Discrete Variables

The world state captures a complete snapshot of a developer's workload situation:

| Variable | Type | Cap | Description |
|----------|------|-----|-------------|
| `deepWorkCount` | int | 5 | Active deep work items |
| `quickWinCount` | int | 5 | Active quick win items |
| `maintenanceCount` | int | 5 | Active maintenance items |
| `deferredCount` | int | 10 | Deferred / backlog items |
| `delegatedCount` | int | — | Items marked for delegation |
| `urgentUnassigned` | int | 10 | Urgent items with no owner |
| `contradictoryLabels` | int | 5 | Issues with conflicting labels (bug+enhancement) |
| `issuesTouchedToday` | int | 10 | Issues updated within 8h |
| `issuesUpdatedAfterHours` | int | 5 | Issues updated before 9am/after 6pm |
| `staleIssueCount` | int | 10 | Issues untouched for 14+ days |
| `mysteryMeatCount` | int | 10 | Issues with blank body |
| `unclearQuickWins` | int | 5 | Quick-win label + blank body |
| `totalAssigned` | int | 15 | Total open issues assigned |
| `chaosBucket` | enum | — | LOW / MEDIUM / HIGH / CRITICAL |
| `complianceScore` | int | — | Rounded to nearest 5 |
| `is333Compliant` | bool | — | Meets 3-3-3 structure |
| `calendarBlocked` | bool | — | No 90-min free block |
| `consecutiveHighChaosDays` | int | — | Days with chaosScore ≥ 5 |

### GitHub Mutation Actions

The GOAP planner outputs a sealed interface of three action types:

| Action | Purpose | Example |
|--------|---------|---------|
| `AddLabels(issueNumber, labels)` | Classify, defer, flag | Add `deferred`, `next-sprint` |
| `RemoveLabels(issueNumber, labels)` | Declassify, unblock | Remove `priority:critical` |
| `Comment(issueNumber, body)` | Communicate, suggest | "🛡️ Deferred to protect your focus" |

### Stress Reduction Estimation

After the supervisor generates a plan, the system estimates the resulting stress reduction:

```java
int reduction = actionCount * 7;   // ~7 stress points per action
estimatedStress = max(0, currentStress - reduction);
```

This heuristic is intentionally conservative — it's better to underestimate relief than to overpromise.

---

## 12. AI Agent Architecture

The system uses LangChain4j's **Supervisor Pattern** (`langchain4j-agentic`) — a hierarchical multi-agent system where a planner LLM coordinates specialized sub-agents.

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

Each sub-agent is an autonomous LLM-powered agent with access to specific tools:

| Agent | Role | Tools | Psychology |
|-------|------|-------|-----------|
| **DeferAgent** | Moves non-critical work to next sprint | `deferIssue()` | Reduces cognitive load by shrinking the active set |
| **DelegateAgent** | Redistributes work across the team | `delegateIssue()` | Addresses learned helplessness — "I don't have to do everything" |
| **ClassifyAgent** | Organizes issues into 3-3-3 structure | `markAsDeepWork()`, `classifyAsQuickWin()`, `classifyAsMaintenance()` | Imposes order on chaos — reduces ambiguity stress |
| **ScopeAgent** | Flags unclear issues | `addScopeNeeded()` | Eliminates "mystery meat" — makes commitments concrete |
| **WellnessAgent** | Recommends self-care actions | `suggestBreak()`, `slowIntake()`, `blockCalendarTime()` | Direct burnout prevention — rest and boundaries |

### 9 Mutation Tools

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

### Supervisor Configuration

```java
SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
    .chatModel(plannerModel)            // LLM that decides which agents to call
    .subAgents(defer, delegate, classify, scope, wellness)
    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
    .maxAgentsInvocations(10)           // with 5 sub-agents, allows full coverage
    .build();
```

### Three AI Personas

| Service | Persona | Tone | Purpose |
|---------|---------|------|---------|
| **ExplainerAiService** | "Supportive productivity coach" | Warm, encouraging, concise | Explain the *why* behind the plan |
| **ProtectiveAiService** | "Protective AI companion" | Gentle, validating, brief | Emotional support and self-care |
| **FridayDeployAiService** | "Calm, experienced release engineer" | Direct, professional, measured | Deploy risk assessment |

---

## 13. Flamegraph Visualization Psychology

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

## 14. Priority Weighting & Day Plan Construction

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

## 15. Graceful Degradation Philosophy

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

## 16. Complete Constants Reference

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
- Orkin, J. (2003). *Applying goal-oriented action planning to games*. AI Game Programming Wisdom 2.
- Burkeman, O. (2021). *Four Thousand Weeks: Time management for mortals*.
- Yerkes, R. M., & Dodson, J. D. (1908). *The relation of strength of stimulus to rapidity of habit-formation*. Journal of Comparative Neurology and Psychology.
