package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

public record AddScopeToIssue(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Add scope: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "AddScopeToIssue#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return 12; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return (issue.body() == null || issue.body().isBlank()) 
            && !LabelUtils.hasLabel(issue, "size:S", "size:M", "size:L", "scope-defined");
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withMysteryMeatCount(s.mysteryMeatCount() - 1)
                .recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("needs-scope", "blocked")),
            new GitHubAction.Comment(issue.number(), "📋 Needs clearer scope before starting. What does 'done' look like?")
        );
    }
}
