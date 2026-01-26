package com.demo.burnout.model;

import java.util.List;
import java.util.Map;

public record ComplianceReport(
    String userId,
    boolean isCompliant,
    List<Violation> violations,
    Map<String, Integer> bucketCounts,
    int complianceScore
) {
    public static final int SCHEMA_VERSION = 1;
    
    public static ComplianceReport notSynced() {
        return new ComplianceReport(null, false, List.of(), Map.of(), -1);
    }
    
    public boolean isSynced() { 
        return complianceScore >= 0; 
    }
    
    public int schemaVersion() { 
        return SCHEMA_VERSION; 
    }
}
