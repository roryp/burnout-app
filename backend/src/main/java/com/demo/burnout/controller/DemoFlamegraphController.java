package com.demo.burnout.controller;

import com.demo.burnout.model.*;
import com.demo.burnout.service.*;
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
import java.util.Comparator;
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
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, Instant> lastSyncTimes = new ConcurrentHashMap<>();

    public DemoFlamegraphController(IssueCache issueCache,
                                    ChaosMetricsService chaosMetricsService,
                                    ComplianceService complianceService,
                                    IssueClassifierService classifier,
                                    StressSnapshotService snapshotService,
                                    Clock clock,
                                    ObjectMapper objectMapper) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.classifier = classifier;
        this.snapshotService = snapshotService;
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
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
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

        // Record snapshot
        try {
            snapshotService.record(userId, repo, stressScore, stressLevel, "checkin", breakdown);
        } catch (Exception e) {
            log.warn("Failed to persist check-in snapshot: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "stressScore", stressScore,
            "stressLevel", stressLevel.name(),
            "breakdown", breakdown,
            "totalIssues", issues.size(),
            "is333Compliant", state.is333Compliant()
        ));
    }

    private int calculateWorkloadStress(WorldState state) {
        int stress = 0;
        if (state.totalAssigned() > 7) stress += Math.min(20, (state.totalAssigned() - 7) * 4);
        if (state.deepWorkCount() > 1) stress += (state.deepWorkCount() - 1) * 10;
        if (state.deepWorkCount() == 0 && state.totalAssigned() > 0) stress += 5;
        return Math.min(40, stress);
    }

    public record CheckinRequest(String userId, String repo) {}

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
