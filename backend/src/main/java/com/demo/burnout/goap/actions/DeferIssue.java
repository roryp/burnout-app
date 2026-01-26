package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

public record DeferIssue(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Defer: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "DeferIssue#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return s.totalAssigned() > 7 ? 5 : 15; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return !LabelUtils.hasLabel(issue, "urgent") && s.totalAssigned() > 1;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withTotalAssigned(s.totalAssigned() - 1)
                .withDeferredCount(s.deferredCount() + 1)
                .recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("deferred", "next-sprint")),
            new GitHubAction.RemoveLabels(issue.number(), List.of("priority:critical")),
            new GitHubAction.Comment(issue.number(), "🛡️ Deferred to protect your focus. Revisit next sprint.")
        );
    }
}
