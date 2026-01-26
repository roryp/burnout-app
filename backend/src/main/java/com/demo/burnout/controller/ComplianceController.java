package com.demo.burnout.controller;

import com.demo.burnout.model.ComplianceReport;
import com.demo.burnout.service.ComplianceService;
import com.demo.burnout.service.IssueCache;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ComplianceController {
    
    private final IssueCache issueCache;
    private final ComplianceService complianceService;

    public ComplianceController(IssueCache issueCache, ComplianceService complianceService) {
        this.issueCache = issueCache;
        this.complianceService = complianceService;
    }

    @GetMapping("/compliance")
    public ComplianceReport compliance(@RequestParam String repo, @RequestParam String userId) {
        if (!issueCache.hasRepo(repo)) {
            return ComplianceReport.notSynced();
        }
        return complianceService.analyze(issueCache.get(repo), userId);
    }
}
