package com.demo.burnout.agent.supervisor;

import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.goap.GitHubMutationPlan;
import com.demo.burnout.model.Issue;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool class exposing burnout prevention actions to the LLM supervisor.
 * Each @Tool method can be invoked by the supervisor agent to perform
 * GitHub mutations that reduce developer stress.
 */
public class BurnoutMutationTool {

    private final List<Issue> issues;
    private final String repo;
    private final List<GitHubAction> pendingActions = new ArrayList<>();

    public BurnoutMutationTool(List<Issue> issues, String repo) {
        this.issues = issues;
        this.repo = repo;
    }

    @Tool("Defer an issue to next sprint. Use when workload is too high. Reduces assigned count. Pass the issue number.")
    public String deferIssue(@P("The GitHub issue number to defer") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("deferred", "next-sprint")));
        pendingActions.add(new GitHubAction.RemoveLabels(issueNumber, List.of("priority:critical")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "🛡️ Deferred to protect your focus. Revisit next sprint."));
        
        return "Deferred issue #" + issueNumber + " (" + issue.title() + ") to next sprint";
    }

    @Tool("Delegate an issue to redistribute workload. Use when user has too many issues. Pass the issue number.")
    public String delegateIssue(@P("The GitHub issue number to delegate") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("delegated", "needs-owner")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "🤝 Marked for delegation to balance workload."));
        
        return "Delegated issue #" + issueNumber + " (" + issue.title() + ") - needs new owner";
    }

    @Tool("Classify an issue as a quick-win for today's 3-3-3 plan. Quick wins are small tasks under 30 minutes. Pass the issue number.")
    public String classifyAsQuickWin(@P("The GitHub issue number to classify as quick-win") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("quick-win", "size:S")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "⚡ Reclassified as quick win for today's 3-3-3 plan."));
        
        return "Classified issue #" + issueNumber + " (" + issue.title() + ") as quick-win";
    }

    @Tool("Classify an issue as maintenance work for the 3-3-3 plan. Maintenance includes tech debt, docs, cleanup. Pass the issue number.")
    public String classifyAsMaintenance(@P("The GitHub issue number to classify as maintenance") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("maintenance", "3-3-3")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "🔧 Classified as maintenance task for 3-3-3 plan."));
        
        return "Classified issue #" + issueNumber + " (" + issue.title() + ") as maintenance";
    }

    @Tool("Mark an issue as today's deep work focus. Only ONE deep work item per day. Use for critical or architectural work. Pass the issue number.")
    public String markAsDeepWork(@P("The GitHub issue number to mark as deep work focus") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("deep-work", "focus")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "🎯 Marked as today's deep work focus. Protect this time."));
        
        return "Marked issue #" + issueNumber + " (" + issue.title() + ") as deep work focus";
    }

    @Tool("Flag an issue as needing clearer scope before work can begin. Use for vague issues without clear 'done' criteria. Pass the issue number.")
    public String addScopeNeeded(@P("The GitHub issue number that needs scope clarification") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }
        
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("needs-scope", "blocked")));
        pendingActions.add(new GitHubAction.Comment(issueNumber, 
            "📋 Needs clearer scope before starting. What does 'done' look like?"));
        
        return "Flagged issue #" + issueNumber + " (" + issue.title() + ") as needing scope";
    }

    @Tool("Suggest the developer take a break to reduce stress. Use when stress score is high (>70) or after-hours activity detected.")
    public String suggestBreak() {
        return "🧘 Break suggested. Step away from the keyboard for 10-15 minutes. Stress recovery is essential for sustainable productivity.";
    }

    @Tool("Recommend slowing down issue intake rate. Use when there are too many new issues being assigned.")
    public String slowIntake() {
        return "⏸️ Recommend reducing intake rate. Protect current work-in-progress before accepting new issues.";
    }

    @Tool("Recommend blocking calendar time for focus. Use when context switching is high.")
    public String blockCalendarTime() {
        return "📅 Recommend blocking 2-hour focus time on calendar. Reduce meeting fragmentation.";
    }

    /**
     * Get all pending GitHub mutations as a plan.
     */
    public GitHubMutationPlan getMutationPlan() {
        return new GitHubMutationPlan(repo, new ArrayList<>(pendingActions));
    }

    /**
     * Get list of issues available for tool operations.
     */
    public List<Issue> getIssues() {
        return issues;
    }

    private Issue findIssue(int number) {
        return issues.stream()
            .filter(i -> i.number() == number)
            .findFirst()
            .orElse(null);
    }
}
