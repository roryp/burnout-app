package com.demo.burnout.model;

import com.demo.burnout.util.DemoLabels;
import com.demo.burnout.util.LabelUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GOAP World State - all measurable burnout indicators from GitHub issue fields.
 * Keep state DISCRETE (ints, booleans, coarse buckets) for greedy planner.
 */
public record WorldState(
    int deepWorkCount,
    int quickWinCount,
    int maintenanceCount,
    int deferredCount,
    int delegatedCount,
    int urgentUnassigned,
    int contradictoryLabels,
    int issuesTouchedToday,
    int issuesUpdatedAfterHours,
    int staleIssueCount,
    int mysteryMeatCount,
    int unclearQuickWins,
    int totalAssigned,
    ChaosBucket chaosBucket,
    int complianceScore,
    boolean is333Compliant,
    boolean calendarBlocked,
    int consecutiveHighChaosDays
) {
    public enum ChaosBucket { 
        LOW(0), MEDIUM(1), HIGH(2), CRITICAL(3);
        public final int ordinalValue;
        ChaosBucket(int v) { this.ordinalValue = v; }
        
        public static ChaosBucket from(double score) {
            if (score <= 2) return LOW;
            if (score <= 5) return MEDIUM;
            if (score <= 8) return HIGH;
            return CRITICAL;
        }
    }

    public static WorldState from(List<Issue> issues, String userId, 
                                   ChaosMetrics chaos, ComplianceReport compliance, Clock clock) {
        return new WorldState(
            Math.min(5, countDeepWork(issues, userId)),
            Math.min(5, countQuickWins(issues, userId)),
            Math.min(5, countMaintenance(issues, userId)),
            Math.min(10, countDeferred(issues, userId)),
            0,
            Math.min(10, countUrgentUnassigned(issues)),
            Math.min(5, countContradictory(issues)),
            Math.min(10, countTouchedToday(issues, userId, clock)),
            Math.min(5, countAfterHours(issues, userId, clock)),
            Math.min(10, countStale(issues, clock)),
            Math.min(10, countMysteryMeat(issues)),
            Math.min(5, countUnclearQuickWins(issues, userId)),
            Math.min(15, countAssigned(issues, userId)),
            ChaosBucket.from(chaos.score()),
            roundToFive(compliance.complianceScore()),
            compliance.isCompliant(),
            false,
            0
        );
    }

    public WorldState withDeepWorkCount(int v) {
        return new WorldState(v, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday, 
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }
    
    public WorldState withTotalAssigned(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, v, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }
    
    public WorldState withDeferredCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            v, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withDelegatedCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, v, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withQuickWinCount(int v) {
        return new WorldState(deepWorkCount, v, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withMaintenanceCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, v, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withMysteryMeatCount(int v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, v,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withCalendarBlocked(boolean v) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, deferredCount,
            delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, v, consecutiveHighChaosDays);
    }

    public WorldState recalculateCompliance() {
        boolean compliant = deepWorkCount <= 1 && quickWinCount <= 3 && maintenanceCount <= 3;
        int score = 100 - (deepWorkCount > 1 ? 25 : 0) 
                        - (quickWinCount > 3 ? 10 : 0) 
                        - (maintenanceCount > 3 ? 10 : 0);
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, Math.max(0, score),
            compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withChaosBucket(ChaosBucket bucket) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, bucket, complianceScore,
            is333Compliant, calendarBlocked, consecutiveHighChaosDays);
    }

    public WorldState withConsecutiveHighChaosDays(int days) {
        return new WorldState(deepWorkCount, quickWinCount, maintenanceCount, 
            deferredCount, delegatedCount, urgentUnassigned, contradictoryLabels, issuesTouchedToday,
            issuesUpdatedAfterHours, staleIssueCount, mysteryMeatCount,
            unclearQuickWins, totalAssigned, chaosBucket, complianceScore,
            is333Compliant, calendarBlocked, days);
    }

    public int chaosScoreDiscrete() {
        return chaosBucket.ordinalValue;
    }
    
    public double chaosScoreApprox() {
        return switch (chaosBucket) {
            case LOW -> 1.5;
            case MEDIUM -> 4.0;
            case HIGH -> 7.0;
            case CRITICAL -> 9.5;
        };
    }

    public int calculateStressScore() {
        int stress = 0;
        if (totalAssigned > 7) stress += Math.min(20, (totalAssigned - 7) * 4);
        if (deepWorkCount > 1) stress += (deepWorkCount - 1) * 10;
        if (deepWorkCount == 0 && totalAssigned > 0) stress += 5;
        stress += chaosBucket.ordinalValue * 10;
        if (issuesTouchedToday > 5) stress += Math.min(15, (issuesTouchedToday - 5) * 3);
        stress += Math.min(10, mysteryMeatCount * 2);
        stress += Math.min(5, unclearQuickWins);
        stress += Math.min(15, consecutiveHighChaosDays * 5);
        stress += Math.min(10, issuesUpdatedAfterHours * 5);
        return Math.min(100, stress);
    }

    public StressLevel getStressLevel() {
        int score = calculateStressScore();
        if (score >= 70) return StressLevel.CRITICAL;
        if (score >= 50) return StressLevel.HIGH;
        if (score >= 30) return StressLevel.MODERATE;
        return StressLevel.LOW;
    }

    private static int countDeepWork(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("priority:critical", "architecture", "deep-work")))
            .count();
    }

    private static int countQuickWins(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("good-first-issue", "quick-win", "size:S")))
            .count();
    }

    private static int countMaintenance(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("dependencies", "documentation", "maintenance", "tech-debt")))
            .count();
    }

    private static int countDeferred(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("deferred", "next-sprint", "backlog")))
            .count();
    }

    private static int countUrgentUnassigned(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("urgent", "priority:critical")))
            .filter(i -> i.assignees() == null || i.assignees().isEmpty())
            .count();
    }

    private static int countContradictory(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("bug")) && 
                         LabelUtils.hasAnyLabel(i, List.of("enhancement")))
            .count();
    }

    private static int countTouchedToday(List<Issue> issues, String userId, Clock clock) {
        Instant todayCutoff = clock.instant().minus(Duration.ofHours(8));
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) || 
                         (!DemoLabels.hasDemoLabel(i) && i.updatedAt() != null && 
                          i.updatedAt().isAfter(todayCutoff)))
            .count();
    }

    private static int countAfterHours(List<Issue> issues, String userId, Clock clock) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.AFTER_HOURS) || 
                         (!DemoLabels.hasDemoLabel(i) && isAfterHours(i.updatedAt(), clock)))
            .count();
    }

    private static int countStale(List<Issue> issues, Clock clock) {
        return (int) issues.stream()
            .filter(i -> DemoLabels.hasLabel(i, DemoLabels.STALE_14D) || 
                         (!DemoLabels.hasDemoLabel(i) && isStale(i.updatedAt(), 14, clock)))
            .count();
    }

    private static int countMysteryMeat(List<Issue> issues) {
        return (int) issues.stream()
            .filter(i -> i.body() == null || i.body().isBlank())
            .count();
    }

    private static int countUnclearQuickWins(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .filter(i -> LabelUtils.hasAnyLabel(i, List.of("quick-win", "good-first-issue")))
            .filter(i -> i.body() == null || i.body().isBlank())
            .count();
    }

    private static int countAssigned(List<Issue> issues, String userId) {
        return (int) issues.stream()
            .filter(i -> isAssignedTo(i, userId))
            .count();
    }

    private static boolean isAssignedTo(Issue issue, String userId) {
        return issue.assignees() != null && 
               issue.assignees().stream().anyMatch(a -> a.login().equalsIgnoreCase(userId));
    }

    private static int roundToFive(int value) {
        return Math.round(value / 5.0f) * 5;
    }

    private static boolean isAfterHours(Instant timestamp, Clock clock) {
        if (timestamp == null) return false;
        int hour = timestamp.atZone(clock.getZone()).getHour();
        return hour < 9 || hour >= 18;
    }

    private static boolean isStale(Instant timestamp, int days, Clock clock) {
        if (timestamp == null) return true;
        Instant cutoff = clock.instant().minus(Duration.ofDays(days));
        return timestamp.isBefore(cutoff);
    }

    // Convenience methods for compatibility
    public boolean hasAfterHoursActivity() {
        return issuesUpdatedAfterHours > 0;
    }

    public int contextSwitchCount() {
        return issuesTouchedToday;
    }

    public int blockedCount() {
        return urgentUnassigned;
    }
}
