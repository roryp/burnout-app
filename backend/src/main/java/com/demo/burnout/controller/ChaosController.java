package com.demo.burnout.controller;

import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.IssueCache;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChaosController {
    
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;

    public ChaosController(IssueCache issueCache, ChaosMetricsService chaosMetricsService) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
    }

    @GetMapping("/chaos")
    public ChaosResponse chaos(@RequestParam String repo) {
        if (!issueCache.hasRepo(repo)) {
            return ChaosResponse.notSynced();
        }
        ChaosMetrics metrics = chaosMetricsService.calculate(issueCache.get(repo));
        return ChaosResponse.from(metrics);
    }

    public record ChaosResponse(
        String status,
        double score,
        long issuesTouchedRecently,
        long unresolvedUrgent,
        int distinctLabelCount,
        boolean afterHoursSignal,
        int mysteryMeatCount,
        int schemaVersion
    ) {
        public static ChaosResponse notSynced() {
            return new ChaosResponse("not_synced", -1, 0, 0, 0, false, 0, ChaosMetrics.SCHEMA_VERSION);
        }
        
        public static ChaosResponse from(ChaosMetrics m) {
            return new ChaosResponse(
                "ok",
                m.score(),
                m.issuesTouchedRecently(),
                m.unresolvedUrgent(),
                m.distinctLabelCount(),
                m.afterHoursSignal(),
                m.mysteryMeatCount(),
                m.schemaVersion()
            );
        }
    }
}
