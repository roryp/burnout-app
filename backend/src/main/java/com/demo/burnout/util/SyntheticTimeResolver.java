package com.demo.burnout.util;

import com.demo.burnout.config.DemoConfiguration;
import com.demo.burnout.model.Issue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.Clock;

/**
 * Centralized utility for resolving time-based checks with demo label precedence.
 * GOLDEN RULE: If ANY demo:* label exists on issue, never consult real timestamps.
 */
public final class SyntheticTimeResolver {
    
    private SyntheticTimeResolver() {}

    public static boolean isTouchedRecently(Issue issue, int minutesWindow, Instant now) {
        if (DemoLabels.hasLabel(issue, DemoLabels.TOUCHED_TODAY)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false;
        return issue.updatedAt() != null && 
               issue.updatedAt().isAfter(now.minus(Duration.ofMinutes(minutesWindow)));
    }

    public static boolean isAfterHours(Issue issue, Clock clk) {
        if (DemoLabels.hasLabel(issue, DemoLabels.AFTER_HOURS)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false;
        return isRealAfterHours(issue.updatedAt(), clk);
    }

    public static boolean isStale(Issue issue, int days, Instant now) {
        String staleLabel = "demo:stale-" + days + "d";
        if (DemoLabels.hasLabel(issue, staleLabel)) return true;
        if (DemoLabels.hasLabel(issue, DemoLabels.STALE_14D)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false;
        return issue.createdAt() != null && 
               issue.createdAt().isBefore(now.minus(Duration.ofDays(days)));
    }

    public static boolean isFriday(Issue issue, DemoConfiguration config, Clock clk) {
        if (DemoLabels.hasLabel(issue, DemoLabels.FRIDAY)) return true;
        if (DemoLabels.hasDemoLabel(issue)) return false;
        if (config != null && config.getFriday().isEnabled()) return true;
        return LocalDate.now(clk).getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    private static boolean isRealAfterHours(Instant updatedAt, Clock clk) {
        if (updatedAt == null) return false;
        ZonedDateTime zdt = updatedAt.atZone(clk.getZone());
        int hour = zdt.getHour();
        DayOfWeek dow = zdt.getDayOfWeek();
        return hour < 8 || hour >= 18 || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
