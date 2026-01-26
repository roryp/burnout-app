package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.WorldState;

import java.util.List;

public record SuggestBreak() implements Action {
    @Override 
    public String name() { 
        return "Suggest break"; 
    }
    
    @Override 
    public String id() { 
        return "SuggestBreak"; 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return 5; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return s.consecutiveHighChaosDays() >= 2 || s.issuesUpdatedAfterHours() > 0;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withConsecutiveHighChaosDays(Math.max(0, s.consecutiveHighChaosDays() - 1));
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of();
    }
}
