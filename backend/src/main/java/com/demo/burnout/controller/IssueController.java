package com.demo.burnout.controller;

import com.demo.burnout.dto.ChaosResponse;
import com.demo.burnout.dto.SyncRequest;
import com.demo.burnout.dto.SyncResponse;
import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.IssueCache;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class IssueController {
    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;

    public IssueController(IssueCache issueCache, ChaosMetricsService chaosMetricsService) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
    }

    @PostMapping("/issues/sync")
    public SyncResponse sync(@RequestBody SyncRequest request) {
        issueCache.put(request.repo(), request.issues(), Instant.now());
        return new SyncResponse("ok", request.issues().size());
    }

    @GetMapping("/chaos")
    public ChaosResponse chaos(@RequestParam String repo) {
        if (!issueCache.hasRepo(repo)) {
            return ChaosResponse.notSynced();
        }
        ChaosMetrics metrics = chaosMetricsService.calculate(issueCache.get(repo));
        return ChaosResponse.from(metrics);
    }
}