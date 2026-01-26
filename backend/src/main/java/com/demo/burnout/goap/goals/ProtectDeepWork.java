package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

public record ProtectDeepWork() implements Goal {
    @Override 
    public int priority() { 
        return 85; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.deepWorkCount() == 1 && s.issuesTouchedToday() <= 3; 
    }
    
    @Override 
    public int insistence(WorldState s) {
        if (s.deepWorkCount() == 0) return 30;
        if (s.deepWorkCount() > 1) return 50;
        return s.issuesTouchedToday() > 3 ? 40 : 0;
    }
}
