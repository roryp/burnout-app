package com.demo.burnout.service;

import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.Issue;
import com.demo.burnout.util.DemoLabels;
import com.demo.burnout.util.LabelUtils;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ChaosMetricsService {

    private final Clock clock;

    public ChaosMetricsService(Clock clock) {
        this.clock = clock;
    }

    public ChaosMetrics calculate(List<Issue> issues) {
        return calculate(issues, clock);
    }

    public ChaosMetrics calculate(List<Issue> issues, Clock clk) {
        Instant now = clk.instant();
        return new ChaosMetrics(
            countRecentUpdates(issues, 60, now),
            countUrgentOver24h(issues, now),
            countDistinctLabels(issues),
            hasAfterHoursActivity(issues, now, clk),
            countMissingDescriptionOrAssignee(issues),
            calculateOverallScore(issues, now, clk)
        );
    }

    private boolean hasAfterHoursActivity(List<Issue> issues, Instant now, Clock clk) {
        return issues.stream().anyMatch(i -> 
            DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS) ||
            (!DemoLabels.hasDemoLabel(i) && isAfterHours(i.updatedAt(), clk)));
    }

    private long countRecentUpdates(List<Issue> issues, int minutes, Instant now) {
        return issues.stream().filter(i -> 
            DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ||
            (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null && 
             i.updatedAt().isAfter(now.minus(Duration.ofMinutes(minutes))))
        ).count();
    }

    private long countUrgentOver24h(List<Issue> issues, Instant now) {
        return issues.stream().filter(i -> 
            LabelUtils.hasLabel(i, "urgent") &&
            (DemoLabels.hasLabel(i, DemoLabels.STALE_14D) ||
             (!DemoLabels.hasDemoLabel(i) && i.createdAt() != null && 
              i.createdAt().isBefore(now.minusSeconds(86400))))
        ).count();
    }

    private int countDistinctLabels(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> i.labels() != null)
            .flatMap(i -> i.labels().stream())
            .map(Issue.Label::name)
            .distinct()
            .count();
    }

    private int countMissingDescriptionOrAssignee(List<Issue> issues) {
        return (int) issues.stream().filter(i -> 
            (i.body() == null || i.body().isBlank()) ||
            (i.assignees() == null || i.assignees().isEmpty())
        ).count();
    }

    /**
     * EXPLICIT CHAOS SCORE FORMULA (0-10, deterministic):
     * +2 if mysteryMeatCount >= 3
     * +2 if unresolvedUrgent >= 3
     * +2 if issuesTouchedToday >= 6
     * +2 if afterHoursSignal == true
     * +2 if distinctLabelCount >= 12
     */
    private double calculateOverallScore(List<Issue> issues, Instant now, Clock clk) {
        int score = 0;
        
        int mysteryMeat = countMissingDescriptionOrAssignee(issues);
        if (mysteryMeat >= 3) score += 2;
        
        long urgent = countUrgentOver24h(issues, now);
        if (urgent >= 3) score += 2;
        
        long touched = countRecentUpdates(issues, 60, now);
        if (touched >= 6) score += 2;
        
        if (hasAfterHoursActivity(issues, now, clk)) score += 2;
        
        int labels = countDistinctLabels(issues);
        if (labels >= 12) score += 2;
        
        return Math.min(10, score);
    }

    private boolean isAfterHours(Instant timestamp, Clock clk) {
        if (timestamp == null) return false;
        ZonedDateTime zoned = timestamp.atZone(clk.getZone());
        int hour = zoned.getHour();
        DayOfWeek dow = zoned.getDayOfWeek();
        return hour < 8 || hour >= 18 || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
