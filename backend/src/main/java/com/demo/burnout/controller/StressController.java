package com.demo.burnout.controller;

import com.demo.burnout.goap.GoapActionPlan;
import com.demo.burnout.goap.GoapActionSummary;
import com.demo.burnout.model.*;
import com.demo.burnout.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StressController {
    
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final GOAPPlanner goapPlanner;
    private final Clock clock;

    public StressController(IssueCache issueCache, 
                           ChaosMetricsService chaosMetricsService,
                           ComplianceService complianceService,
                           GOAPPlanner goapPlanner,
                           Clock clock) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.goapPlanner = goapPlanner;
        this.clock = clock;
    }

    @GetMapping("/stress")
    public StressResponse stress(@RequestParam String repo, @RequestParam String userId) {
        if (!issueCache.hasRepo(repo)) {
            return StressResponse.notSynced();
        }
        
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
        
        GoapActionPlan actionPlan = goapPlanner.plan(state, issues, userId);
        
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
            actionPlan.toSummaries(state),
            actionPlan.initialStressScore(),
            actionPlan.expectedStressScore(),
            StressResponse.SCHEMA_VERSION
        );
    }

    private int calculateWorkloadStress(WorldState state) {
        int stress = 0;
        if (state.totalAssigned() > 7) stress += Math.min(20, (state.totalAssigned() - 7) * 4);
        if (state.deepWorkCount() > 1) stress += (state.deepWorkCount() - 1) * 10;
        if (state.deepWorkCount() == 0 && state.totalAssigned() > 0) stress += 5;
        return Math.min(40, stress);
    }

    public record StressResponse(
        int stressScore,
        StressLevel stressLevel,
        Map<String, Integer> breakdown,
        boolean is333Compliant,
        List<GoapActionSummary> actionPlanSummary,
        int initialStressScore,
        int expectedStressScore,
        int schemaVersion
    ) {
        public static final int SCHEMA_VERSION = 1;
        
        public static StressResponse notSynced() {
            return new StressResponse(-1, StressLevel.LOW, Map.of(), false, List.of(), 0, 0, SCHEMA_VERSION);
        }
    }
}
