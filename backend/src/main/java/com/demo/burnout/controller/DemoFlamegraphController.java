package com.demo.burnout.controller;

import com.demo.burnout.agent.AgentOrchestrator;
import com.demo.burnout.agent.supervisor.BurnoutSupervisorService;
import com.demo.burnout.goap.*;
import com.demo.burnout.model.*;
import com.demo.burnout.service.*;
import com.demo.burnout.util.DemoLabels;
import com.demo.burnout.util.LabelUtils;
import com.demo.burnout.service.StressSnapshotService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only, unauthenticated endpoint that serves flamegraph data
 * for repos already synced via MCP. Designed for live demos where
 * audience members can view the flamegraph in a browser without
 * needing a GitHub token.
 * 
 * Security: No mutations are performed. Only pre-synced, read-only 
 * issue data is returned. No GitHub tokens are required or accepted.
 * The /demo/api/sync endpoint fetches public GitHub issues (unauthenticated)
 * and is rate-limited to 1 sync per repo per 5 minutes.
 */
@RestController
@RequestMapping("/demo/api")
@CrossOrigin(origins = "*")
public class DemoFlamegraphController {

    private static final Logger log = LoggerFactory.getLogger(DemoFlamegraphController.class);
    private static final Duration SYNC_COOLDOWN = Duration.ofMinutes(5);

    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final IssueClassifierService classifier;
    private final StressSnapshotService snapshotService;
    private final BurnoutSupervisorService supervisorService;
    private final AgentOrchestrator agentOrchestrator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, Instant> lastSyncTimes = new ConcurrentHashMap<>();

    public DemoFlamegraphController(IssueCache issueCache,
                                    ChaosMetricsService chaosMetricsService,
                                    ComplianceService complianceService,
                                    IssueClassifierService classifier,
                                    StressSnapshotService snapshotService,
                                    BurnoutSupervisorService supervisorService,
                                    AgentOrchestrator agentOrchestrator,
                                    Clock clock,
                                    ObjectMapper objectMapper) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.classifier = classifier;
        this.snapshotService = snapshotService;
        this.supervisorService = supervisorService;
        this.agentOrchestrator = agentOrchestrator;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Seed test data for demo/testing purposes. Accepts issues and stores
     * them in the IssueCache so the flamegraph can be viewed without
     * requiring GitHub authentication or the MCP sync flow.
     */
    @PostMapping("/seed")
    public Map<String, Object> seedData(@RequestBody SeedRequest request) {
        issueCache.put(request.repo(), request.issues(), Instant.now());
        return Map.of(
            "status", "seeded",
            "repo", request.repo(),
            "issueCount", request.issues().size()
        );
    }

    public record SeedRequest(String repo, List<Issue> issues) {}

    /**
     * Sync issues directly from GitHub's public REST API. No authentication
     * required (works for public repos only). Rate-limited to 1 sync per
     * repo per 5 minutes to avoid exhausting GitHub's unauthenticated
     * rate limit (60 req/hour per IP).
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncFromGitHub(@RequestParam String repo) {
        // Validate repo format
        if (!repo.matches("^[\\w.-]+/[\\w.-]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid repo format. Use owner/repo"));
        }

        // Rate limiting: 1 sync per repo per 5 minutes
        Instant lastSync = lastSyncTimes.get(repo);
        if (lastSync != null) {
            Duration elapsed = Duration.between(lastSync, Instant.now());
            if (elapsed.compareTo(SYNC_COOLDOWN) < 0) {
                long remainingSecs = SYNC_COOLDOWN.minus(elapsed).toSeconds();
                return ResponseEntity.status(429).body(Map.of(
                    "error", "rate_limited",
                    "message", "Sync for " + repo + " is rate-limited. Try again in " + remainingSecs + "s",
                    "retryAfterSeconds", remainingSecs
                ));
            }
        }

        try {
            List<Issue> issues = fetchGitHubIssues(repo);
            issueCache.put(repo, issues, Instant.now());
            lastSyncTimes.put(repo, Instant.now());

            log.info("Demo sync: fetched {} issues from GitHub for {}", issues.size(), repo);

            return ResponseEntity.ok(Map.of(
                "status", "synced",
                "repo", repo,
                "issueCount", issues.size()
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "error", e.getReason() != null ? e.getReason() : "Unknown error"
            ));
        } catch (Exception e) {
            log.error("Failed to sync from GitHub for repo {}", repo, e);
            return ResponseEntity.status(502).body(Map.of(
                "error", "Failed to fetch issues from GitHub: " + e.getMessage()
            ));
        }
    }

    private List<Issue> fetchGitHubIssues(String repo) throws Exception {
        // Fetch up to 100 open issues from GitHub's public REST API (no auth)
        String url = "https://api.github.com/repos/" + repo + "/issues?state=open&per_page=100";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "burnout-app-demo")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Repository not found: " + repo + ". Make sure it's a public repo.");
        }
        if (response.statusCode() == 403) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "GitHub API rate limit exceeded. Try again later.");
        }
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "GitHub API returned " + response.statusCode());
        }

        // Parse GitHub response into Issue records
        GitHubIssue[] ghIssues = objectMapper.readValue(response.body(), GitHubIssue[].class);
        return java.util.Arrays.stream(ghIssues)
            .filter(i -> i.pullRequest == null)  // Exclude PRs (GitHub includes them in /issues)
            .map(GitHubIssue::toIssue)
            .toList();
    }

    /** Maps GitHub REST API issue fields to our Issue model. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubIssue(
        int number,
        String title,
        String body,
        List<GitHubLabel> labels,
        List<GitHubAssignee> assignees,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        String state,
        GitHubMilestone milestone,
        @JsonProperty("pull_request") Object pullRequest
    ) {
        Issue toIssue() {
            return new Issue(
                number, title, body,
                labels == null ? List.of() : labels.stream().map(l -> new Issue.Label(l.name())).toList(),
                assignees == null ? List.of() : assignees.stream().map(a -> new Issue.Assignee(a.login())).toList(),
                createdAt, updatedAt, state,
                milestone == null ? null : new Issue.Milestone(milestone.title(), milestone.dueOn())
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubLabel(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubAssignee(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubMilestone(String title, @JsonProperty("due_on") Instant dueOn) {}

    @GetMapping("/flamegraph")
    public FlamegraphResponse flamegraph(@RequestParam String repo,
                                         @RequestParam(defaultValue = "") String userId) {
        if (!issueCache.hasRepo(repo)) {
            return FlamegraphResponse.notSynced(repo);
        }

        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
        DayStructure dayPlan = buildDayPlan(issues, userId);

        int fridayScore = calculateFridayScore(chaos, compliance, state);

        return new FlamegraphResponse(
            "ok",
            repo,
            dayPlan,
            state.calculateStressScore(),
            state.getStressLevel(),
            fridayScore,
            compliance.isCompliant(),
            issues.size()
        );
    }

    @GetMapping("/repos")
    public List<String> syncedRepos() {
        // Return repos currently in the cache so the UI can offer a dropdown
        return issueCache.getSyncedRepos();
    }

    /**
     * Demo reshape endpoint. Runs the same supervisor/reshape logic as /api/reshape
     * but without auth, and applies the mutation plan (label changes) to the
     * in-memory IssueCache instead of GitHub.
     */
    @PostMapping("/reshape")
    public ResponseEntity<Map<String, Object>> reshape(@RequestBody ReshapeRequest req) {
        String repo = req.repo();
        String userId = req.userId() != null ? req.userId() : "roryp";

        if (!issueCache.hasRepo(repo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Repo not synced. Seed issues first."));
        }

        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);

        int beforeScore = state.calculateStressScore();

        // Run supervisor pattern — same as ReshapeController
        var supervisorResult = supervisorService.preventBurnout(
            state, issues, userId, repo, chaos);

        GitHubMutationPlan mutationPlan = supervisorResult.mutationPlan();

        // Apply mutations to the IssueCache (in-memory) instead of GitHub
        List<Issue> mutatedIssues = applyMutationsToIssues(issues, mutationPlan);
        issueCache.put(repo, mutatedIssues, Instant.now(clock));

        // Recalculate stress after mutations
        ChaosMetrics afterChaos = chaosMetricsService.calculate(mutatedIssues, clock);
        ComplianceReport afterCompliance = complianceService.analyze(mutatedIssues, userId);
        WorldState afterState = WorldState.from(mutatedIssues, userId, afterChaos, afterCompliance, clock);
        int afterScore = afterState.calculateStressScore();

        // Record snapshot
        try {
            snapshotService.record(userId, repo, afterScore,
                    afterState.getStressLevel(), "reshape", Map.of());
        } catch (Exception e) {
            log.warn("Failed to persist reshape snapshot: {}", e.getMessage());
        }

        DayStructure afterPlan = buildDayPlan(mutatedIssues, userId);

        return ResponseEntity.ok(Map.of(
            "status", "reshaped",
            "beforeScore", beforeScore,
            "afterScore", afterScore,
            "afterLevel", afterState.getStressLevel().name(),
            "actionsApplied", mutationPlan.actions().size(),
            "explanation", supervisorResult.explanation(),
            "llmUsed", supervisorResult.llmUsed(),
            "dayPlan", Map.of(
                "deepWork", afterPlan.deepWork() != null ? afterPlan.deepWork().number() : 0,
                "quickWins", afterPlan.quickWins().stream().map(Issue::number).toList(),
                "maintenance", afterPlan.maintenance().stream().map(Issue::number).toList(),
                "deferred", afterPlan.deferred().stream().map(Issue::number).toList()
            )
        ));
    }

    public record ReshapeRequest(String repo, String userId) {}

    /**
     * Apply mutation plan (label adds/removes) to issues in memory.
     * Returns a new list of issues with updated labels.
     */
    private List<Issue> applyMutationsToIssues(List<Issue> issues, GitHubMutationPlan plan) {
        // Build a map of issue number -> mutable label list
        var labelMap = new ConcurrentHashMap<Integer, List<String>>();
        for (Issue issue : issues) {
            List<String> labels = new ArrayList<>();
            if (issue.labels() != null) {
                issue.labels().forEach(l -> labels.add(l.name()));
            }
            labelMap.put(issue.number(), labels);
        }

        // Apply each action
        for (GitHubAction action : plan.actions()) {
            List<String> labels = labelMap.get(action.issueNumber());
            if (labels == null) continue;

            if (action instanceof GitHubAction.AddLabels add) {
                for (String label : add.labels()) {
                    if (!labels.contains(label)) labels.add(label);
                }
            } else if (action instanceof GitHubAction.RemoveLabels remove) {
                labels.removeAll(remove.labels());
            }
            // Comments don't change issue data
        }

        // Rebuild issues with updated labels
        return issues.stream().map(issue -> {
            List<String> newLabels = labelMap.get(issue.number());
            return new Issue(
                issue.number(), issue.title(), issue.body(),
                newLabels.stream().map(Issue.Label::new).toList(),
                issue.assignees(), issue.createdAt(), issue.updatedAt(),
                issue.state(), issue.milestone()
            );
        }).toList();
    }

    // --- day plan logic (same as ReshapeController, read-only) ---

    private DayStructure buildDayPlan(List<Issue> issues, String userId) {
        Comparator<Issue> order = Comparator
            .comparing((Issue i) -> getPriorityWeight(i))
            .thenComparing(Issue::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Issue::number);

        Map<Classification, List<Issue>> buckets = issues.stream()
            .filter(i -> "open".equalsIgnoreCase(i.state()))
            .filter(i -> userId.isEmpty() || (i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equalsIgnoreCase(userId))))
            .collect(Collectors.groupingBy(classifier::classify));

        List<Issue> deepWork = buckets.getOrDefault(Classification.DEEP_WORK, List.of())
            .stream().sorted(order).toList();
        List<Issue> quickWins = buckets.getOrDefault(Classification.QUICK_WIN, List.of())
            .stream().sorted(order).toList();
        List<Issue> maintenance = buckets.getOrDefault(Classification.MAINTENANCE, List.of())
            .stream().sorted(order).toList();
        List<Issue> deferred = buckets.getOrDefault(Classification.DEFERRED, List.of())
            .stream().sorted(order).toList();

        return new DayStructure(
            deepWork.isEmpty() ? null : deepWork.get(0),
            quickWins.stream().limit(3).toList(),
            maintenance.stream().limit(3).toList(),
            Stream.concat(
                deepWork.stream().skip(1),
                Stream.concat(
                    quickWins.stream().skip(3),
                    Stream.concat(maintenance.stream().skip(3), deferred.stream())
                )
            ).toList()
        );
    }

    private int getPriorityWeight(Issue issue) {
        if (issue.labels() == null) return 2;
        for (Issue.Label l : issue.labels()) {
            if (l.name().equalsIgnoreCase("priority:critical")) return 0;
            if (l.name().equalsIgnoreCase("priority:high")) return 1;
            if (l.name().equalsIgnoreCase("urgent")) return 1;
        }
        return 2;
    }

    private int calculateFridayScore(ChaosMetrics chaos, ComplianceReport compliance, WorldState state) {
        int score = 100;
        if (chaos.score() > 5) score -= 20;
        if (chaos.score() > 8) score -= 20;
        if (!compliance.isCompliant()) score -= 15;
        if (state.urgentUnassigned() > 0) score -= 15;
        if (chaos.afterHoursSignal()) score -= 10;
        if (state.mysteryMeatCount() > 3) score -= 10;
        return Math.max(0, score);
    }

    /**
     * Low-friction study check-in. Syncs a public repo's issues, calculates stress,
     * records a snapshot, and returns the result. No auth required.
     */
    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> checkin(@RequestBody CheckinRequest req) {
        String repo = req.repo();
        String userId = req.userId();

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        if (repo == null || !repo.matches("^[\\w.-]+/[\\w.-]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid repo format. Use owner/repo"));
        }

        // Use user-provided timezone if valid, otherwise fall back to server default
        Clock effectiveClock = clock;
        String effectiveTimezone = clock.getZone().getId();
        if (req.tz() != null && !req.tz().isBlank()) {
            try {
                ZoneId userZone = ZoneId.of(req.tz());
                effectiveClock = Clock.system(userZone);
                effectiveTimezone = userZone.getId();
            } catch (Exception e) {
                log.warn("Invalid timezone '{}', falling back to server default", req.tz());
            }
        }

        // Sync issues from GitHub (reuses cache + rate limiting)
        if (!issueCache.hasRepo(repo)) {
            Instant lastSync = lastSyncTimes.get(repo);
            if (lastSync != null) {
                Duration elapsed = Duration.between(lastSync, Instant.now());
                if (elapsed.compareTo(SYNC_COOLDOWN) < 0) {
                    long remaining = SYNC_COOLDOWN.minus(elapsed).toSeconds();
                    return ResponseEntity.status(429).body(Map.of(
                        "error", "rate_limited",
                        "message", "Try again in " + remaining + "s"
                    ));
                }
            }
            try {
                List<Issue> issues = fetchGitHubIssues(repo);
                issueCache.put(repo, issues, Instant.now());
                lastSyncTimes.put(repo, Instant.now());
            } catch (ResponseStatusException e) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", e.getReason() != null ? e.getReason() : "Unknown error"
                ));
            } catch (Exception e) {
                log.error("Check-in sync failed for {}", repo, e);
                return ResponseEntity.status(502).body(Map.of(
                    "error", "Failed to fetch issues from GitHub: " + e.getMessage()
                ));
            }
        }

        // Calculate stress
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, effectiveClock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, effectiveClock);
        int stressScore = state.calculateStressScore();
        StressLevel stressLevel = state.getStressLevel();

        // Build breakdown
        Map<String, Integer> breakdown = Map.of(
            "workload", calculateWorkloadStress(state),
            "chaos", state.chaosBucket().ordinalValue * 10,
            "contextSwitching", Math.min(15, Math.max(0, state.issuesTouchedToday() - 5) * 3),
            "clarity", Math.min(10, state.mysteryMeatCount() * 2),
            "sustained", Math.min(15, state.consecutiveHighChaosDays() * 5),
            "afterHours", Math.min(10, state.issuesUpdatedAfterHours() * 5)
        );

        // Build human-readable hints explaining each score
        Map<String, String> breakdownHints = Map.of(
            "workload", buildWorkloadHint(state),
            "chaos", buildChaosHint(state, chaos),
            "contextSwitching", state.issuesTouchedToday() + " issues updated in the last 8 hours"
                + (state.issuesTouchedToday() <= 5 ? " (threshold is 6+)" : " — that's a lot of context switching"),
            "clarity", state.mysteryMeatCount() + " issues missing a description or assignee"
                + (state.mysteryMeatCount() == 0 ? " — nice and clear" : " — unclear scope increases cognitive load"),
            "sustained", state.consecutiveHighChaosDays() == 0
                ? "No consecutive high-chaos days detected"
                : state.consecutiveHighChaosDays() + " consecutive days of high chaos — take a break",
            "afterHours", state.issuesUpdatedAfterHours() + " of your assigned issues updated outside working hours (before 9 AM or after 6 PM)"
                + (state.issuesUpdatedAfterHours() == 0 ? " — healthy boundaries" : " — signals overwork")
        );

        // Self-reported fields (optional)
        Integer selfScore = req.selfScore();
        String note = req.note();
        if (note != null && note.length() > 500) {
            note = note.substring(0, 500);
        }

        // Record snapshot
        try {
            snapshotService.record(userId, repo, stressScore, stressLevel, "checkin", breakdown,
                    selfScore, note);
        } catch (Exception e) {
            log.warn("Failed to persist check-in snapshot: {}", e.getMessage());
        }

        // Build issue attribution per metric (lightweight: number + title only)
        Map<String, List<Map<String, Object>>> breakdownIssues = new LinkedHashMap<>();
        breakdownIssues.put("workload", toIssueSummaries(findWorkloadIssues(issues, userId)));
        breakdownIssues.put("chaos", toIssueSummaries(findChaosIssues(issues)));
        breakdownIssues.put("contextSwitching", toIssueSummaries(findContextSwitchingIssues(issues, userId, effectiveClock)));
        breakdownIssues.put("clarity", toIssueSummaries(findClarityIssues(issues)));
        breakdownIssues.put("sustained", List.of()); // historical pattern, not per-issue
        breakdownIssues.put("afterHours", toIssueSummaries(findAfterHoursIssues(issues, userId, effectiveClock)));

        Map<String, Object> response = new HashMap<>();
        response.put("stressScore", stressScore);
        response.put("stressLevel", stressLevel.name());
        response.put("breakdown", breakdown);
        response.put("breakdownHints", breakdownHints);
        response.put("breakdownIssues", breakdownIssues);
        response.put("totalIssues", issues.size());
        response.put("is333Compliant", state.is333Compliant());
        response.put("repo", repo);
        response.put("timezone", effectiveTimezone);

        return ResponseEntity.ok(response);
    }

    private int calculateWorkloadStress(WorldState state) {
        int stress = 0;
        if (state.totalAssigned() > 7) stress += Math.min(20, (state.totalAssigned() - 7) * 4);
        if (state.deepWorkCount() > 1) stress += (state.deepWorkCount() - 1) * 10;
        if (state.deepWorkCount() == 0 && state.totalAssigned() > 0) stress += 5;
        return Math.min(40, stress);
    }

    private String buildWorkloadHint(WorldState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.totalAssigned()).append(" issues assigned");
        if (state.deepWorkCount() > 0) {
            sb.append(", ").append(state.deepWorkCount()).append(" deep work");
        }
        if (state.quickWinCount() > 0) {
            sb.append(", ").append(state.quickWinCount()).append(" quick wins");
        }
        if (state.maintenanceCount() > 0) {
            sb.append(", ").append(state.maintenanceCount()).append(" maintenance");
        }
        if (state.totalAssigned() > 7) {
            sb.append(" — more than 7 is overloaded");
        }
        if (state.deepWorkCount() > 1) {
            sb.append(". Multiple deep work items increase cognitive load");
        }
        return sb.toString();
    }

    private String buildChaosHint(WorldState state, ChaosMetrics chaos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chaos level: ").append(state.chaosBucket().name());
        if (state.urgentUnassigned() > 0) {
            sb.append(". ").append(state.urgentUnassigned()).append(" urgent issues have no assignee");
        }
        if (chaos.afterHoursSignal()) {
            sb.append(". After-hours activity detected in repo (any contributor)");
        }
        if (state.mysteryMeatCount() >= 3) {
            sb.append(". ").append(state.mysteryMeatCount()).append("+ issues lack descriptions");
        }
        return sb.toString();
    }

    // --- Issue-finding helpers (mirror WorldState counting logic, return issues instead of counts) ---

    private List<Issue> findWorkloadIssues(List<Issue> issues, String userId) {
        return issues.stream()
            .filter(i -> isAssignedToUser(i, userId))
            .toList();
    }

    private List<Issue> findChaosIssues(List<Issue> issues) {
        // Return all issues that trigger any chaos signal:
        // - urgent + unassigned
        // - missing description or assignee (mystery meat)
        // - after-hours activity
        // - urgent and older than 24h
        Instant now = clock.instant();
        return issues.stream()
            .filter(i -> {
                boolean urgentUnassigned = LabelUtils.hasAnyLabel(i, List.of("urgent", "priority:critical"))
                    && (i.assignees() == null || i.assignees().isEmpty());
                boolean mysteryMeat = (i.body() == null || i.body().isBlank())
                    || (i.assignees() == null || i.assignees().isEmpty());
                boolean afterHours = DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS)
                    || (!DemoLabels.hasDemoLabel(i) && isAfterHoursTime(i.updatedAt(), clock));
                boolean urgentStale = LabelUtils.hasLabel(i, "urgent")
                    && (DemoLabels.hasLabel(i, DemoLabels.STALE_14D)
                        || (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null
                            && i.createdAt().isBefore(now.minusSeconds(86400))));
                return urgentUnassigned || mysteryMeat || afterHours || urgentStale;
            })
            .toList();
    }

    private List<Issue> findContextSwitchingIssues(List<Issue> issues, String userId, Clock clock) {
        Instant cutoff = clock.instant().minus(Duration.ofHours(8));
        return issues.stream()
            .filter(i -> isAssignedToUser(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ||
                         (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null &&
                          i.updatedAt().isAfter(cutoff)))
            .toList();
    }

    private List<Issue> findClarityIssues(List<Issue> issues) {
        return issues.stream()
            .filter(i -> i.body() == null || i.body().isBlank())
            .toList();
    }

    private List<Issue> findAfterHoursIssues(List<Issue> issues, String userId, Clock clock) {
        return issues.stream()
            .filter(i -> isAssignedToUser(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS) ||
                         (!DemoLabels.hasDemoLabel(i) && isAfterHoursTime(i.updatedAt(), clock)))
            .toList();
    }

    private static boolean isAssignedToUser(Issue issue, String userId) {
        if (userId == null || userId.isEmpty()) return true;
        return issue.assignees() != null &&
               issue.assignees().stream().anyMatch(a -> a.login().equalsIgnoreCase(userId));
    }

    private static boolean isAfterHoursTime(Instant timestamp, Clock clock) {
        if (timestamp == null) return false;
        java.time.ZonedDateTime zdt = timestamp.atZone(clock.getZone());
        int hour = zdt.getHour();
        java.time.DayOfWeek dow = zdt.getDayOfWeek();
        return hour < 9 || hour >= 18 || dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
    }

    private List<Map<String, Object>> toIssueSummaries(List<Issue> issues) {
        return issues.stream()
            .map(i -> Map.<String, Object>of("number", i.number(), "title", i.title()))
            .toList();
    }

    public record CheckinRequest(String userId, String repo, Integer selfScore, String note, String tz) {}

    public record FlamegraphResponse(
        String status,
        String repo,
        DayStructure dayPlan,
        int stressScore,
        StressLevel stressLevel,
        int fridayScore,
        boolean compliant,
        int totalIssues
    ) {
        public static FlamegraphResponse notSynced(String repo) {
            return new FlamegraphResponse(
                "not_synced", repo, null, -1, StressLevel.LOW, -1, false, 0
            );
        }
    }
}
