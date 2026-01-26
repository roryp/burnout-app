package com.demo.burnout.util;

import com.demo.burnout.model.Issue;

import java.util.Set;

/**
 * Canonical demo label names. Use these constants everywhere to avoid case/spelling mismatches.
 */
public final class DemoLabels {
    private DemoLabels() {}

    // Synthetic time labels (override real timestamps)
    public static final String TOUCHED_TODAY = "demo:touched-today";
    public static final String AFTER_HOURS = "demo:after-hours";
    public static final String STALE_14D = "demo:stale-14d";
    public static final String FRIDAY = "demo:friday";

    public static final Set<String> ALL = Set.of(
        TOUCHED_TODAY, AFTER_HOURS, STALE_14D, FRIDAY
    );

    public static boolean hasDemoLabel(Issue issue) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().toLowerCase().startsWith("demo:"));
    }

    public static boolean hasLabel(Issue issue, String demoLabel) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(demoLabel));
    }
}
