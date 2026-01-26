# Burnout-as-a-Service — Friday Deploy Agent (GitHub Copilot Demo Plan)

Build a VS Code extension + Copilot-flavored agent experience that demonstrates burnout prevention through accessibility-inspired heuristics, the **3-3-3 day structure**, and emotionally supportive AI. The demo progresses through **five phases**:

1. **Noisy Day Simulator** — Show chaos before the Agent intervenes  
2. **WCAG Anti-Burnout Rules** — Apply accessibility principles as heuristics  
3. **Agent Layer (LangChain4j)** — AI agents read backlog and make decisions  
4. **Emotionally Supportive Agent** — Detect emotional signals, respond protectively  
5. **Friday Deploy Confidence** — Make "deploy on Friday" calm and repeatable  

This is written to be **stage-safe**: deterministic UI, explicit data contracts, and a single GitHub mutation path.

---

## Tech Stack Overview

- **VS Code Extension (TypeScript)**: UI layer + **ALL GitHub I/O** via `gh` CLI  
- **Java Backend (Spring Boot + LangChain4j)**: **ALL logic** (metrics, heuristics, agents, scoring)  
- **Optional Copilot UX (nice-to-have)**: Natural-language “Reshape my day” prompt surface inside Copilot Chat / panel.  
  - **Note**: core demo works without Copilot SDK/bridge; keep it as an optional layer to avoid live-demo failure modes.

---

## Architecture (Single Source of Truth)

**One truth**:

- **Extension** owns ALL GitHub I/O (read + write) using `gh` CLI  
- **Backend** owns ALL logic (chaos metrics, WCAG heuristics, 3-3-3 planning, emotional protection, Friday score)  
- Backend **never calls GitHub** directly — it only processes issue data synced from extension  

### Contracts

1) **Issue Sync (Extension → Backend)**  
- Extension fetches issues with `gh issue list ... --json ...`  
- Extension posts to backend cache: `POST /api/issues/sync`  

2) **Mutation (Backend → Extension)**  
- Backend returns `ActionPlan` (label/comment/assign operations)  
- Extension executes actions via `gh` and then **re-syncs** issues to keep cache honest  

This prevents “split-brain” bugs and makes the demo predictable.

---

## Phase 1: The Noisy Day Simulator (Background to Burnout)

**Goal**: Make the audience *feel* burnout signals by showing a chaotic backlog and fragmented day.

### Demo Flow (live)

1. Open VS Code with the extension loaded  
2. Issues are pre-seeded (or seeded during the demo)  
3. Extension syncs issues → backend returns deterministic chaos score → status bar turns red  
4. Narrate: “This is what burnout looks like in our tools.”  
5. Trigger: “Let me ask the agent for help.” → reshapes the day

### Step 1 — Create demo repo

```bash
gh repo create roryp/burnout-demo --public --confirm
```

### Step 1.5 — Create Labels FIRST (before seeding issues)

**Labels must exist before issues can use them.** GitHub silently ignores unknown labels on `gh issue create -l unknown`.

```bash
#!/bin/bash
REPO="roryp/burnout-demo"

# Demo labels (signals for demo mode)
gh label create "demo:stale-14d" -R $REPO --color "808080" --description "Demo: simulate 14-day staleness" --force
gh label create "demo:after-hours" -R $REPO --color "ffa500" --description "Demo: simulate late-night work" --force
gh label create "demo:touched-today" -R $REPO --color "1d76db" --description "Demo: simulate context switching" --force
gh label create "demo:friday" -R $REPO --color "5319e7" --description "Demo: Friday deploy scenario" --force

# 3-3-3 workflow labels
gh label create "deep-work" -R $REPO --color "d73a4a" --description "3-3-3: Deep focus work" --force
gh label create "quick-win" -R $REPO --color "0e8a16" --description "3-3-3: Quick win (<30min)" --force
gh label create "maintenance" -R $REPO --color "c5def5" --description "3-3-3: Routine maintenance" --force
gh label create "deferred" -R $REPO --color "cfd3d7" --description "3-3-3: Deferred to next sprint" --force
gh label create "delegated" -R $REPO --color "f9d0c4" --description "Marked for delegation" --force
gh label create "needs-owner" -R $REPO --color "fbca04" --description "Needs someone to own" --force
gh label create "needs-scope" -R $REPO --color "d93f0b" --description "Needs clearer definition" --force
gh label create "blocked" -R $REPO --color "b60205" --description "Blocked on something" --force
gh label create "focus" -R $REPO --color "6f42c1" --description "Today's focus item" --force
gh label create "next-sprint" -R $REPO --color "bfd4f2" --description "Scheduled for next sprint" --force
gh label create "3-3-3" -R $REPO --color "0052cc" --description "Part of 3-3-3 plan" --force

# Priority labels
gh label create "priority:critical" -R $REPO --color "b60205" --description "Critical priority" --force
gh label create "priority:high" -R $REPO --color "d93f0b" --description "High priority" --force
gh label create "priority:low" -R $REPO --color "0e8a16" --description "Low priority" --force
gh label create "urgent" -R $REPO --color "ff0000" --description "Urgent" --force

# Size labels
gh label create "size:S" -R $REPO --color "c2e0c6" --description "Small (< 1 day)" --force
gh label create "size:M" -R $REPO --color "fef2c0" --description "Medium (1-3 days)" --force
gh label create "size:L" -R $REPO --color "f9d0c4" --description "Large (> 3 days)" --force
gh label create "scope-defined" -R $REPO --color "bfdadc" --description "Scope is clear" --force

# Category labels (for classifier)
gh label create "documentation" -R $REPO --color "0075ca" --description "Documentation work" --force
gh label create "tech-debt" -R $REPO --color "d4c5f9" --description "Technical debt" --force
gh label create "cleanup" -R $REPO --color "e4e669" --description "Cleanup task" --force
gh label create "routine" -R $REPO --color "c5def5" --description "Routine work" --force
gh label create "architecture" -R $REPO --color "5319e7" --description "Architecture work" --force
gh label create "security" -R $REPO --color "b60205" --description "Security related" --force
gh label create "performance" -R $REPO --color "ff7619" --description "Performance work" --force

# Additional workflow labels
gh label create "bug" -R $REPO --color "d73a4a" --description "Bug" --force
gh label create "enhancement" -R $REPO --color "a2eeef" --description "Enhancement" --force
gh label create "wontfix" -R $REPO --color "ffffff" --description "Won't fix" --force
gh label create "investigation" -R $REPO --color "d4c5f9" --description "Investigation needed" --force
gh label create "flaky" -R $REPO --color "fbca04" --description "Flaky test" --force
gh label create "dependencies" -R $REPO --color "0366d6" --description "Dependency updates" --force
gh label create "accessibility" -R $REPO --color "a2eeef" --description "Accessibility improvements" --force
gh label create "triage" -R $REPO --color "d876e3" --description "Needs triage" --force
gh label create "chore" -R $REPO --color "fef2c0" --description "Chore/housekeeping" --force
gh label create "refactor" -R $REPO --color "d4c5f9" --description "Refactoring" --force
gh label create "ci" -R $REPO --color "000000" --description "CI/CD related" --force
gh label create "devops" -R $REPO --color "006b75" --description "DevOps work" --force

echo "✅ All labels created for $REPO"
```

### Step 2 — Seed 15+ chaotic issues

```bash
REPO="roryp/burnout-demo"

# ============================================================
# DEMO LABELS: Use canonical names from DemoLabels constants
# - demo:stale-14d    = simulate 14-day staleness
# - demo:after-hours  = simulate after-hours work  
# - demo:touched-today = simulate recent activity (context switching)
# - demo:friday       = simulate Friday deploy scenario
# ============================================================

# 5 Mystery Meat (urgent, no owner, vague)
gh issue create -R $REPO -t "URGENT: Production is slow" -b "" -l urgent
gh issue create -R $REPO -t "Fix the thing" -b "You know which one" -l urgent
gh issue create -R $REPO -t "ASAP: Customer unhappy" -b "" -l urgent,priority:critical
gh issue create -R $REPO -t "!!!" -b "Look at logs" -l urgent
gh issue create -R $REPO -t "Need this yesterday" -b "" -l urgent

# 3 Contradictory labels
gh issue create -R $REPO -t "Refactor auth module" -b "Might be a bug or enhancement" -l bug,enhancement
gh issue create -R $REPO -t "Blocked but critical" -b "Waiting on API team" -l blocked,priority:critical
gh issue create -R $REPO -t "Won't fix but do it" -b "Legacy requirement" -l wontfix,priority:high

# 4 Stale issues (use demo:stale-14d to simulate staleness without waiting)
gh issue create -R $REPO -t "Update README" -b "Been meaning to do this" -l documentation,demo:stale-14d
gh issue create -R $REPO -t "Investigate memory usage" -b "Opened 2 weeks ago" -l investigation,demo:stale-14d
gh issue create -R $REPO -t "Add dark mode" -b "Community request" -l enhancement,demo:stale-14d
gh issue create -R $REPO -t "Flaky test in CI" -b "Fails sometimes" -l bug,flaky,demo:stale-14d

# 3 Overlapping deadlines (assigned to demo user)
gh issue create -R $REPO -t "Fix N+1 query in dashboard" -b "DEEP WORK: Performance critical. Look at DashboardService.java line 47." -l priority:critical,performance --assignee "@me"
gh issue create -R $REPO -t "Quarterly report due" -b "Due Friday" -l priority:high --assignee "@me"
gh issue create -R $REPO -t "Security audit findings" -b "Also due Friday" -l priority:critical,security --assignee "@me"

# 2 Clean quick-wins (will classify correctly for satisfying reshape)
gh issue create -R $REPO -t "Bump lodash to 4.17.21" -b "- [ ] Update package.json\n- [ ] Run tests\n\nDone when: CI green" -l quick-win,size:S,dependencies --assignee "@me"
gh issue create -R $REPO -t "Add alt text to logo image" -b "Accessibility fix. Done when: lighthouse score improves." -l quick-win,size:S,accessibility --assignee "@me"

# 2 Clean maintenance items (will classify correctly)
gh issue create -R $REPO -t "Update CONTRIBUTING.md" -b "Add section on running tests locally." -l documentation,maintenance --assignee "@me"
gh issue create -R $REPO -t "Remove unused CSS classes" -b "Cleanup from Q3 refactor." -l tech-debt,cleanup,maintenance --assignee "@me"
```

> Tip: Seed issues a few days before the demo so “stale” metrics are natural.

### Step 2.5 — (Moved to Step 1.5)

> Labels are now created in **Step 1.5** before seeding issues. This prevents GitHub from silently ignoring unknown labels.

### Step 2.6 — Demo Configuration (Single Source of Truth)

**Extension Config** (`src/config.ts`):

```typescript
// === SINGLE SOURCE OF TRUTH for demo configuration ===
export const DemoConfig = {
    REPO: 'roryp/burnout-demo',
    USER_ID: 'roryp',
    BACKEND_URL: 'http://localhost:8080',
    DEMO_MODE: true,
} as const;

// Usage: import { DemoConfig } from './config';
// const repo = DemoConfig.REPO;
```

**Backend Config** (`application-demo.yml`):

```yaml
demo:
  repo: roryp/burnout-demo
  userId: roryp
  friday:
    enabled: true  # Forces Friday scenario regardless of actual day
  clock:
    zone: America/New_York
    # Optional: freeze time for demo
    # fixed: "2025-01-17T14:30:00"
```

**Java Config Class**:

```java
@Configuration
@ConfigurationProperties(prefix = "demo")
public class DemoConfiguration {
    private String repo = "roryp/burnout-demo";
    private String userId = "roryp";
    private FridayConfig friday = new FridayConfig();
    private ClockConfig clock = new ClockConfig();

    // Getters and setters...

    public static class FridayConfig {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ClockConfig {
        private String zone = "UTC";
        private String fixed; // Optional ISO instant for frozen time
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }
        public String getFixed() { return fixed; }
        public void setFixed(String fixed) { this.fixed = fixed; }
    }
}
```

### Step 3 — Issue sync and deterministic chaos score (backend)

**IssueCache** (stores synced issues):

```java
@Service
public class IssueCache {
    private final Map<String, CachedIssues> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();

    public void put(String repo, List<Issue> issues, Instant fetchedAt) {
        cache.put(repo, new CachedIssues(issues, fetchedAt));
        versions.computeIfAbsent(repo, r -> new AtomicLong()).incrementAndGet();
    }

    public List<Issue> get(String repo) {
        CachedIssues c = cache.get(repo);
        return c == null ? List.of() : c.issues();
    }

    public boolean hasRepo(String repo) {
        return cache.containsKey(repo);
    }

    public long getVersion(String repo) {
        return versions.getOrDefault(repo, new AtomicLong(0)).get();
    }

    record CachedIssues(List<Issue> issues, Instant fetchedAt) {}
}
```

**IssueSyncController** (receives issues from extension):

```java
@RestController
@RequestMapping("/api/issues")
public class IssueSyncController {
    private final IssueCache issueCache;

    public IssueSyncController(IssueCache issueCache) {
        this.issueCache = issueCache;
    }

    @PostMapping("/sync")
    public SyncAck sync(@RequestBody IssueSyncRequest req) {
        // Validate schema version (prevents silent drift)
        if (req.schemaVersion() != IssueSyncRequest.SCHEMA_VERSION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Schema version mismatch: expected " + IssueSyncRequest.SCHEMA_VERSION + ", got " + req.schemaVersion());
        }
        issueCache.put(req.repo(), req.issues(), req.fetchedAt());
        return new SyncAck(req.repo(), req.issues().size(), req.fetchedAt(), issueCache.getVersion(req.repo()));
    }
}

// Request with schema version for drift detection
public record IssueSyncRequest(String repo, List<Issue> issues, Instant fetchedAt, int schemaVersion) {
    public static final int SCHEMA_VERSION = 1; // Increment on breaking changes
}

// ACK response for debugging
public record SyncAck(String repo, int receivedCount, Instant fetchedAt, long cacheVersion) {}
```

**Issue record** (GitHub JSON fields we analyze):

```java
public record Issue(
    int number,
    String title,
    String body,                    // Scope clarity: empty = "mystery meat"
    List<Label> labels,             // PRIMARY: classification into 3-3-3 buckets
    List<Assignee> assignees,       // Who owns this work
    Instant createdAt,              // Staleness calculation
    Instant updatedAt,              // Activity patterns, context switching
    String state,                   // open/closed
    Milestone milestone             // Planning visibility (optional)
) {
    public record Label(String name) {}
    public record Assignee(String login) {}
    public record Milestone(String title, Instant dueOn) {}
}
```

**ChaosMetricsService** (IMPORTANT: reads issues, never calls GitHub):

```java
@Service
public class ChaosMetricsService {

    private final Clock clock; // Injected for time determinism

    public ChaosMetricsService(Clock clock) {
        this.clock = clock;
    }

    public ChaosMetrics calculate(List<Issue> issues) {
        return calculate(issues, clock);
    }

    public ChaosMetrics calculate(List<Issue> issues, Clock clk) {
        Instant now = clk.instant();
        return new ChaosMetrics(
            countRecentUpdates(issues, 60, now),
            countUrgentOver24h(issues, now),
            countDistinctLabels(issues),
            hasAfterHoursActivity(issues, now, clk),  // Pass clk explicitly
            countMissingDescriptionOrAssignee(issues),
            calculateOverallScore(issues, now, clk)  // Pass clk explicitly
        );
    }

    // === DEMO-MODE: Synthetic time offsets via labels ===
    // Demo labels take precedence over real timestamps for stage safety
    // ALWAYS use DemoLabels constants to avoid name drift
    
    private boolean hasAfterHoursActivity(List<Issue> issues, Instant now, Clock clk) {
        return issues.stream().anyMatch(i -> 
            DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS) || // Demo label wins
            (!DemoLabels.hasDemoLabel(i) && isAfterHours(i.updatedAt(), clk))); // Real check only if no demo labels
    }

    private long countRecentUpdates(List<Issue> issues, int minutes, Instant now) {
        return issues.stream().filter(i -> 
            DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) || // Demo label wins
            (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null && i.updatedAt().isAfter(now.minus(Duration.ofMinutes(minutes))))
        ).count();
    }

    private long countStaleIssues(List<Issue> issues, int days, Instant now) {
        // Build dynamic stale label: demo:stale-14d, demo:stale-7d, etc.
        String staleLabel = "demo:stale-" + days + "d";
        return issues.stream().filter(i -> 
            DemoLabels.hasLabel(i, staleLabel) || // Demo label wins
            (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null && i.createdAt().isBefore(now.minus(Duration.ofDays(days))))
        ).count();
    }

    private long countUrgentOver24h(List<Issue> issues, Instant now) {
        return issues.stream().filter(i -> 
            LabelUtils.hasLabel(i, "urgent") &&  // Use LabelUtils for real labels
            (DemoLabels.hasLabel(i, DemoLabels.STALE_14D) || // Demo staleness
             (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null && i.createdAt().isBefore(now.minusSeconds(86400))))
        ).count();
    }

    private int countDistinctLabels(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> i.labels() != null)
            .flatMap(i -> i.labels().stream())
            .map(Issue.Label::name)  // Nested record: Issue.Label
            .distinct()
            .count();
    }

    private int countMissingDescriptionOrAssignee(List<Issue> issues) {
        return (int) issues.stream().filter(i -> 
            (i.body() == null || i.body().isBlank()) ||
            (i.assignees() == null || i.assignees().isEmpty())
        ).count();
    }

    /**
     * EXPLICIT CHAOS SCORE FORMULA (0-10, deterministic):
     * +2 if mysteryMeatCount >= 3
     * +2 if unresolvedUrgent >= 3
     * +2 if issuesTouchedToday >= 6
     * +2 if afterHoursSignal == true
     * +2 if distinctLabelCount >= 12
     * Clamped to max 10.
     */
    private double calculateOverallScore(List<Issue> issues, Instant now, Clock clk) {
        int score = 0;
        
        int mysteryMeat = countMissingDescriptionOrAssignee(issues);
        if (mysteryMeat >= 3) score += 2;
        
        long urgent = countUrgentOver24h(issues, now);
        if (urgent >= 3) score += 2;
        
        long touched = countRecentUpdates(issues, 60, now);
        if (touched >= 6) score += 2;
        
        if (hasAfterHoursActivity(issues, now, clk)) score += 2;
        
        int labels = countDistinctLabels(issues);
        if (labels >= 12) score += 2;
        
        return Math.min(10, score);
    }

    // After-hours check: 6pm-8am local time (or weekend)
    // Takes Clock explicitly - never use field inside helpers
    private boolean isAfterHours(Instant timestamp, Clock clk) {
        if (timestamp == null) return false;
        ZonedDateTime zoned = timestamp.atZone(clk.getZone()); // Use passed clock
        int hour = zoned.getHour();
        DayOfWeek dow = zoned.getDayOfWeek();
        return hour < 8 || hour >= 18 || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}

public record ChaosMetrics(
    long issuesTouchedRecently,   // Renamed: was "interruptionsPerHour"
    long unresolvedUrgent,
    int distinctLabelCount,       // Renamed: was "contextSwitches"
    boolean afterHoursSignal,
    int mysteryMeatCount,
    double score
) {
    public static final int SCHEMA_VERSION = 1;
    public static ChaosMetrics notSynced() {
        return new ChaosMetrics(0, 0, 0, false, 0, -1); // score=-1 means not synced
    }
    public boolean isSynced() { return score >= 0; }
    public int schemaVersion() { return SCHEMA_VERSION; }
}
```

### Clock Configuration (Demo Mode vs Production)

```java
@Configuration
public class ClockConfig {
    
    @Bean
    @Profile("!demo")
    public Clock productionClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("demo")
    public Clock demoClock(@Value("${demo.clock.zone:America/New_York}") String zone,
                           @Value("${demo.clock.fixed:}") String fixed) {
        ZoneId zoneId = ZoneId.of(zone);
        
        if (fixed != null && !fixed.isBlank()) {
            // Freeze time at specific instant for 100% reproducible demo
            return Clock.fixed(Instant.parse(fixed), zoneId);
        }
        
        // Pin to a specific Friday afternoon for predictable demo
        return Clock.fixed(
            ZonedDateTime.of(2026, 1, 30, 14, 0, 0, 0, zoneId).toInstant(),
            zoneId
        );
    }
}
```
```

### SyntheticTimeResolver (centralized demo label time logic)

**Purpose**: Single source of truth for "demo label wins" precedence. Both `ChaosMetricsService` and `WorldState` delegate here.

```java
/**
 * Centralized utility for resolving time-based checks with demo label precedence.
 * GOLDEN RULE: If ANY demo:* label exists on issue, never consult real timestamps.
 */
public final class SyntheticTimeResolver {
    
    private SyntheticTimeResolver() {} // Utility class

    /** Returns true if issue should be considered "touched recently" */
    public static boolean isTouchedRecently(Issue issue, int minutesWindow, Instant now) {
        if (DemoLabels.hasLabel(issue, DemoLabels.TOUCHED_TODAY)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false; // Has demo label but not touched-today
        return issue.updatedAt() != null && 
               issue.updatedAt().isAfter(now.minus(Duration.ofMinutes(minutesWindow)));
    }

    /** Returns true if issue should be considered "after hours" */
    public static boolean isAfterHours(Issue issue, Clock clk) {
        if (DemoLabels.hasLabel(issue, DemoLabels.AFTER_HOURS)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false; // Has demo label but not after-hours
        return isRealAfterHours(issue.updatedAt(), clk);
    }

    /** Returns true if issue should be considered "stale" (older than N days) */
    public static boolean isStale(Issue issue, int days, Instant now) {
        String staleLabel = "demo:stale-" + days + "d";
        if (DemoLabels.hasLabel(issue, staleLabel)) return true;
        if (DemoLabels.hasLabel(issue, DemoLabels.STALE_14D)) return true; // Generic stale
        if (DemoLabels.hasDemoLabel(issue)) return false;
        return issue.createdAt() != null && 
               issue.createdAt().isBefore(now.minus(Duration.ofDays(days)));
    }

    /** Returns true if it's Friday (demo label > config > clock) */
    public static boolean isFriday(Issue issue, DemoConfig config, Clock clk) {
        if (DemoLabels.hasLabel(issue, DemoLabels.FRIDAY)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false;
        if (config != null && config.forceFriday()) return true;
        return LocalDate.now(clk).getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    private static boolean isRealAfterHours(Instant updatedAt, Clock clk) {
        if (updatedAt == null) return false;
        ZonedDateTime zdt = updatedAt.atZone(clk.getZone());
        int hour = zdt.getHour();
        DayOfWeek dow = zdt.getDayOfWeek();
        return hour < 8 || hour >= 18 || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
```

---

### 3-3-3 Compliance Analyzer (NEW)

**Purpose**: Detect when a user is NOT following the 3-3-3 day structure by analyzing GitHub issue fields.

#### Key Fields for 3-3-3 Violation Detection

| Field | Why It Matters | Violation Signal |
|-------|----------------|------------------|
| `labels` | **Primary classifier** — determines bucket | Wrong distribution across buckets |
| `assignees` | Identifies whose workload to analyze | Overloaded if too many active issues |
| `state` | Only `open` issues count toward active work | N/A |
| `updatedAt` | Detects context switching patterns | Many issues touched same day = fragmented |
| `body` | Scope clarity for quick-win detection | Empty body = can't be a quick win |
| `createdAt` | Staleness: old issues pile up as deferred | Deferred backlog growing = avoidance |

### DemoLabels Constants (canonical label names)

**CRITICAL**: Use these constants EVERYWHERE (Java + TypeScript) to avoid silent mismatches.

```java
/**
 * Canonical demo label names. Use these constants to avoid case/spelling mismatches.
 * Mirror this in TypeScript: src/constants/demoLabels.ts
 */
public final class DemoLabels {
    private DemoLabels() {}

    // Synthetic time labels (override real timestamps)
    public static final String TOUCHED_TODAY = "demo:touched-today";
    public static final String AFTER_HOURS = "demo:after-hours";
    public static final String STALE_14D = "demo:stale-14d";
    public static final String FRIDAY = "demo:friday";

    // All demo labels (for checking if issue has any)
    public static final Set<String> ALL = Set.of(
        TOUCHED_TODAY, AFTER_HOURS, STALE_14D, FRIDAY
    );

    /** Returns true if this issue has ANY demo label (should skip real timestamp checks) */
    public static boolean hasDemoLabel(Issue issue) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().toLowerCase().startsWith("demo:"));
    }

    /** Check for specific demo label (case-insensitive) */
    public static boolean hasLabel(Issue issue, String demoLabel) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(demoLabel));
    }
}
```

**TypeScript mirror** (`src/constants/demoLabels.ts`):
```typescript
export const DemoLabels = {
    TOUCHED_TODAY: 'demo:touched-today',
    AFTER_HOURS: 'demo:after-hours',
    STALE_14D: 'demo:stale-14d',
    FRIDAY: 'demo:friday',
} as const;

export const ALL_DEMO_LABELS = Object.values(DemoLabels);

export function hasDemoLabel(issue: Issue): boolean {
    return issue.labels?.some(l => l.name.toLowerCase().startsWith('demo:')) ?? false;
}
```

```java
@Service
public class ThreeThreeThreeComplianceService {

    private final IssueClassifierService classifier;
    private final Clock clock; // Inject for time determinism

    public ThreeThreeThreeComplianceService(IssueClassifierService classifier, Clock clock) {
        this.classifier = classifier;
        this.clock = clock;
    }

    /**
     * Analyzes a user's assigned issues for 3-3-3 compliance.
     * Returns violations with severity and actionable recommendations.
     */
    public ComplianceReport analyze(List<Issue> issues, String userId) {
        // Filter to user's assigned open issues
        // Defensive null checks for stage safety
        List<Issue> userIssues = issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream().anyMatch(a -> a.login().equals(userId)))
            .toList();

        // Classify each issue into 3-3-3 buckets
        Map<Classification, List<Issue>> buckets = userIssues.stream()
            .collect(Collectors.groupingBy(classifier::classify));

        List<Issue> deepWork = buckets.getOrDefault(Classification.DEEP_WORK, List.of());
        List<Issue> quickWins = buckets.getOrDefault(Classification.QUICK_WIN, List.of());
        List<Issue> maintenance = buckets.getOrDefault(Classification.MAINTENANCE, List.of());
        List<Issue> deferred = buckets.getOrDefault(Classification.DEFERRED, List.of());

        List<Violation> violations = new ArrayList<>();

        // === VIOLATION 1: Multiple Deep Work (max 1) ===
        if (deepWork.size() > 1) {
            violations.add(new Violation(
                ViolationType.MULTIPLE_DEEP_WORK,
                Severity.CRITICAL,
                "You have " + deepWork.size() + " deep-work issues active. Max is 1.",
                deepWork,
                "Pick ONE critical issue. Move others to next sprint or delegate.",
                "labels"  // The field that identified this
            ));
        }

        // === VIOLATION 2: Too Many Quick Wins (max 3) ===
        if (quickWins.size() > 3) {
            violations.add(new Violation(
                ViolationType.QUICK_WIN_OVERLOAD,
                Severity.WARNING,
                "You have " + quickWins.size() + " quick wins. Max is 3 per day.",
                quickWins.subList(3, quickWins.size()),
                "Defer " + (quickWins.size() - 3) + " quick wins to tomorrow.",
                "labels"
            ));
        }

        // === VIOLATION 3: Too Much Maintenance (max 3) ===
        if (maintenance.size() > 3) {
            violations.add(new Violation(
                ViolationType.MAINTENANCE_OVERLOAD,
                Severity.WARNING,
                "You have " + maintenance.size() + " maintenance tasks. Max is 3.",
                maintenance.subList(3, maintenance.size()),
                "Batch remaining maintenance for a dedicated maintenance day.",
                "labels"
            ));
        }

        // === VIOLATION 4: No Deep Work (missing focused work) ===
        if (deepWork.isEmpty() && !userIssues.isEmpty()) {
            violations.add(new Violation(
                ViolationType.NO_DEEP_WORK,
                Severity.INFO,
                "No deep-work issue assigned. You may be stuck in reactive mode.",
                List.of(),
                "Identify one priority:critical or architecture issue to focus on.",
                "labels"
            ));
        }

        // === VIOLATION 5: Context Switching (updatedAt analysis) ===
        // Use demo labels OR real timestamps with injected Clock
        Instant todayCutoff = clock.instant().minus(Duration.ofHours(8));
        long issuesTouchedToday = userIssues.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ||
                         (!DemoLabels.hasDemoLabel(i) && i.updatedAt().isAfter(todayCutoff)))
            .count();
        if (issuesTouchedToday > 5) {
            violations.add(new Violation(
                ViolationType.EXCESSIVE_CONTEXT_SWITCHING,
                Severity.CRITICAL,
                "You've touched " + issuesTouchedToday + " issues today. High context-switch cost.",
                List.of(),
                "Focus on completing one issue before moving to the next.",
                "updatedAt"
            ));
        }

        // === VIOLATION 6: Mystery Meat Quick Wins (body analysis) ===
        List<Issue> mysteryQuickWins = quickWins.stream()
            .filter(i -> i.body() == null || i.body().isBlank())
            .toList();
        if (!mysteryQuickWins.isEmpty()) {
            violations.add(new Violation(
                ViolationType.UNCLEAR_QUICK_WINS,
                Severity.WARNING,
                mysteryQuickWins.size() + " quick wins have no description. Can't verify they're actually quick.",
                mysteryQuickWins,
                "Add scope/acceptance criteria or reclassify as deferred.",
                "body"
            ));
        }

        // === VIOLATION 7: Deferred Backlog Growing (createdAt analysis) ===
        // Use demo:stale-14d label OR real timestamp with injected Clock
        Instant staleCutoff = clock.instant().minus(Duration.ofDays(14));
        List<Issue> staleDeferred = deferred.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.STALE_14D) ||
                         (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null && i.createdAt().isBefore(staleCutoff)))
            .toList();
        if (staleDeferred.size() > 5) {
            violations.add(new Violation(
                ViolationType.DEFERRED_BACKLOG_GROWING,
                Severity.INFO,
                staleDeferred.size() + " deferred issues are >14 days old. Backlog debt accumulating.",
                staleDeferred,
                "Schedule a backlog grooming session. Close or delegate stale issues.",
                "createdAt"
            ));
        }

        // === VIOLATION 8: Total Workload Overload ===
        int totalActive = deepWork.size() + quickWins.size() + maintenance.size();
        int maxAllowed = 1 + 3 + 3; // 3-3-3 = 7 max
        if (totalActive > maxAllowed) {
            violations.add(new Violation(
                ViolationType.TOTAL_OVERLOAD,
                Severity.CRITICAL,
                "Total active issues: " + totalActive + ". Max for 3-3-3 is " + maxAllowed + ".",
                List.of(),
                "Defer " + (totalActive - maxAllowed) + " issues to protect your focus.",
                "assignees"
            ));
        }

        return new ComplianceReport(
            userId,
            violations.isEmpty(),
            violations,
            Map.of(
                "deepWork", deepWork.size(),
                "quickWins", quickWins.size(),
                "maintenance", maintenance.size(),
                "deferred", deferred.size()
            ),
            calculateComplianceScore(violations)
        );
    }

    private int calculateComplianceScore(List<Violation> violations) {
        int score = 100;
        for (Violation v : violations) {
            score -= switch (v.severity()) {
                case CRITICAL -> 25;
                case WARNING -> 10;
                case INFO -> 5;
            };
        }
        return Math.max(0, score);
    }
}

public record ComplianceReport(
    String userId,
    boolean isCompliant,
    List<Violation> violations,
    Map<String, Integer> bucketCounts,
    int complianceScore  // 0-100
) {
    public static final int SCHEMA_VERSION = 1;
    public static ComplianceReport notSynced() {
        return new ComplianceReport(null, false, List.of(), Map.of(), -1);
    }
    public boolean isSynced() { return complianceScore >= 0; }
    public int schemaVersion() { return SCHEMA_VERSION; }
}

public record Violation(
    ViolationType type,
    Severity severity,
    String message,
    List<Issue> affectedIssues,
    String recommendation,
    String field  // Which GitHub field identified this violation
) {}

public enum ViolationType {
    MULTIPLE_DEEP_WORK,
    QUICK_WIN_OVERLOAD,
    MAINTENANCE_OVERLOAD,
    NO_DEEP_WORK,
    EXCESSIVE_CONTEXT_SWITCHING,
    UNCLEAR_QUICK_WINS,
    DEFERRED_BACKLOG_GROWING,
    TOTAL_OVERLOAD
}

public enum Severity { CRITICAL, WARNING, INFO }
```

### /api/compliance endpoint (deterministic)

```java
@GetMapping("/api/compliance")
public ComplianceReport compliance(@RequestParam String repo, @RequestParam String userId) {
    if (!issueCache.hasRepo(repo)) {
        return ComplianceReport.notSynced();
    }
    return complianceService.analyze(issueCache.get(repo), userId);
}
```

**/api/chaos** endpoint (deterministic):

```java
@GetMapping("/api/chaos")
public ChaosResponse chaos(@RequestParam String repo) {
    if (!issueCache.hasRepo(repo)) {
        return ChaosResponse.notSynced();
    }
    ChaosMetrics metrics = chaosMetricsService.calculate(issueCache.get(repo));
    return ChaosResponse.from(metrics);
}
```

### Step 4 — Extension GitHub I/O via `gh` (safe execution)

Use `execFile` (not `exec`) to avoid quoting issues on Windows.

```ts
import { execFile } from "child_process";
import { promisify } from "util";
const execFileAsync = promisify(execFile);

/**
 * Safe gh CLI wrapper (Windows-compatible):
 * - Uses execFile (not exec) to avoid shell injection
 * - Always uses --json for structured output, parses stdout only
 * - Ignores stderr warnings (e.g., "Notice: A new release of gh is available")
 * - Handles non-zero exits with friendly error messages
 */
async function gh(args: string[], cwd?: string): Promise<string> {
  try {
    const { stdout, stderr } = await execFileAsync("gh", args, { 
      cwd, 
      windowsHide: true,
      maxBuffer: 10 * 1024 * 1024  // 10MB for large issue lists
    });
    
    // Log stderr warnings but don't fail (gh often prints notices)
    if (stderr && stderr.trim()) {
      console.debug(`[gh CLI warning]: ${stderr.trim()}`);
    }
    
    return stdout;
  } catch (e: unknown) {
    const err = e as { code?: string; stderr?: string; message?: string };
    
    // Handle common errors with friendly messages
    if (err.code === 'ENOENT') {
      throw new Error('gh CLI not found. Install from https://cli.github.com');
    }
    if (err.stderr?.includes('authentication')) {
      throw new Error('gh CLI not authenticated. Run: gh auth login');
    }
    if (err.stderr?.includes('Could not resolve')) {
      throw new Error(`Repository not found or no access: ${args.join(' ')}`);
    }
    
    throw new Error(`gh CLI failed: ${err.message || 'Unknown error'}`);
  }
}

/** Parse JSON from gh CLI - always use for structured commands */
async function ghJson<T>(args: string[], cwd?: string): Promise<T> {
  const stdout = await gh(args, cwd);
  try {
    return JSON.parse(stdout.trim());
  } catch {
    throw new Error(`Failed to parse gh CLI JSON output: ${stdout.slice(0, 200)}...`);
  }
}

export async function fetchIssues(repo: string) {
  const stdout = await gh([
    "issue", "list",
    "-R", repo,
    "--json", "number,title,body,labels,assignees,createdAt,updatedAt,state,milestone",
    "--limit", "100"
  ]);
  return JSON.parse(stdout);
}
```

### Step 5 — Status bar polling pattern (sync → score)

```ts
const REPO = "roryp/burnout-demo";

// === SCHEMA VERSION VALIDATION (catches backend/extension drift) ===
const EXPECTED_SCHEMA_VERSIONS: Record<string, number> = {
  ChaosMetrics: 1,
  ComplianceReport: 1,
  StressResponse: 1,
  GitHubMutationPlan: 1,
  GoapActionSummary: 1,
};

interface SchemaVersioned {
  schemaVersion?: number;
}

function validateSchemaVersion<T extends SchemaVersioned>(
  data: T, 
  type: keyof typeof EXPECTED_SCHEMA_VERSIONS
): T {
  const expected = EXPECTED_SCHEMA_VERSIONS[type];
  const actual = data.schemaVersion;
  
  if (actual !== expected) {
    const msg = `Schema mismatch for ${type}: expected v${expected}, got v${actual ?? 'undefined'}. ` +
                `Extension and backend may be out of sync. Rebuild both.`;
    vscode.window.showWarningMessage(`⚠️ ${msg}`);
    console.error(`[Schema Drift] ${msg}`);
    // Continue anyway (graceful degradation) but warn user
  }
  
  return data;
}

// Every 10 seconds: sync issues -> read deterministic chaos score
async function updateChaosStatus() {
  const issues = await fetchIssues(REPO);
  await syncIssues(REPO, issues);             // POST /api/issues/sync
  const chaosRaw = await getChaosScore(REPO); // GET /api/chaos
  const chaos = validateSchemaVersion(chaosRaw, 'ChaosMetrics');

  if (chaos.status === "not_synced") {
    chaosStatusBar.text = "$(sync) Syncing...";
    return;
  }

  chaosStatusBar.text = `$(alert) Chaos: ${chaos.score}/10`;
  chaosStatusBar.backgroundColor =
    chaos.score > 7 ? new vscode.ThemeColor("statusBarItem.errorBackground") :
    chaos.score > 4 ? new vscode.ThemeColor("statusBarItem.warningBackground") :
    undefined;
}
```

### Step 6 — Mock calendar fragmentation (backend)

```java
@Service
public class CalendarService {
    public CalendarFragmentation getFragmentation() {
        return CalendarFragmentation.builder()
            .meetings(8)
            .totalHours(6)
            .avgGapMinutes(15)
            .contextSwitchCost("HIGH")
            .build();
    }
}
```

---

## Phase 2: WCAG Anti-Burnout Rules

**Goal**: Apply WCAG principles as burnout prevention heuristics (deterministic + explainable).

| WCAG Principle | Anti-Burnout Rule | Implementation |
|---|---|---|
| Perceivable | Work must be visible in chunks | Group into 3-3-3 buckets; avoid “wall of tickets” |
| Operable | Limit simultaneous contexts | Max 1 Deep Work issue active |
| Understandable | Every issue needs clear next step | Flag missing `next-step`, suggest wording |
| Robust | System works under pressure | If >15 issues, auto-defer lowest priority |

### GitHub Issue Fields for 3-3-3 Violation Detection

The algorithm analyzes these fields to detect when someone is **NOT** following 3-3-3:

| Field | Analysis Purpose | Violation Signals |
|-------|------------------|-------------------|
| **`labels`** | Primary bucket classifier | Multiple `priority:critical` = too many deep work; No quick-win labels = missing balance |
| **`assignees`** | Identify whose workload to check | >7 open issues assigned = overloaded beyond 3-3-3 capacity |
| **`body`** | Validate scope clarity | Empty body on "quick win" = can't verify it's quick → reclassify as Deferred |
| **`updatedAt`** | Detect context-switching | >5 issues touched in 8 hours = fragmented attention, not focused |
| **`createdAt`** | Detect deferred backlog growth | >5 issues older than 14 days = avoidance/tech debt accumulating |
| **`state`** | Filter active work | Only `open` issues count toward 3-3-3 limits |
| **`milestone`** | Check planning visibility | No milestone = work not chunked into perceivable sprints |

### DayStructure (3-3-3)

```java
public record DayStructure(
    Issue deepWork,
    List<Issue> quickWins,
    List<Issue> maintenance,
    List<Issue> deferred
) {
    public static final int MAX_DEEP_WORK = 1;
    public static final int MAX_QUICK_WINS = 3;
    public static final int MAX_MAINTENANCE = 3;
    public static final int MAX_ACTIVE = MAX_DEEP_WORK + MAX_QUICK_WINS + MAX_MAINTENANCE; // 7

    public boolean isCompliant() {
        return (deepWork == null ? 0 : 1) <= MAX_DEEP_WORK
            && quickWins.size() <= MAX_QUICK_WINS
            && maintenance.size() <= MAX_MAINTENANCE;
    }
}

public enum Classification { DEEP_WORK, QUICK_WIN, MAINTENANCE, DEFERRED }
```

### Enhanced Issue Classifier (field-based analysis)

```java
@Service
public class IssueClassifierService {

    /**
     * Classifies an issue into a 3-3-3 bucket using multiple GitHub fields.
     * 
     * Field Analysis Priority:
     * 1. labels     — Primary signal (explicit categorization)
     * 2. body       — Scope clarity (empty = can't be quick win)
     * 3. createdAt  — Staleness (old issues often get deferred)
     * 4. updatedAt  — Activity (recently active = higher priority)
     */
    public Classification classify(Issue issue) {
        // === DEEP WORK: High-stakes, focused work (max 1) ===
        // Detected via: labels indicating criticality/complexity
        if (isDeepWork(issue)) {
            return Classification.DEEP_WORK;
        }

        // === QUICK WIN: Small, completable tasks (max 3) ===
        // Detected via: labels + body (must have clear scope)
        if (isQuickWin(issue)) {
            return Classification.QUICK_WIN;
        }

        // === MAINTENANCE: Routine upkeep (max 3) ===
        // Detected via: labels indicating housekeeping tasks
        if (isMaintenance(issue)) {
            return Classification.MAINTENANCE;
        }

        // === DEFERRED: Everything else (protect the human) ===
        return Classification.DEFERRED;
    }

    private boolean isDeepWork(Issue issue) {
        return hasAnyLabel(issue, "priority:critical", "priority:high", "architecture", "security")
            || estimateHours(issue) > 2
            || hasLabelPattern(issue, "epic.*|feature.*");
    }

    private boolean isQuickWin(Issue issue) {
        // CRITICAL: Quick wins MUST have clear scope (body field)
        if (issue.body() == null || issue.body().isBlank()) {
            return false;  // "Mystery meat" cannot be a quick win
        }

        return hasAnyLabel(issue, "good-first-issue", "quick-win", "low-hanging-fruit", "trivial")
            || (estimateHours(issue) < 0.5 && hasClearScope(issue))
            || (hasLabel(issue, "enhancement") && issue.body().length() < 500);
    }

    private boolean isMaintenance(Issue issue) {
        return hasAnyLabel(issue, "dependencies", "documentation", "triage", 
                          "chore", "refactor", "tech-debt", "ci", "devops");
    }

    /**
     * Estimates hours from labels or body content.
     * Looks for: "estimate:Xh", "size:S/M/L", or body keywords.
     */
    private double estimateHours(Issue issue) {
        // Null guard: labels can be null in weird GitHub payloads
        if (issue.labels() == null) return 2.0;
        
        // Check for explicit estimate labels: "estimate:2h", "size:L"
        for (Issue.Label label : issue.labels()) {  // Nested record: Issue.Label
            String name = label.name().toLowerCase();
            if (name.startsWith("estimate:")) {
                return parseEstimate(name.substring(9));
            }
            if (name.equals("size:s") || name.equals("small")) return 0.5;
            if (name.equals("size:m") || name.equals("medium")) return 2.0;
            if (name.equals("size:l") || name.equals("large")) return 4.0;
            if (name.equals("size:xl")) return 8.0;
        }

        // Fallback: estimate from body length (rough heuristic)
        if (issue.body() != null) {
            int bodyLength = issue.body().length();
            if (bodyLength < 100) return 0.5;
            if (bodyLength < 500) return 2.0;
            return 4.0;
        }

        return 2.0; // Default to medium
    }

    private boolean hasClearScope(Issue issue) {
        if (issue.body() == null) return false;
        String body = issue.body().toLowerCase();
        // Look for acceptance criteria indicators
        return body.contains("- [ ]") 
            || body.contains("acceptance criteria")
            || body.contains("done when")
            || body.contains("steps:")
            || body.contains("expected:");
    }

    private boolean hasLabel(Issue issue, String labelName) {
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(labelName));
    }

    private boolean hasAnyLabel(Issue issue, String... labelNames) {
        // Normalize BOTH sides to lowercase for reliable matching
        Set<String> target = Arrays.stream(labelNames)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    private boolean hasLabelPattern(Issue issue, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return issue.labels().stream()
            .anyMatch(l -> pattern.matcher(l.name()).matches());
    }

    private double parseEstimate(String est) {
        try {
            return Double.parseDouble(est.replace("h", "").replace("hr", ""));
        } catch (NumberFormatException e) {
            return 2.0;
        }
    }
}
```

### Wheel visualization (extension webview)

- D3 donut chart with 3 segments (Deep / Quick / Maintenance)
- 800ms transitions on reshape
- Hover shows issue titles
- Data source: use `dayPlan` from `/api/reshape` response, or `GET /api/day-plan` if cached

---

## Phase 3: Agent Layer (LangChain4j)

**Goal**: Show an agent workflow that reads backlog → analyzes signals → reshapes day plan.

### Dependencies (example)

```xml
<dependencies>
  <dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic</artifactId>
    <version>1.0.0-beta6</version>
  </dependency>
  <dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic-patterns</artifactId>
    <version>1.0.0-beta6</version>
  </dependency>
  <dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-azure-open-ai</artifactId>
    <version>1.0.0-beta6</version>
  </dependency>
</dependencies>
```

### Agent interfaces

**IMPORTANT**: The agents below are for *orchestration and explanation only*. All real decisions come from deterministic services.

Data flow: `repo` → (deterministic) IssueClassifierService → buckets → (deterministic) ComplianceService → violations → (deterministic) GOAPPlanner → actions → (LLM) ExplainerAgent → human-friendly narration

```java
// ❌ WRONG: Don't let LLM make decisions
// public interface ChaosAnalyzer { @Agent ChaosMetrics analyze(...) }

// ✅ RIGHT: LLM only explains deterministic results
public interface ExplainerAgent {
    @SystemMessage("""
      You are a friendly narrator for a burnout prevention system.
      
      Given a DETERMINISTIC RESULT (already computed by Java services):
      - Explain the chaos score in human terms
      - Describe why each issue landed in its bucket
      - Justify the GOAP action plan
      - Use empathetic, protective language
      
      DO NOT make any decisions. The decisions are already made.
      Your job is to make them understandable and reassuring.
      
      If you're unsure, just say "See details below" and let the UI show raw data.
      """)
    @Agent
    String explain(@V("result") DeterministicResult result);
}

public record DeterministicResult(
    ChaosMetrics chaos,
    ComplianceReport compliance,
    WorldState worldState,
    DayStructure dayPlan,
    ActionPlan actionPlan,
    int fridayScore
) {}

// ❌ REMOVED: DayPlanner as LLM agent (was making decisions)
// LLM should ONLY explain, not decide. See ReshapeService below.
```

### Deterministic Reshape Service (NOT an LLM agent)

```java
@Service
public class ReshapeService {
    private final IssueClassifierService classifier;
    private final ThreeThreeThreeComplianceService complianceService;
    private final GOAPPlanner goapPlanner;
    private final ChaosMetricsService chaosService;
    private final FridayScoreService fridayService;
    private final ExplainerAgent explainerAgent; // LLM for narration only
    private final Clock clock; // Injected for time determinism

    public ReshapeResponse reshape(List<Issue> issues, String userId) {
        // 1. ALL DECISIONS ARE DETERMINISTIC
        ChaosMetrics chaos = chaosService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
        DayStructure dayPlan = buildDayPlan(issues, userId);
        GoapActionPlan actionPlan = goapPlanner.plan(state, issues, userId);  // Pass userId
        int fridayScore = fridayService.calculate(issues, chaos, clock);

        DeterministicResult result = new DeterministicResult(
            chaos, compliance, state, dayPlan, actionPlan, fridayScore
        );

        // 2. LLM ONLY EXPLAINS (with fallback)
        String explanation;
        try {
            explanation = explainerAgent.explain(result);
        } catch (Exception e) {
            explanation = "See details below."; // Graceful fallback
        }

        return new ReshapeResponse(result, explanation);
    }

    // DETERMINISTIC issue ordering for stable selection
    private static final Comparator<Issue> STABLE_ISSUE_ORDER = Comparator
        .comparing((Issue i) -> getPriorityWeight(i))    // critical > high > normal
        .thenComparing(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ? 0 : 1)
        .thenComparing(Issue::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(Issue::number);  // stable tiebreaker

    private static int getPriorityWeight(Issue issue) {
        if (LabelUtils.hasLabel(issue, "priority:critical")) return 0;
        if (LabelUtils.hasLabel(issue, "priority:high")) return 1;
        if (LabelUtils.hasLabel(issue, "urgent")) return 1;
        return 2;
    }

    private DayStructure buildDayPlan(List<Issue> issues, String userId) {
        Map<Classification, List<Issue>> buckets = issues.stream()
            .filter(i -> "open".equals(i.state()))
            // Null guard: assignees can be null in malformed payloads
            .filter(i -> i.assignees() != null && i.assignees().stream().anyMatch(a -> a.login().equals(userId)))
            .collect(Collectors.groupingBy(classifier::classify));

        // Sort each bucket deterministically for stable selection
        List<Issue> deepWork = buckets.getOrDefault(Classification.DEEP_WORK, List.of())
            .stream().sorted(STABLE_ISSUE_ORDER).toList();
        List<Issue> quickWins = buckets.getOrDefault(Classification.QUICK_WIN, List.of())
            .stream().sorted(STABLE_ISSUE_ORDER).toList();
        List<Issue> maintenance = buckets.getOrDefault(Classification.MAINTENANCE, List.of())
            .stream().sorted(STABLE_ISSUE_ORDER).toList();
        List<Issue> deferred = buckets.getOrDefault(Classification.DEFERRED, List.of())
            .stream().sorted(STABLE_ISSUE_ORDER).toList();

        return new DayStructure(
            deepWork.isEmpty() ? null : deepWork.get(0),  // Pick highest priority
            quickWins.stream().limit(3).toList(),
            maintenance.stream().limit(3).toList(),
            Stream.concat(
                deepWork.stream().skip(1),  // Overflow deep work → deferred
                Stream.concat(
                    quickWins.stream().skip(3),
                    Stream.concat(maintenance.stream().skip(3), deferred.stream())
                )
            ).toList()
        );
    }
}
```

### Tool: read cache only

```java
@Component
public class GitHubIssueTool {
    private final IssueCache issueCache;

    public GitHubIssueTool(IssueCache issueCache) {
        this.issueCache = issueCache;
    }

    @Tool("Fetch all open issues for a repo (from the synced cache)")
    public List<Issue> fetchIssues(@P("repo") String repo) {
        return issueCache.get(repo);
    }
}
```

### Workflow orchestration (sequential)

```java
UntypedAgent burnoutAgent = AgenticServices.sequenceBuilder()
    .subAgents(fetcher, chaosAnalyzer, dayPlanner)
    .outputKey("dayPlan")
    .listener(listener) // create per-request listener to avoid step leakage
    .build();

DayPlan result = (DayPlan) burnoutAgent.invoke(Map.of("repo", "roryp/burnout-demo"));
```

---

## GOAP Pattern for Stress/Burnout/Workload Calculation

**Goal-Oriented Action Planning (GOAP)** is an AI planning technique that finds optimal action sequences to achieve goals. We use it to:

1. Calculate a **composite stress metric** from world state
2. Plan **protective actions** to reduce burnout risk
3. Prioritize interventions based on **goal urgency**

### GOAP Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         GOAP PLANNER                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐  │
│  │  World State │───▶│Greedy Planner│───▶│  Bounded Action Plan │  │
│  │  (current)   │    │  (cost-based)│    │  (sequence to goal)  │  │
│  └──────────────┘    └──────────────┘    └──────────────────────┘  │
│         │                   ▲                                        │
│         ▼                   │                                        │
│  ┌──────────────┐    ┌──────────────┐                               │
│  │    Goals     │    │   Actions    │                               │
│  │  (weighted)  │    │ (pre/effect) │                               │
│  └──────────────┘    └──────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
```

### World State (Burnout Signals)

The world state captures all measurable burnout indicators from GitHub issue fields.

**IMPORTANT for GOAP**: Keep state **discrete** (ints, booleans, coarse buckets) to keep the greedy planner fast and predictable. Continuous values like `Instant` or unbounded `double` would create unbounded state spaces.

```java
public record WorldState(
    // === From labels field ===
    int deepWorkCount,          // # of priority:critical/architecture issues assigned
    int quickWinCount,          // # of good-first-issue/quick-win issues assigned
    int maintenanceCount,       // # of dependencies/documentation issues assigned
    int deferredCount,          // # of unclassified issues
    int delegatedCount,         // # of issues delegated to others
    int urgentUnassigned,       // # of 'urgent' with no assignee (mystery meat)
    int contradictoryLabels,    // # with conflicting labels (bug+enhancement)

    // === From updatedAt field ===
    int issuesTouchedToday,     // context switching signal (capped at 10)
    int issuesUpdatedAfterHours,// after-hours work signal (capped at 5)

    // === From createdAt field ===
    int staleIssueCount,        // open > 14 days with no update (capped at 10)

    // === From body field ===
    int mysteryMeatCount,       // empty body issues (capped at 10)
    int unclearQuickWins,       // quick-win label but no scope (capped at 5)

    // === From assignees field ===
    int totalAssigned,          // total open issues assigned to user (capped at 15)
    
    // === Derived metrics (DISCRETE BUCKETS for GOAP) ===
    ChaosBucket chaosBucket,    // LOW(0-2), MEDIUM(3-5), HIGH(6-8), CRITICAL(9-10)
    int complianceScore,        // 0-100 in steps of 5
    boolean is333Compliant,     // true if within 1+3+3 limits
    boolean calendarBlocked,    // true if deep work time is protected on calendar
    
    // === Historical ===
    int consecutiveHighChaosDays  // sustained stress indicator (capped at 7)
) {
    // === GOAP-friendly: Discrete chaos buckets ===
    public enum ChaosBucket { 
        LOW(0), MEDIUM(1), HIGH(2), CRITICAL(3);
        public final int ordinalValue;
        ChaosBucket(int v) { this.ordinalValue = v; }
        
        public static ChaosBucket from(double score) {
            if (score <= 2) return LOW;
            if (score <= 5) return MEDIUM;
            if (score <= 8) return HIGH;
            return CRITICAL;
        }
    }

    // === Factory for building from raw metrics ===
    public static WorldState from(List<Issue> issues, String userId, 
                                   ChaosMetrics chaos, ComplianceReport compliance, Clock clock) {
        // Count and cap values for discrete state space
        // Pass Clock to all time-based counting methods
        return new WorldState(
            Math.min(5, countDeepWork(issues, userId)),
            Math.min(5, countQuickWins(issues, userId)),
            Math.min(5, countMaintenance(issues, userId)),
            Math.min(10, countDeferred(issues, userId)),
            0, // delegatedCount - starts at 0, updated by actions
            Math.min(10, countUrgentUnassigned(issues)),
            Math.min(5, countContradictory(issues)),
            Math.min(10, countTouchedToday(issues, userId, clock)),
            Math.min(5, countAfterHours(issues, userId, clock)),
            Math.min(10, countStale(issues, clock)),
            Math.min(10, countMysteryMeat(issues)),
            Math.min(5, countUnclearQuickWins(issues, userId)),
            Math.min(15, countAssigned(issues, userId)),
            ChaosBucket.from(chaos.score()),
            roundToFive(compliance.complianceScore()),
            compliance.isCompliant(),
            false, // calendarBlocked - starts false
            Math.min(7, getConsecutiveHighChaosDays(userId))
        );
    }

    // === Wither methods for GOAP state transitions ===
    public WorldState withDeepWorkCount(int v) {
        return new WorldState(v, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday, 
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }
    
    public WorldState withTotalAssigned(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, v, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }
    
    public WorldState withDeferredCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            v, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withDelegatedCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, v, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withQuickWinCount(int v) {
        return new WorldState(deepWorkCount, v, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withMaintenanceCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, v, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withMysteryMeatCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, v,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withCalendarBlocked(boolean v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, v, consecutiveHighChaosDays);
    }

    public WorldState recalculateCompliance() {
        boolean compliant = deepWorkCount <= 1 && quickWinCount <= 3 && maintenanceCount <= 3;
        int score = 100 - (deepWorkCount > 1 ? 25 : 0) 
                        - (quickWinCount > 3 ? 10 : 0) 
                        - (maintenanceCount > 3 ? 10 : 0);
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, Math.max(0, score),
            compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withChaosBucket(ChaosBucket bucket) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, bucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withConsecutiveHighChaosDays(int days) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, days);
    }

    // === Convenience methods for GOAP goals ===
    /** Returns discrete chaos score (0-3) from bucket for goal comparisons */
    public int chaosScoreDiscrete() {
        return chaosBucket.ordinalValue;
    }
    
    /** Returns approximate raw chaos score (0-10) for display purposes */
    public double chaosScoreApprox() {
        return switch (chaosBucket) {
            case LOW -> 1.5;
            case MEDIUM -> 4.0;
            case HIGH -> 7.0;
            case CRITICAL -> 9.5;
        };
    }

    /** Composite stress score (0-100) calculated using weighted factors */
    public int calculateStressScore() {
        int stress = 0;

        // === WORKLOAD STRESS (max 40 points) ===
        if (totalAssigned > 7) stress += Math.min(20, (totalAssigned - 7) * 4);
        if (deepWorkCount > 1) stress += (deepWorkCount - 1) * 10;
        if (deepWorkCount == 0 && totalAssigned > 0) stress += 5;

        // === CHAOS STRESS (max 30 points) ===
        stress += chaosBucket.ordinalValue * 10; // 0/10/20/30

        // === CONTEXT SWITCHING STRESS (max 15 points) ===
        if (issuesTouchedToday > 5) stress += Math.min(15, (issuesTouchedToday - 5) * 3);

        // === CLARITY STRESS (max 10 points) ===
        stress += Math.min(10, mysteryMeatCount * 2);
        stress += Math.min(5, unclearQuickWins);

        // === SUSTAINED STRESS (max 15 points) ===
        stress += Math.min(15, consecutiveHighChaosDays * 5);

        // === AFTER-HOURS STRESS (max 10 points) ===
        stress += Math.min(10, issuesUpdatedAfterHours * 5);

        return Math.min(100, stress);
    }

    public StressLevel getStressLevel() {
        int score = calculateStressScore();
        if (score >= 70) return StressLevel.CRITICAL;
        if (score >= 50) return StressLevel.HIGH;
        if (score >= 30) return StressLevel.MODERATE;
        return StressLevel.LOW;
    }

    // === Helper functions for counting issues ===
    private static int countDeepWork(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> hasAnyLabel(i, List.of("priority:critical", "architecture", "deep-work")))
            .count();
    }

    private static int countQuickWins(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> hasAnyLabel(i, List.of("good-first-issue", "quick-win", "size:S")))
            .count();
    }

    private static int countMaintenance(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> hasAnyLabel(i, List.of("dependencies", "documentation", "maintenance", "tech-debt")))
            .count();
    }

    private static int countDeferred(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> hasAnyLabel(i, List.of("deferred", "next-sprint", "backlog")))
            .count();
    }

    private static int countUrgentUnassigned(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> hasAnyLabel(i, List.of("urgent", "priority:critical")))
            .filter(i -> i.assignees() == null || i.assignees().isEmpty())
            .count();
    }

    private static int countContradictory(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> hasAnyLabel(i, List.of("bug")) && hasAnyLabel(i, List.of("enhancement")))
            .count();
    }

    // NOTE: Demo labels ALWAYS win over real timestamps. If issue has demo label, skip timestamp check.
    private static int countTouchedToday(List<Issue> issues, String userId, Clock clock) {
        Instant todayCutoff = clock.instant().minus(Duration.ofHours(8));
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) || 
                         (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null && i.updatedAt().isAfter(todayCutoff)))
            .count();
    }

    private static int countAfterHours(List<Issue> issues, String userId, Clock clock) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS) || 
                         (!DemoLabels.hasDemoLabel(i) && isAfterHours(i.updatedAt(), clock)))
            .count();
    }

    private static int countStale(List<Issue> issues, Clock clock) {
        return (int) issues.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.STALE_14D) || 
                         (!DemoLabels.hasDemoLabel(i) && isStale(i.updatedAt(), 14, clock)))
            .count();
    }

    private static int countMysteryMeat(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> i.body() == null || i.body().isBlank())
            .count();
    }

    private static int countUnclearQuickWins(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> hasAnyLabel(i, List.of("quick-win", "good-first-issue")))
            .filter(i -> i.body() == null || i.body().isBlank())
            .count();
    }

    private static int countAssigned(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .count();
    }

    private static boolean isAssignedTo(Issue issue, String userId) {
        return issue.assignees() != null && 
               issue.assignees().stream().anyMatch(a -> a.login().equalsIgnoreCase(userId));
    }

    private static int roundToFive(int value) {
        return Math.round(value / 5.0f) * 5;
    }

    private static int getConsecutiveHighChaosDays(String userId) {
        // TODO: Retrieve from persistent storage / metrics history
        return 0; // Default for now
    }

    // === Time helper functions (use injected Clock for determinism) ===
    // NOTE: These are only called when issue has NO demo labels
    
    private static boolean isToday(Instant timestamp, Clock clock) {
        if (timestamp == null) return false;
        LocalDate issueDate = timestamp.atZone(clock.getZone()).toLocalDate(); // Use clock's zone
        LocalDate today = LocalDate.now(clock);
        return issueDate.equals(today);
    }

    private static boolean isAfterHours(Instant timestamp, Clock clock) {
        if (timestamp == null) return false;
        // After-hours is determined by the issue's timestamp, using clock's zone
        int hour = timestamp.atZone(clock.getZone()).getHour(); // NOT systemDefault()
        return hour < 9 || hour >= 18; // Before 9am or after 6pm
    }

    private static boolean isStale(Instant timestamp, int days, Clock clock) {
        if (timestamp == null) return true; // No update = stale
        Instant cutoff = clock.instant().minus(Duration.ofDays(days));
        return timestamp.isBefore(cutoff);
    }
}

public enum StressLevel { LOW, MODERATE, HIGH, CRITICAL }

/**
 * Utility class for label matching - used by both WorldState and Action implementations.
 * Statically imported in Action records for convenience.
 */
public final class LabelUtils {
    private LabelUtils() {}

    /** Check if issue has a specific label (case-insensitive) */
    public static boolean hasLabel(Issue issue, String labelName) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(labelName));
    }

    /** Check if issue has ANY of the specified labels (varargs, case-insensitive) */
    public static boolean hasLabel(Issue issue, String... labelNames) {
        if (issue.labels() == null || labelNames.length == 0) return false;
        Set<String> target = Arrays.stream(labelNames)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    /** Check if issue has ANY of the specified labels (List version, case-insensitive) */
    public static boolean hasAnyLabel(Issue issue, List<String> labelNames) {
        if (issue.labels() == null || labelNames.isEmpty()) return false;
        Set<String> target = labelNames.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    /** Check if issue has a label matching a regex pattern */
    public static boolean hasLabelPattern(Issue issue, String regex) {
        if (issue.labels() == null) return false;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return issue.labels().stream()
            .anyMatch(l -> pattern.matcher(l.name()).matches());
    }
}
```

### GOAP Goals (Prioritized)

Goals define desired end states. Each has a **priority weight** for the planner:

```java
public sealed interface Goal permits 
    ReduceChaos, Achieve333Compliance, ProtectDeepWork, 
    ClearMysteryMeat, PreventBurnout, EnableFridayDeploy {
    
    int priority();           // Higher = more urgent
    boolean isSatisfied(WorldState state);
    int insistence(WorldState state); // Dynamic urgency based on state
}

public record ReduceChaos() implements Goal {
    @Override public int priority() { return 80; }
    @Override public boolean isSatisfied(WorldState s) { 
        // Satisfied when chaos is LOW or MEDIUM (bucket 0 or 1)
        return s.chaosBucket().ordinalValue <= 1; 
    }
    @Override public int insistence(WorldState s) {
        // Urgency based on discrete bucket (0=0, 1=20, 2=50, 3=80)
        return s.chaosBucket().ordinalValue * 25;
    }
}

public record Achieve333Compliance() implements Goal {
    @Override public int priority() { return 90; }
    @Override public boolean isSatisfied(WorldState s) { return s.is333Compliant(); }
    @Override public int insistence(WorldState s) {
        return s.is333Compliant() ? 0 : 100 - s.complianceScore();
    }
}

public record ProtectDeepWork() implements Goal {
    @Override public int priority() { return 85; }
    @Override public boolean isSatisfied(WorldState s) { 
        return s.deepWorkCount() == 1 && s.issuesTouchedToday() <= 3; 
    }
    @Override public int insistence(WorldState s) {
        if (s.deepWorkCount() == 0) return 30; // need to pick one
        if (s.deepWorkCount() > 1) return 50;  // too many
        return s.issuesTouchedToday() > 3 ? 40 : 0; // context switching
    }
}

public record ClearMysteryMeat() implements Goal {
    @Override public int priority() { return 60; }
    @Override public boolean isSatisfied(WorldState s) { return s.mysteryMeatCount() == 0; }
    @Override public int insistence(WorldState s) { return s.mysteryMeatCount() * 15; }
}

public record PreventBurnout() implements Goal {
    @Override public int priority() { return 100; } // Highest priority
    @Override public boolean isSatisfied(WorldState s) { 
        return s.calculateStressScore() < 50 && s.consecutiveHighChaosDays() < 3; 
    }
    @Override public int insistence(WorldState s) {
        int stress = s.calculateStressScore();
        if (stress >= 70) return 100; // Emergency
        if (s.consecutiveHighChaosDays() >= 3) return 90; // Sustained stress
        return stress;
    }
}

public record EnableFridayDeploy(Clock clock, boolean demoFridayEnabled) implements Goal {
    @Override public int priority() { return 70; }
    @Override public boolean isSatisfied(WorldState s) { 
        // Use discrete bucket: LOW or MEDIUM is acceptable
        return s.chaosBucket().ordinalValue <= 1 && s.is333Compliant() && s.urgentUnassigned() == 0;
    }
    @Override public int insistence(WorldState s) {
        // Use demo:friday label OR real day check with injected Clock
        boolean isFridayish = demoFridayEnabled || isThuOrFri(clock);
        if (!isFridayish) return 0;
        return isSatisfied(s) ? 0 : 80;
    }
    
    private boolean isThuOrFri(Clock clk) {
        DayOfWeek day = LocalDate.now(clk).getDayOfWeek();
        return day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY;
    }
}
```

### GOAP Actions (With Preconditions & Effects)

Each action has:
- **Preconditions**: World state requirements to execute
- **Effects**: How world state changes after execution
- **Cost**: Used by A* planner to find optimal sequence

```java
// Static import LabelUtils for convenience in Action implementations
import static com.demo.burnout.util.LabelUtils.*;

public sealed interface Action permits 
    DeferIssue, DelegateIssue, ReclassifyAsQuickWin, 
    ReclassifyAsMaintenance, AddScopeToIssue, BlockCalendarTime,
    MarkDeepWorkFocus, SlowIntake, SuggestBreak {
    
    String name();
    
    /**
     * Unique identifier for this action instance. Used by GOAP planner 
     * to prevent applying the same action twice (e.g., deferring issue #12 twice).
     * Format: "ActionType" for singleton actions, "ActionType#issueNumber" for issue-specific.
     */
    String id();
    
    int cost(WorldState state);                    // Lower = preferred
    boolean preconditionsMet(WorldState state);
    WorldState apply(WorldState state);            // Returns new state
    
    /**
     * Returns GitHub mutations for this action.
     * Issue-specific actions use their stored issue; singletons return empty list.
     * No parameter needed - issue is stored in the record.
     */
    List<GitHubAction> toGitHubActions();
}

public record DeferIssue(Issue issue) implements Action {
    @Override public String name() { return "Defer: " + issue.title(); }
    @Override public String id() { return "DeferIssue#" + issue.number(); }
    
    @Override public int cost(WorldState s) { 
        // Cheaper if we're overloaded
        return s.totalAssigned() > 7 ? 5 : 15; 
    }
    
    @Override public boolean preconditionsMet(WorldState s) {
        // Can defer if not the only deep work and not urgent
        return !hasLabel(issue, "urgent") && s.totalAssigned() > 1;
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withTotalAssigned(s.totalAssigned() - 1)
                .withDeferredCount(s.deferredCount() + 1)
                .recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("deferred", "next-sprint")),
            new RemoveLabels(issue.number(), List.of("priority:critical")),
            new Comment(issue.number(), "🛡️ Deferred to protect your focus. Revisit next sprint.")
        );
    }
}

public record ReclassifyAsQuickWin(Issue issue) implements Action {
    @Override public String name() { return "Quick-win: " + issue.title(); }
    @Override public String id() { return "ReclassifyAsQuickWin#" + issue.number(); }
    
    @Override public int cost(WorldState s) { 
        return s.quickWinCount() >= 3 ? 20 : 8; // Costly if already at limit
    }
    
    @Override public boolean preconditionsMet(WorldState s) {
        // Must have clear scope (body field) and not be too complex
        return issue.body() != null && !issue.body().isBlank() 
            && !hasLabel(issue, "priority:critical")
            && s.quickWinCount() < 3;
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withQuickWinCount(s.quickWinCount() + 1)
                .withDeferredCount(s.deferredCount() - 1)
                .recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("quick-win", "size:S")),
            new Comment(issue.number(), "⚡ Reclassified as quick win for today's 3-3-3 plan.")
        );
    }
}

public record MarkDeepWorkFocus(Issue issue) implements Action {
    @Override public String name() { return "Focus: " + issue.title(); }
    @Override public String id() { return "MarkDeepWorkFocus#" + issue.number(); }
    
    @Override public int cost(WorldState s) { 
        return s.deepWorkCount() == 0 ? 5 : 25; // Cheap if no deep work yet
    }
    
    @Override public boolean preconditionsMet(WorldState s) {
        return s.deepWorkCount() == 0 && hasLabel(issue, "priority:critical", "architecture");
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withDeepWorkCount(1).recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("deep-work", "focus")),
            new Comment(issue.number(), "🎯 Marked as today's deep work focus. Protect this time.")
        );
    }
}

public record SlowIntake() implements Action {
    @Override public String name() { return "Slow intake (hide new issues)"; }
    @Override public String id() { return "SlowIntake"; } // Singleton action
    @Override public int cost(WorldState s) { return 10; }
    
    @Override public boolean preconditionsMet(WorldState s) {
        return s.calculateStressScore() >= 70; // Only when critical
    }
    
    @Override public WorldState apply(WorldState s) {
        // Reduces visible chaos by one bucket level, gives breathing room
        ChaosBucket reduced = switch (s.chaosBucket()) {
            case CRITICAL -> ChaosBucket.HIGH;
            case HIGH -> ChaosBucket.MEDIUM;
            case MEDIUM -> ChaosBucket.LOW;
            case LOW -> ChaosBucket.LOW; // Already low, no change
        };
        return s.withChaosBucket(reduced);
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(); // UI-only action, no GitHub mutation
    }
}

public record SuggestBreak() implements Action {
    @Override public String name() { return "Suggest break"; }
    @Override public String id() { return "SuggestBreak"; } // Singleton action
    @Override public int cost(WorldState s) { return 5; } // Low cost, high value
    
    @Override public boolean preconditionsMet(WorldState s) {
        return s.consecutiveHighChaosDays() >= 2 || s.issuesUpdatedAfterHours() > 0;
    }
    
    @Override public WorldState apply(WorldState s) {
        // Models the stress reduction from taking a break
        return s.withConsecutiveHighChaosDays(Math.max(0, s.consecutiveHighChaosDays() - 1));
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(); // UI-only action
    }
}

public record DelegateIssue(Issue issue) implements Action {
    @Override public String name() { return "Delegate: " + issue.title(); }
    @Override public String id() { return "DelegateIssue#" + issue.number(); }
    
    @Override public int cost(WorldState s) { 
        return s.totalAssigned() > 5 ? 8 : 15; // Cheaper when overloaded
    }
    
    @Override public boolean preconditionsMet(WorldState s) {
        // Can delegate if not sole owner and has multiple assignees context
        return s.totalAssigned() > 1 && !hasLabel(issue, "solo", "owner-only");
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withTotalAssigned(s.totalAssigned() - 1)
                .withDelegatedCount(s.delegatedCount() + 1)
                .recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("delegated", "needs-owner")),
            new Comment(issue.number(), "🤝 Marked for delegation. Consider who has bandwidth.")
        );
    }
}

public record ReclassifyAsMaintenance(Issue issue) implements Action {
    @Override public String name() { return "Maintenance: " + issue.title(); }
    @Override public String id() { return "ReclassifyAsMaintenance#" + issue.number(); }
    
    @Override public int cost(WorldState s) { 
        return s.maintenanceCount() >= 3 ? 20 : 6; // Cheaper than quick-win
    }
    
    @Override public boolean preconditionsMet(WorldState s) {
        // Good for routine work, docs, cleanup
        return hasLabel(issue, "documentation", "tech-debt", "cleanup", "routine")
            && s.maintenanceCount() < 3;
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withMaintenanceCount(s.maintenanceCount() + 1)
                .recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("maintenance", "3-3-3")),
            new Comment(issue.number(), "🔧 Classified as maintenance task for 3-3-3 plan.")
        );
    }
}

public record AddScopeToIssue(Issue issue) implements Action {
    @Override public String name() { return "Add scope: " + issue.title(); }
    @Override public String id() { return "AddScopeToIssue#" + issue.number(); }
    
    @Override public int cost(WorldState s) { return 12; }
    
    @Override public boolean preconditionsMet(WorldState s) {
        // Only for mystery meat (no body, unclear labels)
        return (issue.body() == null || issue.body().isBlank()) 
            && !hasLabel(issue, "size:S", "size:M", "size:L", "scope-defined");
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withMysteryMeatCount(s.mysteryMeatCount() - 1)
                .recalculateCompliance();
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(
            new AddLabels(issue.number(), List.of("needs-scope", "blocked")),
            new Comment(issue.number(), "📋 Needs clearer scope before starting. What does 'done' look like?")
        );
    }
}

public record BlockCalendarTime() implements Action {
    @Override public String name() { return "Block calendar for deep work"; }
    @Override public String id() { return "BlockCalendarTime"; } // Singleton
    @Override public int cost(WorldState s) { return 8; }
    
    @Override public boolean preconditionsMet(WorldState s) {
        return s.deepWorkCount() > 0 && !s.calendarBlocked();
    }
    
    @Override public WorldState apply(WorldState s) {
        return s.withCalendarBlocked(true);
    }
    
    @Override public List<GitHubAction> toGitHubActions() {
        return List.of(); // Calendar action, not GitHub
    }
}
```

### GOAP Planner (Cost-Based Greedy)

The planner uses **iterative greedy selection** (NOT full A* - simpler and more predictable for demo):

```java
@Service
public class GOAPPlanner {

    private static final int MAX_ACTIONS = 5; // Budget for demo (prevents runaway plans)

    private final List<Goal> goals;
    private final Clock clock;
    private final boolean demoFridayEnabled;
    private final Comparator<Issue> STABLE_ISSUE_ORDER;

    public GOAPPlanner(Clock clock, @Value("${demo.friday.enabled:false}") boolean demoFridayEnabled) {
        this.clock = clock;
        this.demoFridayEnabled = demoFridayEnabled;
        
        // Goals in priority order (used for iterative satisfaction)
        this.goals = List.of(
            new PreventBurnout(),
            new Achieve333Compliance(),
            new ProtectDeepWork(),
            new ReduceChaos(),
            new EnableFridayDeploy(clock, demoFridayEnabled),
            new ClearMysteryMeat()
        );

        // DETERMINISTIC issue ordering for stable action generation
        this.STABLE_ISSUE_ORDER = Comparator
            .comparing((Issue i) -> getPriorityWeight(i))  // critical > high > normal
            .thenComparing(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ? 0 : 1) // touched first
            .thenComparing(Issue::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Issue::number); // stable tiebreaker
    }

    private int getPriorityWeight(Issue issue) {
        if (hasLabel(issue, "priority:critical")) return 0;
        if (hasLabel(issue, "priority:high")) return 1;
        if (hasLabel(issue, "urgent")) return 1;
        return 2; // normal
    }

    /**
     * FRIDAY PRECEDENCE (pick ONE mechanism, documented):
     * 1. If demo.friday.enabled == true → treat as Friday regardless of clock/labels
     * 2. Else if any issue has demo:friday label → treat as Friday scenario
     * 3. Else use clock day-of-week
     */
    public boolean isFridayScenario(List<Issue> issues) {
        if (demoFridayEnabled) return true;  // Config wins
        if (issues.stream().anyMatch(i -> DemoLabels.hasLabel(i, DemoLabels.FRIDAY))) return true;  // Label wins
        return clock.instant().atZone(clock.getZone()).getDayOfWeek() == DayOfWeek.FRIDAY;  // Real day
    }

    public GoapActionPlan plan(WorldState initial, List<Issue> availableIssues, String userId) {
        // Sort issues deterministically for stable action generation
        List<Issue> sortedIssues = availableIssues.stream()
            .sorted(STABLE_ISSUE_ORDER)
            .toList();

        // Generate all possible actions (deterministic order, filtered to user's open issues)
        List<Action> possibleActions = generateActions(initial, sortedIssues, userId);
        
        // ITERATIVE GOAL SATISFACTION: Plan until top goals satisfied or budget exhausted
        List<Action> allActions = new ArrayList<>();
        WorldState currentState = initial;
        Set<String> appliedIds = new HashSet<>();

        while (allActions.size() < MAX_ACTIONS) {
            // Find highest-priority unsatisfied goal
            Goal targetGoal = goals.stream()
                .filter(g -> !g.isSatisfied(currentState))
                .max(Comparator.comparingInt(g -> g.priority() + g.insistence(currentState)))
                .orElse(null);
            
            if (targetGoal == null) {
                break; // All goals satisfied
            }

            // Greedy: find ONE best action toward this goal
            Action nextAction = findBestAction(currentState, targetGoal, possibleActions, appliedIds);
            if (nextAction == null) {
                break; // No valid action found
            }

            // Apply action, track it, continue
            allActions.add(nextAction);
            appliedIds.add(nextAction.id());
            currentState = nextAction.apply(currentState);
        }
        
        return toActionPlan(allActions, initial);
    }

    private Action findBestAction(WorldState state, Goal goal, List<Action> actions, Set<String> alreadyApplied) {
        // Greedy: find lowest-cost action that makes progress toward goal
        return actions.stream()
            .filter(a -> !alreadyApplied.contains(a.id()))
            .filter(a -> a.preconditionsMet(state))
            .filter(a -> goal.insistence(a.apply(state)) < goal.insistence(state)) // Must make progress
            .min(Comparator.comparingInt(a -> a.cost(state)))
            .orElse(null);
    }

    private GoapActionPlan toActionPlan(List<Action> actions, WorldState initial) {
        if (actions.isEmpty()) return GoapActionPlan.empty();
        
        // Calculate expected state after all actions
        WorldState finalState = actions.stream()
            .reduce(initial, (s, a) -> a.apply(s), (s1, s2) -> s2);
        
        return new GoapActionPlan(actions, initial.calculateStressScore(), finalState.calculateStressScore());
    }

    private List<Action> generateActions(WorldState state, List<Issue> sortedIssues, String userId) {
        List<Action> actions = new ArrayList<>();
        
        // STAGE-SAFE: Only generate actions from open issues assigned to user
        // This prevents nonsense actions and makes plans explainable
        List<Issue> actionableIssues = sortedIssues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equals(userId)))
            .toList();
        
        // Issue-specific actions (in deterministic order from filtered issues)
        for (Issue issue : actionableIssues) {
            actions.add(new DeferIssue(issue));
            actions.add(new ReclassifyAsQuickWin(issue));
            actions.add(new ReclassifyAsMaintenance(issue));
            actions.add(new MarkDeepWorkFocus(issue));
            actions.add(new AddScopeToIssue(issue));
        }
        
        // Singleton actions (no issue)
        actions.add(new SlowIntake());
        actions.add(new SuggestBreak());
        actions.add(new BlockCalendarTime());
        
        return actions;
    }
}

// Renamed from ActionPlan to avoid collision with GitHubMutationPlan
public record GoapActionPlan(
    List<Action> actions,
    int initialStressScore,
    int expectedStressScore
) {
    public static GoapActionPlan empty() {
        return new GoapActionPlan(List.of(), 0, 0);
    }
    
    public boolean isEmpty() { return actions.isEmpty(); }
    
    /** Convert to GitHubMutationPlan for extension to execute */
    public GitHubMutationPlan toMutationPlan(String repo) {
        List<GitHubAction> ghActions = actions.stream()
            .flatMap(a -> a.toGitHubActions().stream())
            .toList();
        return new GitHubMutationPlan(repo, ghActions);
    }
    
    /** Convert to JSON-safe DTO (costs computed at selection time, not serialized as functions) */
    public List<GoapActionSummary> toSummaries(WorldState stateAtSelection) {
        return actions.stream()
            .map(a -> new GoapActionSummary(
                a.id(),
                a.name(),
                a.cost(stateAtSelection)  // Compute once, serialize as int
            ))
            .toList();
    }
}

/** JSON-safe DTO for action summaries (no method references, clean serialization) */
public record GoapActionSummary(
    String id,
    String name,
    int cost
) {}
```

### GOAP Explanation Agent (LangChain4j)

**IMPORTANT**: The LLM does NOT compute GOAP plans. The deterministic `GOAPPlanner` does.
This agent ONLY explains the already-computed plan in human-friendly terms.

```java
// ❌ WRONG: LLM "GOAPAgent" that decides actions - contradicts determinism requirement
// ✅ CORRECT: Deterministic GOAPPlanner (greedy) + LLM explains the result

public interface GoapExplainerAgent {
    @SystemMessage("""
      You are explaining a pre-computed GOAP action plan for burnout prevention.
      
      You will receive:
      - The WORLD STATE (stress signals computed from GitHub issues)
      - The ACTION PLAN (already computed by deterministic A* planner)
      - The PRIMARY GOAL that was targeted
      
      Your job is ONLY to:
      1. Explain WHY the planner chose these actions (in human terms)
      2. Describe the expected BENEFIT of each action
      3. Provide encouragement and context for the user
      
      DO NOT:
      - Suggest different actions than what was planned
      - Re-compute or second-guess the plan
      - Make any decisions - just explain
      
      Keep it brief and actionable (2-3 sentences per action).
      """)
    String explain(@V("worldState") WorldState state, 
                   @V("actionPlan") GoapActionPlan plan,
                   @V("primaryGoal") Goal goal);
}
```
```

### Updated Agent Workflow (with GOAP)

```java
// Build world state from cached issues
WorldState worldState = worldStateBuilder.build(issueCache.get(repo), userId);

// GOAP planning for stress-aware action selection
GOAPResult goapResult = goapAgent.plan(worldState, issues);

// Integrate with day planner
UntypedAgent burnoutAgent = AgenticServices.sequenceBuilder()
    .subAgents(fetcher, chaosAnalyzer, goapPlanner, dayPlanner, protectiveResponder)
    .outputKey("dayPlan")
    .listener(listener)
    .build();
```

### /api/stress endpoint (deterministic GOAP metrics)

```java
@RestController
public class StressController {
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ThreeThreeThreeComplianceService complianceService;
    private final GOAPPlanner goapPlanner;
    private final Clock clock;

    @GetMapping("/api/stress")
    public StressResponse stress(@RequestParam String repo, @RequestParam String userId) {
        if (!issueCache.hasRepo(repo)) {
            return StressResponse.notSynced();
        }
        
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
        
        // GOAP planning (deterministic greedy - NOT LLM)
        GoapActionPlan actionPlan = goapPlanner.plan(state, issues, userId);  // Pass userId
        
        return new StressResponse(
            state.calculateStressScore(),
            state.getStressLevel(),
            Map.of(
                "workload", calculateWorkloadStress(state),
                "chaos", state.chaosBucket().ordinalValue * 10,
                "contextSwitching", Math.min(15, Math.max(0, state.issuesTouchedToday() - 5) * 3),
                "clarity", Math.min(10, state.mysteryMeatCount() * 2),
                "sustained", Math.min(15, state.consecutiveHighChaosDays() * 5),
                "afterHours", Math.min(10, state.issuesUpdatedAfterHours() * 5)
            ),
            state.is333Compliant(),
            actionPlan
        );
    }

    private int calculateWorkloadStress(WorldState state) {
        int stress = 0;
        if (state.totalAssigned() > 7) stress += Math.min(20, (state.totalAssigned() - 7) * 4);
        if (state.deepWorkCount() > 1) stress += (state.deepWorkCount() - 1) * 10;
        if (state.deepWorkCount() == 0 && state.totalAssigned() > 0) stress += 5;
        return Math.min(40, stress);
    }
}

public record StressResponse(
    int stressScore,
    StressLevel stressLevel,
    Map<String, Integer> breakdown,
    boolean is333Compliant,
    GoapActionPlan actionPlan
) {
    public static final int SCHEMA_VERSION = 1;
    public static StressResponse notSynced() {
        return new StressResponse(-1, StressLevel.LOW, Map.of(), false, GoapActionPlan.empty());
    }
    public boolean isSynced() { return stressScore >= 0; }
    public int schemaVersion() { return SCHEMA_VERSION; }
}
```

---

## Backend mutations: ActionPlan (extension executes)

**Important**: backend does not mutate GitHub. It returns an `ActionPlan`.

```java
// Type discriminator for Jackson serialization (extension switches on action.type)
public sealed interface GitHubAction permits AddLabels, RemoveLabels, Comment {
    int issueNumber();
    String type();  // Explicit discriminator for predictable JSON
}

public record AddLabels(int issueNumber, List<String> labels) implements GitHubAction {
    @Override public String type() { return "AddLabels"; }
}
public record RemoveLabels(int issueNumber, List<String> labels) implements GitHubAction {
    @Override public String type() { return "RemoveLabels"; }
}
public record Comment(int issueNumber, String body) implements GitHubAction {
    @Override public String type() { return "Comment"; }
}

// Renamed from ActionPlan to avoid collision with GoapActionPlan
// Field is "actions" (not "mutations") to match TS consumer: actionPlan.actions
public record GitHubMutationPlan(String repo, List<GitHubAction> actions) {
    public static final int SCHEMA_VERSION = 1; // Increment on breaking changes
    public static GitHubMutationPlan empty() { return new GitHubMutationPlan("", List.of()); }
    public boolean isEmpty() { return actions.isEmpty(); }
    public int schemaVersion() { return SCHEMA_VERSION; }
}
```

Extension executes GitHubMutationPlan via `gh`:

```ts
// === TYPE DEFINITIONS (aligned with Java GitHubMutationPlan) ===
interface GitHubMutationPlan {
  repo: string;
  actions: GitHubAction[];  // Field name matches Java record
  schemaVersion: number;    // Drift detection
}

type GitHubAction = 
  | { type: 'AddLabels'; issueNumber: number; labels: string[] }
  | { type: 'RemoveLabels'; issueNumber: number; labels: string[] }
  | { type: 'Comment'; issueNumber: number; body: string };

const EXPECTED_SCHEMA_VERSION = 1;

// Validate type before switching (catches API drift)
function isValidActionType(type: string): type is GitHubAction['type'] {
  return ['AddLabels', 'RemoveLabels', 'Comment'].includes(type);
}

export async function executeMutationPlan(plan: GitHubMutationPlan) {
  // Schema version check (catches backend/extension drift)
  if (plan.schemaVersion !== EXPECTED_SCHEMA_VERSION) {
    throw new Error(`Schema version mismatch: expected ${EXPECTED_SCHEMA_VERSION}, got ${plan.schemaVersion}. Rebuild extension.`);
  }

  for (const action of plan.actions) {
    // VALIDATE discriminator before switching
    if (!isValidActionType(action.type)) {
      console.warn(`Unknown action type: ${action.type}`);
      continue;
    }
    
    try {
      switch (action.type) {
        case 'AddLabels': 
          // IDEMPOTENT: Skip if labels already present (read from cache)
          const currentLabels = await getIssueLabels(plan.repo, action.issueNumber);
          const labelsToAdd = action.labels.filter(l => !currentLabels.includes(l));
          if (labelsToAdd.length > 0) {
            await addLabels(plan.repo, action.issueNumber, labelsToAdd);
          }
          break;
        case 'RemoveLabels': 
          // IDEMPOTENT: Skip if labels already absent
          const existingLabels = await getIssueLabels(plan.repo, action.issueNumber);
          const labelsToRemove = action.labels.filter(l => existingLabels.includes(l));
          if (labelsToRemove.length > 0) {
            await removeLabels(plan.repo, action.issueNumber, labelsToRemove);
          }
          break;
        case 'Comment': 
          await comment(plan.repo, action.issueNumber, action.body); 
          break;
        default:
          const _exhaustive: never = action;
          throw new Error(`Unhandled action type: ${(_exhaustive as any).type}`);
      }
    } catch (e) {
      // SOFT FAIL: Log and continue (don't abort whole plan on one failure)
      console.error(`Action failed (continuing): ${action.type} #${action.issueNumber}`, e);
    }
  }
  
  // === POST-MUTATION CONSISTENCY DELAY ===
  // GitHub's API has eventual consistency - labels may not appear immediately.
  // Wait before resync to avoid showing stale state.
  if (plan.actions.length > 0) {
    await new Promise(resolve => setTimeout(resolve, 500)); // 500ms safety margin
  }
}

/** 
 * Targeted resync: Only refetch mutated issues instead of full list.
 * Faster and more reliable than waiting + full refresh.
 */
async function resyncMutatedIssues(plan: GitHubMutationPlan): Promise<Issue[]> {
  const mutatedNumbers = [...new Set(plan.actions.map(a => a.issueNumber))];
  
  const refreshed: Issue[] = [];
  for (const num of mutatedNumbers) {
    const issue = await ghJson<Issue>(
      ['issue', 'view', String(num), '-R', plan.repo, '--json', 
       'number,title,body,labels,assignees,createdAt,updatedAt,state,milestone']
    );
    refreshed.push(issue);
  }
  
  return refreshed;
}
```

---

## Phase 4: Emotionally Supportive Agent (Plutchik)

**Goal**: detect emotional / stress signals and respond protectively.

Signals (examples):

- Language: sentiment on comments (“stuck”, “frustrating”)  
- Behaviour: rapid switching, late-night activity  
- Workload: sustained high chaos score across days  

### Sustained chaos tracking (seeded for demo)

```java
@Service
public class ChaosHistoryService {
    private final Map<String, List<DailyChaos>> history = new ConcurrentHashMap<>();
    private final Clock clock; // Inject for determinism

    public ChaosHistoryService(Clock clock) {
        this.clock = clock;
    }

    public void recordChaos(String userId, ChaosMetrics chaos) {
        LocalDate today = LocalDate.now(clock);
        history.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(new DailyChaos(today, chaos.score()));
    }

    public boolean isSustainedHighChaos(String userId, int days, double threshold) {
        LocalDate cutoff = LocalDate.now(clock).minusDays(days);
        List<DailyChaos> recent = history.getOrDefault(userId, List.of()).stream()
            .filter(d -> d.date().isAfter(cutoff))
            .toList();
        return recent.size() >= days && recent.stream().allMatch(d -> d.score() > threshold);
    }
}
```

### ProtectiveResponder (Deterministic + LLM Narration)

```java
public class ProtectivePolicyService {
    
    public ProtectiveAction computeAction(EmotionalSignals signals, ChaosMetrics chaos) {
        // Rule-based, predictable, stage-safe (LLM does NOT decide)
        if (chaos.score() >= 8) {
            return new ProtectiveAction("slowIntake", "Chaos is critical. Hiding new issues temporarily.");
        }
        if (signals.sustainedHighStressDays() >= 3) {
            return new ProtectiveAction("promptForHelp", "3+ days of high stress. Consider team lead.");
        }
        if (signals.emotionalTone() == EmotionalTone.FRUSTRATED) {
            return new ProtectiveAction("offerQuickWin", "Detected frustration. Here's an easy win.");
        }
        return ProtectiveAction.none();
    }
}

public record ProtectiveAction(String action, String fallbackMessage) {
    public static ProtectiveAction none() { return new ProtectiveAction("none", ""); }
    public boolean isActive() { return !"none".equals(action); }
}
```

Extension shows a gentle toast: “Agent is protecting you” + reason.

---

## Phase 5: Friday Deploy Confidence

**Goal**: make “deploy on a Friday” calm and repeatable with objective readiness checks.

Friday readiness score (deterministic):

- ✅ all critical issues resolved  
- ✅ no stale urgents (>24h)  
- ✅ deep work completed for the week  
- ✅ chaos score < 5  
- ✅ no after-hours signals in last 48h  
- **Score 0–100** with color coding

Extension status bar: `$(rocket)` green when score > 80.

Optional “Fix the Issue” moment: CodeAnalyzer agent suggests a diff, human approves before applying.

---

## Endpoints (Deterministic vs Agentic)

All GET endpoints must be deterministic (no LLM calls). Only reshape runs agents.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/issues/sync` | Extension syncs issues to backend cache |
| GET | `/api/chaos?repo=...` | Deterministic chaos score |
| GET | `/api/chaos/details?repo=...` | Deterministic breakdown for webview |
| GET | `/api/compliance?repo=...&userId=...` | 3-3-3 compliance analysis with violations |
| GET | `/api/stress?repo=...&userId=...` | **NEW**: GOAP-based stress/burnout/workload metric |
| POST | `/api/reshape` | Runs GOAP + agent workflow → returns `{ dayPlan, actionPlan, stressReport, goapActions, steps }` |
| GET | `/api/friday-score?repo=...` | Deterministic deploy readiness score |
| GET | `/api/calendar` | Mock calendar fragmentation |
| WS | `/ws/agent` | Stream agent steps (CALLING/RESULT) |

### Stress Response Example (GOAP)

```json
{
  "stressScore": 72,
  "stressLevel": "CRITICAL",
  "breakdown": {
    "workload": 24,
    "chaos": 21,
    "contextSwitching": 12,
    "clarity": 6,
    "sustained": 5,
    "afterHours": 4
  },
  "is333Compliant": false,
  "schemaVersion": 1,
  "actionPlan": {
    "actions": [
      { "id": "DeferIssue#15", "name": "Defer: Fix N+1 query", "cost": 5 },
      { "id": "MarkDeepWorkFocus#18", "name": "Focus: Security audit", "cost": 5 }
    ],
    "initialStressScore": 72,
    "expectedStressScore": 52
  }
}
```

### Compliance Response Example

```json
{
  "userId": "roryp",
  "isCompliant": false,
  "complianceScore": 50,
  "schemaVersion": 1,
  "bucketCounts": {
    "deepWork": 3,
    "quickWins": 2,
    "maintenance": 1,
    "deferred": 5
  },
  "violations": [
    {
      "type": "MULTIPLE_DEEP_WORK",
      "severity": "CRITICAL",
      "message": "You have 3 deep-work issues active. Max is 1.",
      "field": "labels",
      "affectedIssues": [12, 15, 18],
      "recommendation": "Pick ONE critical issue. Move others to next sprint or delegate."
    },
    {
      "type": "EXCESSIVE_CONTEXT_SWITCHING",
      "severity": "CRITICAL", 
      "message": "You've touched 7 issues today. High context-switch cost.",
      "field": "updatedAt",
      "recommendation": "Focus on completing one issue before moving to the next."
    }
  ]
}
```

---

## Demo Flow (45 minutes)

1) **0–8 min (The Noise)**  
- Open VS Code → chaos already high  
- Click breakdown → show mystery meat, contradictory labels  
- “This is what burnout looks like in our tools.”

2) **8–15 min (The Stress)**  
- Show fragmented calendar  
- Introduce WCAG → anti-burnout heuristics  
- “Knowing isn’t enough. Let me ask for help.”

3) **15–30 min (Ask the Agent)**  
- Run `Burnout: Reshape My Day`  
- Watch Agent Explains stream decisions  
- Wheel animates into 3-3-3 structure  
- Pop hood: LangChain4j workflow + tools + guardrails

4) **30–40 min (Emotional Protection)**  
- Show seeded 3-day high chaos history  
- Agent triggers protective mode  
- Toast explains “why”

5) **40–45 min (Friday Deploy)**  
- Friday score turns green rocket  
- “This is how we make Friday deploys calm.”

---

## Minimum Viable Demo (panic button mode)

1. Show seeded chaotic backlog — 2 min  
2. Run reshape — 3 min  
3. Agent Explains + wheel animation — 5 min  
4. Show final 3-3-3 plan — 2 min  
5. Friday score green — 1 min  

Total: **13 minutes** core demo.

---

## Stage Controls (Extension Commands)

### Preflight Check Command

```ts
// Command: "Burnout: Preflight Check"
async function preflightCheck(): Promise<void> {
  const checks: { name: string; check: () => Promise<boolean> }[] = [
    { name: "gh CLI installed", check: async () => {
      try { await gh(["--version"]); return true; } catch { return false; }
    }},
    { name: "gh authenticated", check: async () => {
      try { 
        // Use API call - succeeds only if authenticated (no text parsing)
        await gh(["api", "user", "-q", ".login"]); 
        return true; 
      } catch { return false; }
    }},
    { name: "Repo exists", check: async () => {
      try { 
        // Use --jq for repo view (not -q)
        await gh(["repo", "view", REPO, "--json", "name", "--jq", ".name"]); 
        return true; 
      } catch { return false; }
    }},
    { name: "Backend reachable", check: async () => {
      try { 
        const res = await fetch("http://localhost:8080/actuator/health");
        return res.ok; 
      } catch { return false; }
    }},
    { name: "Issues seeded", check: async () => {
      const issues = await fetchIssues(REPO);
      return issues.length >= 10;
    }},
    { name: "Required labels exist", check: async () => {
      // Must-exist labels for demo to work correctly
      const REQUIRED_LABELS = [
        // Demo scenario labels
        'demo:stale-14d', 'demo:after-hours', 'demo:touched-today', 'demo:friday',
        // Bucket classification labels  
        'deep-work', 'quick-win', 'maintenance',
        // Priority/urgency labels
        'urgent', 'priority:critical',
        // Focus/deferral labels
        'focus', 'deferred'
      ];
      
      try {
        const stdout = await gh(['label', 'list', '-R', REPO, '--json', 'name']);
        const existingLabels: string[] = JSON.parse(stdout).map((l: {name: string}) => l.name);
        const missing = REQUIRED_LABELS.filter(l => !existingLabels.includes(l));
        
        if (missing.length > 0) {
          console.log(`Missing labels: ${missing.join(', ')}`);
          console.log('Run: npm run setup:labels  (or Step 1.5 in plan.md)');
          return false;
        }
        return true;
      } catch {
        return false;
      }
    }},
  ];

  let allPassed = true;
  for (const { name, check } of checks) {
    const passed = await check();
    console.log(`${passed ? "✅" : "❌"} ${name}`);
    if (!passed) allPassed = false;
  }

  if (allPassed) {
    vscode.window.showInformationMessage("✅ Preflight: All systems ready!");
  } else {
    vscode.window.showErrorMessage("❌ Preflight: Some checks failed. See output.");
  }
}
```

### Demo Mode Toggle

```ts
// Command: "Burnout: Toggle Demo Mode"
let demoModeEnabled = false;

function toggleDemoMode() {
  demoModeEnabled = !demoModeEnabled;
  
  if (demoModeEnabled) {
    // 1. Stop polling
    clearInterval(chaosPollingInterval);
    
    // 2. Freeze repo/user selection
    frozenRepo = REPO;
    frozenUserId = "roryp";
    
    // 3. Show manual refresh button in status bar
    demoRefreshButton.show();
    
    // 4. Lock sidebar to prevent accidental clicks
    vscode.commands.executeCommand("setContext", "burnout.demoMode", true);
    
    vscode.window.showInformationMessage("🎭 Demo Mode ON: Polling stopped, using manual refresh");
  } else {
    // Resume normal operation
    chaosPollingInterval = setInterval(updateChaosStatus, 10000);
    demoRefreshButton.hide();
    vscode.commands.executeCommand("setContext", "burnout.demoMode", false);
    
    vscode.window.showInformationMessage("Demo Mode OFF: Resuming normal operation");
  }
}
```

### Dry Run / Apply Split (Stage Safety Bailout)

```ts
// Two commands: Preview shows plan without mutations, Apply executes
// This gives you a clean bailout if room energy changes

// Command: "Burnout: Reshape My Day (Preview)"
async function reshapePreview(): Promise<void> {
  const result = await fetch(`${BACKEND_URL}/api/reshape`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repo: REPO, userId: USER_ID, dryRun: true })
  });
  
  const rawData = await result.json();
  
  // VALIDATE SCHEMA on all response types
  const data = {
    ...rawData,
    chaos: validateSchemaVersion(rawData.chaos, 'ChaosMetrics'),
    compliance: validateSchemaVersion(rawData.compliance, 'ComplianceReport'),
    actionPlan: validateSchemaVersion(rawData.actionPlan, 'GitHubMutationPlan'),
  };
  
  // Show wheel + planned mutations (read-only)
  showWheelWebview(data.dayPlan);
  showMutationPreview(data.actionPlan);  // "These labels would be added..."
  
  // Enable "Apply Plan" button
  vscode.commands.executeCommand("setContext", "burnout.hasPendingPlan", true);
  pendingPlan = data.actionPlan;
  
  vscode.window.showInformationMessage(
    `📋 Preview: ${data.actionPlan.actions.length} actions planned. Click "Apply" to execute.`
  );
}

// Command: "Burnout: Apply Plan"
async function applyPlan(): Promise<void> {
  if (!pendingPlan || pendingPlan.actions.length === 0) {
    vscode.window.showWarningMessage("No pending plan. Run 'Reshape My Day (Preview)' first.");
    return;
  }
  
  // Disable polling during mutations (prevent race with resync)
  const wasPolling = chaosPollingInterval !== null;
  if (wasPolling) clearInterval(chaosPollingInterval);
  
  try {
    await executeMutationPlan(pendingPlan);
    
    // Resync after mutations
    await syncIssues(REPO);
    
    vscode.window.showInformationMessage(`✅ Applied ${pendingPlan.actions.length} actions. Day reshaped!`);
  } finally {
    // Clear pending plan
    pendingPlan = null;
    vscode.commands.executeCommand("setContext", "burnout.hasPendingPlan", false);
    
    // Resume polling if it was active
    if (wasPolling) {
      chaosPollingInterval = setInterval(updateChaosStatus, 10000);
    }
  }
}

let pendingPlan: GitHubMutationPlan | null = null;
```

---

## Key Architecture Decision: Deterministic Decisions, LLM Narration

**Critical for stage safety**: The agent's *decisions* must be deterministic. The LLM only *explains* them.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SEPARATION OF CONCERNS                                │
│                                                                          │
│  DETERMINISTIC (always correct)          LLM (nice-to-have narration)   │
│  ─────────────────────────────           ────────────────────────────   │
│  • IssueClassifierService                • "Explain this JSON to a      │
│  • ThreeThreeThreeComplianceService        human in friendly terms"     │
│  • WorldState.calculateStressScore()     • Streaming explanation UI     │
│  • GOAPPlanner.plan() → ActionPlan       • Emotional tone detection     │
│  • FridayScoreService                                                   │
│                                                                          │
│  If LLM fails → UI still correct         If LLM fails → show raw JSON   │
└─────────────────────────────────────────────────────────────────────────┘
```

**Implementation pattern**:

```java
// 1. Compute decisions DETERMINISTICALLY
DeterministicResult result = new DeterministicResult(
    classifier.classify(issues),           // bucket assignments
    complianceService.analyze(issues),     // violations
    worldState.calculateStressScore(),     // stress metric
    goapPlanner.plan(worldState, issues)   // action sequence
);

// 2. Ask LLM to EXPLAIN (non-blocking, fallback-safe)
String explanation;
try {
    explanation = explainerAgent.explain(result); // "Explain this JSON to a human"
} catch (Exception e) {
    explanation = "See details below."; // Graceful fallback
}

// 3. Return both (UI shows explanation, but decisions are already locked in)
return new ReshapeResponse(result, explanation);
```

This means:
- ✅ Wheel always shows correct 3-3-3 buckets
- ✅ ActionPlan always has correct GitHub mutations
- ✅ Stress score always computed correctly
- ⚠️ If LLM goes weird, just show "See details" — demo still works

---

## Pre-demo Setup Checklist

- [ ] `roryp/burnout-demo` exists with seeded issues  
- [ ] Demo labels applied (use canonical names from `DemoLabels`):
  - `demo:stale-14d` (staleness signal)
  - `demo:after-hours` (late-night work signal)
  - `demo:touched-today` (context switching signal)
  - `demo:friday` (Friday deploy scenario)
- [ ] `gh api user -q .login` succeeds (authenticated)  
- [ ] Backend running: `mvn spring-boot:run -Dspring.profiles.active=demo` (8080)  
- [ ] Extension loaded in debug mode  
- [ ] **Run "Burnout: Preflight Check" command — all green**
- [ ] Model key configured in `application.yml`  
- [ ] Seeded chaos history JSON loaded  
- [ ] D3 bundle works in webview (no CDN dependency)  
- [ ] ActionPlan execution uses `execFile` (no shell quoting issues)  
- [ ] **Demo Mode toggle tested**  
- [ ] Clock set to demo time OR demo labels applied to issues  

---

## Step Summary (consistent)

| # | Phase | Step | Tech |
|---|---|---|---|
| 1 | Noisy Day | Create demo repo | gh CLI |
| 2 | Noisy Day | Seed chaotic issues | gh CLI script |
| 3 | Noisy Day | Issue sync + IssueCache + ChaosMetricsService | TS (gh) → Java (cache + deterministic) |
| 4 | Noisy Day | Chaos status bar (sync → chaos poll) | TS (calls Java) |
| 5 | Noisy Day | Mock calendar | Java |
| 6 | WCAG Rules | BurnoutHeuristicsService | Java |
| 7 | WCAG Rules | DayStructure model (3-3-3 limits) | Java |
| 8 | WCAG Rules | IssueClassifierService (multi-field analysis) | Java |
| 9 | WCAG Rules | ThreeThreeThreeComplianceService | Java |
| 10 | WCAG Rules | D3.js wheel webview | TS |
| 11 | Agent | LangChain4j dependencies | pom.xml |
| 12 | Agent | Agent interfaces (IssueFetcher, ChaosAnalyzer, DayPlanner) | Java |
| 13 | Agent | WorldState record (burnout signals from GitHub fields) | Java |
| 14 | Agent | GOAP Goals (PreventBurnout, Achieve333Compliance, etc.) | Java |
| 15 | Agent | GOAP Actions (DeferIssue, MarkDeepWorkFocus, etc.) | Java |
| 16 | Agent | GOAPPlanner (Cost-Based Greedy) | Java |
| 17 | Agent | GOAPAgent interface | Java |
| 18 | Agent | Sequential workflow with GOAP | Java |
| 19 | Agent | Supervisor pattern | Java |
| 20 | Agent | Custom BurnoutPlanner | Java |
| 21 | Agent | P2P pattern | Java |
| 22 | Agent | ExplainingListener (per-request) | Java |
| 23 | Agent | REST controllers (/api/stress, /api/compliance) | Java |
| 24 | Agent | Reshape command (returns dayPlan + actionPlan + stressReport) | TS → Java → TS(gh) |
| 25 | Emotion | EmotionalSignalService | Java |
| 26 | Emotion | EmotionMapper (Plutchik) | Java |
| 27 | Emotion | ProtectivePolicyService (deterministic) | Java |
| 28 | Emotion | Toast notification | TS |
| 29 | Friday | FridayScoreService | Java |
| 30 | Friday | Friday status bar | TS |
| 31 | Friday | Deploy checklist webview | TS |
| 32 | Friday | CodeAnalyzer agent | Java |

---

## MVP Path (Fastest to Shippable Demo)

**If time is tight**, implement only this spine — everything else layers on without breaking it:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MVP CRITICAL PATH                                │
│                                                                          │
│  1. sync         POST /api/issues/sync                                   │
│       ↓                                                                  │
│  2. chaos        GET /api/chaos → status bar color                       │
│       ↓                                                                  │
│  3. compliance   GET /api/compliance → violations list                   │
│       ↓                                                                  │
│  4. reshape      POST /api/reshape → { dayPlan, actionPlan }             │
│       ↓                                                                  │
│  5. execute      Extension runs actionPlan via gh CLI                    │
│       ↓                                                                  │
│  6. resync       POST /api/issues/sync (refresh cache)                   │
│       ↓                                                                  │
│  7. friday       GET /api/friday-score → green rocket                    │
│                                                                          │
│  UI: wheel + single toast ("Day reshaped!")                              │
└─────────────────────────────────────────────────────────────────────────┘
```

**What to defer**:
- Calendar fragmentation → nice visual, not core
- Plutchik emotional mapping → layer on top of stress score later
- GOAP deep-dive → mention by name ("cost-based planner"), show one result card
- Copilot Chat surface → keep as appendix/bonus
- CodeAnalyzer agent → phase 2

**MVP acceptance criteria**:
- [ ] Chaos score shows in status bar (red/yellow/green)
- [ ] "Reshape My Day" command produces visible 3-3-3 wheel
- [ ] ActionPlan executes (labels appear on GitHub issues)
- [ ] Friday score flips green after reshape
- [ ] Demo mode toggle works (no accidental changes mid-talk)
- [ ] Preflight check passes

**Estimated MVP time**: 2-3 focused days for extension + backend skeleton, 1 day for polish.
