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
}
