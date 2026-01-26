package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

public record MarkDeepWorkFocus(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Focus: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "MarkDeepWorkFocus#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return s.deepWorkCount() == 0 ? 5 : 25; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return s.deepWorkCount() == 0 && 
               LabelUtils.hasLabel(issue, "priority:critical", "architecture");
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withDeepWorkCount(1).recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("deep-work", "focus")),
            new GitHubAction.Comment(issue.number(), "🎯 Marked as today's deep work focus. Protect this time.")
        );
    }
}
