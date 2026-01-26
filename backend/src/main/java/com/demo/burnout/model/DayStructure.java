package com.demo.burnout.model;

import java.util.List;

/**
 * 3-3-3 Day Structure: 1 Deep Work + 3 Quick Wins + 3 Maintenance
 */
public record DayStructure(
    Issue deepWork,
    List<Issue> quickWins,
    List<Issue> maintenance,
    List<Issue> deferred
) {
    public static final int MAX_DEEP_WORK = 1;
    public static final int MAX_QUICK_WINS = 3;
    public static final int MAX_MAINTENANCE = 3;
    public static final int MAX_ACTIVE = MAX_DEEP_WORK + MAX_QUICK_WINS + MAX_MAINTENANCE;

    public boolean isCompliant() {
        return (deepWork == null ? 0 : 1) <= MAX_DEEP_WORK
            && quickWins.size() <= MAX_QUICK_WINS
            && maintenance.size() <= MAX_MAINTENANCE;
    }
}
