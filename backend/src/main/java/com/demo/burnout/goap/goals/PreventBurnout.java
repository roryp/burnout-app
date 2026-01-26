package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

public record PreventBurnout() implements Goal {
    @Override 
    public int priority() { 
        return 100; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.calculateStressScore() < 50 && s.consecutiveHighChaosDays() < 3; 
    }
    
    @Override 
    public int insistence(WorldState s) {
        int stress = s.calculateStressScore();
        if (stress >= 70) return 100;
        if (s.consecutiveHighChaosDays() >= 3) return 90;
        return stress;
    }
}
