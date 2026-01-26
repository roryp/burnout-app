package com.demo.burnout.goap;

import com.demo.burnout.model.WorldState;

import java.util.List;

public record GoapActionPlan(
    List<Action> actions,
    int initialStressScore,
    int expectedStressScore
) {
    public static GoapActionPlan empty() {
        return new GoapActionPlan(List.of(), 0, 0);
    }
    
    public boolean isEmpty() { 
        return actions.isEmpty(); 
    }
    
    public GitHubMutationPlan toMutationPlan(String repo) {
        List<GitHubAction> ghActions = actions.stream()
            .flatMap(a -> a.toGitHubActions().stream())
            .toList();
        return new GitHubMutationPlan(repo, ghActions);
    }
    
    public List<GoapActionSummary> toSummaries(WorldState stateAtSelection) {
        return actions.stream()
            .map(a -> new GoapActionSummary(
                a.id(),
                a.name(),
                a.cost(stateAtSelection)
            ))
            .toList();
    }
}
