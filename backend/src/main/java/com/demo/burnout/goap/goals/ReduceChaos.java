package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

public record ReduceChaos() implements Goal {
    @Override 
    public int priority() { 
        return 80; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.chaosBucket().ordinalValue <= 1; 
    }
    
    @Override 
    public int insistence(WorldState s) {
        return s.chaosBucket().ordinalValue * 25;
    }
}
