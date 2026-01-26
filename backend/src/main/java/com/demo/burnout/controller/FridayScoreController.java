package com.demo.burnout.controller;

import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.Issue;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.ComplianceService;
import com.demo.burnout.service.IssueCache;
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
        
        int score = 100;
        
        // -20 for chaos > 5
        if (chaos.score() > 5) score -= 20;
        
        // -20 more for chaos > 8
        if (chaos.score() > 8) score -= 20;
        
        // -15 for unresolved urgent > 24h
        if (chaos.unresolvedUrgent() > 0) score -= 15;
        
        // -10 for after hours signals
        if (chaos.afterHoursSignal()) score -= 10;
        
        // -10 for mystery meat
        if (chaos.mysteryMeatCount() > 3) score -= 10;
        
        // Compliance check if userId provided
        if (userId != null && !userId.isEmpty()) {
            var compliance = complianceService.analyze(issues, userId);
            if (!compliance.isCompliant()) score -= 15;
        }
        
        score = Math.max(0, score);
        
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
