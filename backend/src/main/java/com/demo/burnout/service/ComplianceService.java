package com.demo.burnout.service;

import com.demo.burnout.model.*;
import com.demo.burnout.util.DemoLabels;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    private final IssueClassifierService classifier;
    private final Clock clock;

    public ComplianceService(IssueClassifierService classifier, Clock clock) {
        this.classifier = classifier;
        this.clock = clock;
    }

    public ComplianceReport analyze(List<Issue> issues, String userId) {
        List<Issue> userIssues = issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equals(userId)))
            .toList();

        Map<Classification, List<Issue>> buckets = userIssues.stream()
            .collect(Collectors.groupingBy(classifier::classify));

        List<Issue> deepWork = buckets.getOrDefault(Classification.DEEP_WORK, List.of());
        List<Issue> quickWins = buckets.getOrDefault(Classification.QUICK_WIN, List.of());
        List<Issue> maintenance = buckets.getOrDefault(Classification.MAINTENANCE, List.of());
        List<Issue> deferred = buckets.getOrDefault(Classification.DEFERRED, List.of());

        List<Violation> violations = new ArrayList<>();

        if (deepWork.size() > 1) {
            violations.add(new Violation(
                ViolationType.MULTIPLE_DEEP_WORK,
                Severity.CRITICAL,
                "You have " + deepWork.size() + " deep-work issues active. Max is 1.",
                deepWork,
                "Pick ONE critical issue. Move others to next sprint or delegate.",
                "labels"
            ));
        }

        if (quickWins.size() > 3) {
            violations.add(new Violation(
                ViolationType.QUICK_WIN_OVERLOAD,
                Severity.WARNING,
                "You have " + quickWins.size() + " quick wins. Max is 3 per day.",
                quickWins.subList(3, quickWins.size()),
                "Defer " + (quickWins.size() - 3) + " quick wins to tomorrow.",
                "labels"
            ));
        }

        if (maintenance.size() > 3) {
            violations.add(new Violation(
                ViolationType.MAINTENANCE_OVERLOAD,
                Severity.WARNING,
                "You have " + maintenance.size() + " maintenance tasks. Max is 3.",
                maintenance.subList(3, maintenance.size()),
                "Batch remaining maintenance for a dedicated maintenance day.",
                "labels"
            ));
        }

        if (deepWork.isEmpty() && !userIssues.isEmpty()) {
            violations.add(new Violation(
                ViolationType.NO_DEEP_WORK,
                Severity.INFO,
                "No deep-work issue assigned. You may be stuck in reactive mode.",
                List.of(),
                "Identify one priority:critical or architecture issue to focus on.",
                "labels"
            ));
        }

        Instant todayCutoff = clock.instant().minus(Duration.ofHours(8));
        long issuesTouchedToday = userIssues.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ||
                         (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null && 
                          i.updatedAt().isAfter(todayCutoff)))
            .count();
        if (issuesTouchedToday > 5) {
            violations.add(new Violation(
                ViolationType.EXCESSIVE_CONTEXT_SWITCHING,
                Severity.CRITICAL,
                "You've touched " + issuesTouchedToday + " issues today. High context-switch cost.",
                List.of(),
                "Focus on completing one issue before moving to the next.",
                "updatedAt"
            ));
        }

        List<Issue> mysteryQuickWins = quickWins.stream()
            .filter(i -> i.body() == null || i.body().isBlank())
            .toList();
        if (!mysteryQuickWins.isEmpty()) {
            violations.add(new Violation(
                ViolationType.UNCLEAR_QUICK_WINS,
                Severity.WARNING,
                mysteryQuickWins.size() + " quick wins have no description.",
                mysteryQuickWins,
                "Add scope/acceptance criteria or reclassify as deferred.",
                "body"
            ));
        }

        Instant staleCutoff = clock.instant().minus(Duration.ofDays(14));
        List<Issue> staleDeferred = deferred.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.STALE_14D) ||
                         (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null && 
                          i.createdAt().isBefore(staleCutoff)))
            .toList();
        if (staleDeferred.size() > 5) {
            violations.add(new Violation(
                ViolationType.DEFERRED_BACKLOG_GROWING,
                Severity.INFO,
                staleDeferred.size() + " deferred issues are >14 days old.",
                staleDeferred,
                "Schedule a backlog grooming session.",
                "createdAt"
            ));
        }

        int totalActive = deepWork.size() + quickWins.size() + maintenance.size();
        int maxAllowed = DayStructure.MAX_ACTIVE;
        if (totalActive > maxAllowed) {
            violations.add(new Violation(
                ViolationType.TOTAL_OVERLOAD,
                Severity.CRITICAL,
                "Total active issues: " + totalActive + ". Max for 3-3-3 is " + maxAllowed + ".",
                List.of(),
                "Defer " + (totalActive - maxAllowed) + " issues to protect your focus.",
                "assignees"
            ));
        }

        return new ComplianceReport(
            userId,
            violations.isEmpty(),
            violations,
            Map.of(
                "deepWork", deepWork.size(),
                "quickWins", quickWins.size(),
                "maintenance", maintenance.size(),
                "deferred", deferred.size()
            ),
            calculateComplianceScore(violations)
        );
    }

    private int calculateComplianceScore(List<Violation> violations) {
        int score = 100;
        for (Violation v : violations) {
            score -= switch (v.severity()) {
                case CRITICAL -> 25;
                case WARNING -> 10;
                case INFO -> 5;
            };
        }
        return Math.max(0, score);
    }
}
