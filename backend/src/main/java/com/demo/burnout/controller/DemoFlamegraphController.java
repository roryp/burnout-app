package com.demo.burnout.controller;

import com.demo.burnout.model.*;
import com.demo.burnout.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 */
@RestController
@RequestMapping("/demo/api")
@CrossOrigin(origins = "*")
public class DemoFlamegraphController {

    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final IssueClassifierService classifier;
    private final Clock clock;

    public DemoFlamegraphController(IssueCache issueCache,
                                    ChaosMetricsService chaosMetricsService,
                                    ComplianceService complianceService,
                                    IssueClassifierService classifier,
                                    Clock clock) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.classifier = classifier;
        this.clock = clock;
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
