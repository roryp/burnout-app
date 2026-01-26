package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

public record ClearMysteryMeat() implements Goal {
    @Override 
    public int priority() { 
        return 60; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.mysteryMeatCount() == 0; 
    }
    
    @Override 
    public int insistence(WorldState s) { 
        return s.mysteryMeatCount() * 15; 
    }
}
