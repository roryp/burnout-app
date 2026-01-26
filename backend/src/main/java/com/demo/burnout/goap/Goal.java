package com.demo.burnout.goap;

import com.demo.burnout.model.WorldState;

/**
 * GOAP Goal interface - defines desired end states.
 */
public interface Goal {
    int priority();
    boolean isSatisfied(WorldState state);
    int insistence(WorldState state);
}
