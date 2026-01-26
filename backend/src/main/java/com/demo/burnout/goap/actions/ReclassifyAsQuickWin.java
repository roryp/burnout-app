package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

public record ReclassifyAsQuickWin(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Quick-win: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "ReclassifyAsQuickWin#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return s.quickWinCount() >= 3 ? 20 : 8; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return issue.body() != null && !issue.body().isBlank() 
            && !LabelUtils.hasLabel(issue, "priority:critical")
            && s.quickWinCount() < 3;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withQuickWinCount(s.quickWinCount() + 1)
                .withDeferredCount(s.deferredCount() - 1)
                .recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("quick-win", "size:S")),
            new GitHubAction.Comment(issue.number(), "⚡ Reclassified as quick win for today's 3-3-3 plan.")
        );
    }
}
