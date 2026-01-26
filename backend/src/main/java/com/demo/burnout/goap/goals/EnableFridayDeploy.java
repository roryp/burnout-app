package com.demo.burnout.goap.goals;

import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record EnableFridayDeploy(Clock clock, boolean demoFridayEnabled) implements Goal {
    @Override 
    public int priority() { 
        return 70; 
    }
    
    @Override 
    public boolean isSatisfied(WorldState s) { 
        return s.chaosBucket().ordinalValue <= 1 && 
               s.is333Compliant() && 
               s.urgentUnassigned() == 0;
    }
    
    @Override 
    public int insistence(WorldState s) {
        boolean isFridayish = demoFridayEnabled || isThuOrFri(clock);
        if (!isFridayish) return 0;
        return isSatisfied(s) ? 0 : 80;
    }
    
    private boolean isThuOrFri(Clock clk) {
        DayOfWeek day = LocalDate.now(clk).getDayOfWeek();
        return day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY;
    }
}
