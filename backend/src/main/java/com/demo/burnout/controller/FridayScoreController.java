package com.demo.burnout.controller;

import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.ComplianceReport;
import com.demo.burnout.model.Issue;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.ComplianceService;
import com.demo.burnout.service.IssueCache;
import com.demo.burnout.util.FridayScoreFormula;
import com.demo.burnout.util.LabelUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FridayScoreController {
    
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final Clock clock;

    public FridayScoreController(IssueCache issueCache, 
                                 ChaosMetricsService chaosMetricsService,
                                 ComplianceService complianceService,
                                 Clock clock) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.clock = clock;
    }

    @GetMapping("/friday-score")
    public FridayScoreResponse fridayScore(@RequestParam String repo, 
                                            @RequestParam(defaultValue = "") String userId) {
        if (!issueCache.hasRepo(repo)) {
            return FridayScoreResponse.notSynced();
        }
        
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);

        // Deploy-relevant signals computed directly from the issue list so we
        // don't depend on a full WorldState here.
        int mysteryMeatByEmptyBody = (int) issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.body() == null || i.body().isBlank())
            .count();
        int urgentUnassignedCount = (int) issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("urgent", "priority:critical")))
            .filter(i -> i.assignees() == null || i.assignees().isEmpty())
            .count();

        // If a userId is provided, gate on the structural 1-3-3-0 check.
        // Otherwise (no user context) treat the day plan as compliant -- we
        // can't know what the user's plan is from chaos signals alone.
        int score;
        if (userId != null && !userId.isEmpty()) {
            ComplianceReport compliance = complianceService.analyze(issues, userId);
            score = FridayScoreFormula.compute(
                compliance, mysteryMeatByEmptyBody, urgentUnassignedCount, chaos.afterHoursSignal());
        } else {
            score = FridayScoreFormula.compute(
                true, mysteryMeatByEmptyBody, urgentUnassignedCount, chaos.afterHoursSignal());
        }
        
        return new FridayScoreResponse(
            score,
            score >= 80 ? "READY" : score >= 50 ? "CAUTION" : "NOT_READY",
            chaos.score(),
            chaos.unresolvedUrgent(),
            chaos.afterHoursSignal(),
            FridayScoreResponse.SCHEMA_VERSION
        );
    }

    public record FridayScoreResponse(
        int score,
        String status,
        double chaosScore,
        long unresolvedUrgent,
        boolean afterHoursSignal,
        int schemaVersion
    ) {
        public static final int SCHEMA_VERSION = 1;
        
        public static FridayScoreResponse notSynced() {
            return new FridayScoreResponse(-1, "NOT_SYNCED", -1, 0, false, SCHEMA_VERSION);
        }
    }
}
