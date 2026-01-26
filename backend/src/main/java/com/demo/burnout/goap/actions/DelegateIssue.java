package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

/**
 * GOAP Action: Delegate an issue to redistribute workload.
 * Marks issues for delegation when user is overloaded.
 */
public record DelegateIssue(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Delegate: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "DelegateIssue#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        // Lower cost when overloaded (more beneficial to delegate)
        return s.totalAssigned() > 7 ? 3 : 12; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        // Can delegate if: not critical, not already delegated, user has multiple issues
        return !LabelUtils.hasLabel(issue, "priority:critical") 
            && !LabelUtils.hasLabel(issue, "delegated")
            && s.totalAssigned() > 3;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withTotalAssigned(s.totalAssigned() - 1)
                .recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("delegated", "needs-owner")),
            new GitHubAction.Comment(issue.number(), "🤝 Marked for delegation to balance workload.")
        );
    }
}
