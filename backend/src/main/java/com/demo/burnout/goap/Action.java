package com.demo.burnout.goap;

import com.demo.burnout.model.WorldState;

import java.util.List;

/**
 * GOAP Action interface - defines preconditions and effects.
 */
public interface Action {
    String name();
    String id();
    int cost(WorldState state);
    boolean preconditionsMet(WorldState state);
    WorldState apply(WorldState state);
    List<GitHubAction> toGitHubActions();
}
