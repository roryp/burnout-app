package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

public record Achieve333Compliance() implements Goal {
    @Override 
    public int priority() { 
        return 90; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.is333Compliant(); 
    }
    
    @Override 
    public int insistence(WorldState s) {
        return s.is333Compliant() ? 0 : 100 - s.complianceScore();
    }
}
