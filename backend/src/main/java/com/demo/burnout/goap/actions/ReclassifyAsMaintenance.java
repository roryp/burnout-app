package com.demo.burnout.goap.actions;

import com.demo.burnout.goap.Action;
import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.LabelUtils;

import java.util.List;

public record ReclassifyAsMaintenance(Issue issue) implements Action {
    @Override 
    public String name() { 
        return "Maintenance: " + issue.title(); 
    }
    
    @Override 
    public String id() { 
        return "ReclassifyAsMaintenance#" + issue.number(); 
    }
    
    @Override 
    public int cost(WorldState s) { 
        return s.maintenanceCount() >= 3 ? 20 : 6; 
    }
    
    @Override 
    public boolean preconditionsMet(WorldState s) {
        return LabelUtils.hasLabel(issue, "documentation", "tech-debt", "cleanup", "routine")
            && s.maintenanceCount() < 3;
    }
    
    @Override 
    public WorldState apply(WorldState s) {
        return s.withMaintenanceCount(s.maintenanceCount() + 1)
                .recalculateCompliance();
    }
    
    @Override 
    public List<GitHubAction> toGitHubActions() {
        return List.of(
            new GitHubAction.AddLabels(issue.number(), List.of("maintenance", "3-3-3")),
            new GitHubAction.Comment(issue.number(), "🔧 Classified as maintenance task for 3-3-3 plan.")
        );
    }
}
