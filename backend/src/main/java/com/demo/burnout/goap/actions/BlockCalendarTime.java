package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.WorldState;

import java.util.List;

public record BlockCalendarTime() implements Action {
    @Override 
    public String name() { 
        return "Block calendar for deep work"; 
    }
    
    @Override 
    public String id() { 
        return "BlockCalendarTime"; 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return 8; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return s.deepWorkCount() > 0 && !s.calendarBlocked();
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withCalendarBlocked(true);
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of();
    }
}
