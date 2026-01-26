package com.demo.burnout.goap;

import java.util.List;

public record GitHubMutationPlan(String repo, List<GitHubAction> actions) {
    public static final int SCHEMA_VERSION = 1;
    
    public static GitHubMutationPlan empty() { 
        return new GitHubMutationPlan("", List.of()); 
    }
    
    public boolean isEmpty() { 
        return actions.isEmpty(); 
    }
    
    public int schemaVersion() { 
        return SCHEMA_VERSION; 
    }
}
