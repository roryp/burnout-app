package com.demo.burnout.model;

/**
 * Chaos metrics calculated from GitHub issues.
 */
public record ChaosMetrics(
    long issuesTouchedRecently,
    long unresolvedUrgent,
    int distinctLabelCount,
    boolean afterHoursSignal,
    int mysteryMeatCount,
    double score
) {
    public static final int SCHEMA_VERSION = 1;
    
    public static ChaosMetrics notSynced() {
        return new ChaosMetrics(0, 0, 0, false, 0, -1);
    }
    
    public boolean isSynced() { 
        return score >= 0; 
    }
    
    public int schemaVersion() { 
        return SCHEMA_VERSION; 
    }

    // Convenience methods for compatibility
    public long staleIssues() {
        return issuesTouchedRecently > 0 ? 0 : 1;
    }

    public int mysteryMeatItems() {
        return mysteryMeatCount;
    }

    public int contradictoryLabels() {
        return distinctLabelCount > 10 ? distinctLabelCount - 10 : 0;
    }

    public boolean afterHoursActivity() {
        return afterHoursSignal;
    }
}
