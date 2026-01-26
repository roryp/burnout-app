package com.demo.burnout.controller;

import com.demo.burnout.goap.*;
import com.demo.burnout.model.*;
import com.demo.burnout.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReshapeController {
    
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final IssueClassifierService classifier;
    private final GOAPPlanner goapPlanner;
    private final Clock clock;

    public ReshapeController(IssueCache issueCache, 
                            ChaosMetricsService chaosMetricsService,
                            ComplianceService complianceService,
                            IssueClassifierService classifier,
                            GOAPPlanner goapPlanner,
                            Clock clock) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.classifier = classifier;
        this.goapPlanner = goapPlanner;
        this.clock = clock;
    }

    @PostMapping("/reshape")
    public ReshapeResponse reshape(@RequestBody ReshapeRequest req) {
        if (!issueCache.hasRepo(req.repo())) {
            return ReshapeResponse.notSynced();
        }
        
        List<Issue> issues = issueCache.get(req.repo());
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, req.userId());
        WorldState state = WorldState.from(issues, req.userId(), chaos, compliance, clock);
        
        DayStructure dayPlan = buildDayPlan(issues, req.userId());
        GoapActionPlan actionPlan = goapPlanner.plan(state, issues, req.userId());
        GitHubMutationPlan mutationPlan = req.dryRun() 
            ? GitHubMutationPlan.empty() 
            : actionPlan.toMutationPlan(req.repo());
        
        int fridayScore = calculateFridayScore(chaos, compliance, state);
        
        return new ReshapeResponse(
            "ok",
            dayPlan,
            mutationPlan,
            actionPlan.toSummaries(state),
            chaos,
            compliance,
            state.calculateStressScore(),
            state.getStressLevel(),
            actionPlan.expectedStressScore(),
            fridayScore,
            ReshapeResponse.SCHEMA_VERSION
        );
    }

    private DayStructure buildDayPlan(List<Issue> issues, String userId) {
        Comparator<Issue> order = Comparator
            .comparing((Issue i) -> getPriorityWeight(i))
            .thenComparing(Issue::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Issue::number);

        Map<Classification, List<Issue>> buckets = issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equals(userId)))
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

    public record ReshapeRequest(String repo, String userId, boolean dryRun) {}

    public record ReshapeResponse(
        String status,
        DayStructure dayPlan,
        GitHubMutationPlan actionPlan,
        List<GoapActionSummary> goapActions,
        ChaosMetrics chaos,
        ComplianceReport compliance,
        int stressScore,
        StressLevel stressLevel,
        int expectedStressScore,
        int fridayScore,
        int schemaVersion
    ) {
        public static final int SCHEMA_VERSION = 1;
        
        public static ReshapeResponse notSynced() {
            return new ReshapeResponse(
                "not_synced", null, GitHubMutationPlan.empty(), List.of(),
                ChaosMetrics.notSynced(), ComplianceReport.notSynced(),
                -1, StressLevel.LOW, -1, -1, SCHEMA_VERSION
            );
        }
    }
}
