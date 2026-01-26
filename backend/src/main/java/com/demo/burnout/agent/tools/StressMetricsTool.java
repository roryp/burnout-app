package com.demo.burnout.agent.tools;

import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.ComplianceReport;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.ComplianceService;
import com.demo.burnout.service.IssueCache;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * LangChain4j Tool for reading stress and workload metrics.
 * 
 * Provides agents with access to deterministic metrics calculated by services.
 */
@Component
public class StressMetricsTool {

    private final IssueCache issueCache;
    private final ChaosMetricsService chaosMetricsService;
    private final ComplianceService complianceService;
    private final Clock clock;

    public StressMetricsTool(IssueCache issueCache, 
                             ChaosMetricsService chaosMetricsService,
                             ComplianceService complianceService,
                             Clock clock) {
        this.issueCache = issueCache;
        this.chaosMetricsService = chaosMetricsService;
        this.complianceService = complianceService;
        this.clock = clock;
    }

    @Tool("Get the current chaos score and breakdown for a repository")
    public String getChaosMetrics(@P("The repository in format owner/repo") String repo) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics metrics = chaosMetricsService.calculate(issues, clock);
        
        return String.format("""
            Chaos Metrics:
            - Overall Score: %.1f/10 (%s)
            - Issues Touched Recently: %d (context switching signal)
            - Stale Issues (14d+): %d
            - Mystery Meat Items: %d
            - Contradictory Labels: %d
            - After-Hours Activity: %s
            """,
            metrics.score(),
            metrics.score() > 7 ? "CRITICAL" : metrics.score() > 4 ? "ELEVATED" : "NORMAL",
            metrics.issuesTouchedRecently(),
            metrics.staleIssues(),
            metrics.mysteryMeatItems(),
            metrics.contradictoryLabels(),
            metrics.afterHoursActivity() ? "YES ⚠️" : "No"
        );
    }

    @Tool("Get 3-3-3 compliance analysis for a user's workload")
    public String getComplianceReport(
            @P("The repository in format owner/repo") String repo,
            @P("The GitHub username") String userId) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        ComplianceReport report = complianceService.analyze(issues, userId);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            3-3-3 Compliance Report for %s:
            - Compliance Score: %d/100 (%s)
            - Violations: %d
            
            """, 
            report.userId(),
            report.complianceScore(),
            report.complianceScore() >= 80 ? "COMPLIANT" : report.complianceScore() >= 50 ? "PARTIAL" : "NON-COMPLIANT",
            report.violations().size()
        ));
        
        if (!report.violations().isEmpty()) {
            sb.append("Violations:\n");
            for (var v : report.violations()) {
                sb.append(String.format("  - [%s] %s: %s\n", 
                    v.severity(), v.type(), v.message()));
            }
        }
        
        return sb.toString();
    }

    @Tool("Get overall stress score and burnout risk assessment")
    public String getStressAssessment(
            @P("The repository in format owner/repo") String repo,
            @P("The GitHub username") String userId) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        ChaosMetrics chaos = chaosMetricsService.calculate(issues, clock);
        ComplianceReport compliance = complianceService.analyze(issues, userId);
        WorldState state = WorldState.from(issues, userId, chaos, compliance, clock);
        
        int stress = state.calculateStressScore();
        String level = state.getStressLevel().name();
        
        return String.format("""
            Stress Assessment for %s:
            - Stress Score: %d/100
            - Stress Level: %s
            - Burnout Risk: %s
            
            Contributing Factors:
            - Deep Work Items: %d (max recommended: 1)
            - Quick Wins: %d (max recommended: 3)
            - Maintenance: %d (max recommended: 3)
            - Total Assigned: %d issues
            - Is 3-3-3 Compliant: %s
            - After-Hours Signals: %s
            """,
            userId,
            stress,
            level,
            stress >= 70 ? "HIGH ⚠️" : stress >= 50 ? "MODERATE" : "LOW",
            state.deepWorkCount(),
            state.quickWinCount(),
            state.maintenanceCount(),
            state.totalAssigned(),
            state.is333Compliant() ? "Yes ✓" : "No ✗",
            state.hasAfterHoursActivity() ? "Yes ⚠️" : "No"
        );
    }
}
