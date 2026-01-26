package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.WorldState;

import java.util.List;

public record SlowIntake() implements Action {
    @Override 
    public String name() { 
        return "Slow intake (hide new issues)"; 
    }
    
    @Override 
    public String id() { 
        return "SlowIntake"; 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return 10; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return s.calculateStressScore() >= 70;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        WorldState.ChaosBucket reduced = switch (s.chaosBucket()) {
            case CRITICAL -> WorldState.ChaosBucket.HIGH;
            case HIGH -> WorldState.ChaosBucket.MEDIUM;
            case MEDIUM -> WorldState.ChaosBucket.LOW;
            case LOW -> WorldState.ChaosBucket.LOW;
        };
        return s.withChaosBucket(reduced);
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of();
    }
}
