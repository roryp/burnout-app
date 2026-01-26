package com.demo.burnout.agent.tools;

import com.demo.burnout.model.Issue;
import com.demo.burnout.service.IssueCache;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j Tool for reading GitHub issues from the cache.
 * 
 * This tool provides the agent with read-only access to issue data.
 * The agent CANNOT modify issues - all mutations go through GOAP actions.
 */
@Component
public class GitHubIssueTool {

    private final IssueCache issueCache;

    public GitHubIssueTool(IssueCache issueCache) {
        this.issueCache = issueCache;
    }

    @Tool("Get all open issues for a repository. Returns issue numbers, titles, labels, and assignees.")
    public String getOpenIssues(@P("The repository in format owner/repo") String repo) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced. Please sync issues first.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        return issues.stream()
            .filter(i -> "open".equals(i.state()))
            .map(this::formatIssue)
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get issues assigned to a specific user")
    public String getIssuesForUser(
            @P("The repository in format owner/repo") String repo,
            @P("The GitHub username") String userId) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        return issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && 
                        i.assignees().stream().anyMatch(a -> a.login().equals(userId)))
            .map(this::formatIssue)
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get critical and urgent issues that need immediate attention")
    public String getCriticalIssues(@P("The repository in format owner/repo") String repo) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        return issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> hasCriticalLabel(i))
            .map(this::formatIssue)
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get issues with missing scope or unclear requirements (mystery meat)")
    public String getMysteryMeatIssues(@P("The repository in format owner/repo") String repo) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo);
        return issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(this::isMysteryMeat)
            .map(this::formatIssue)
            .collect(Collectors.joining("\n"));
    }

    @Tool("Count issues by classification bucket (deep-work, quick-win, maintenance, deferred)")
    public String getIssueCounts(@P("The repository in format owner/repo") String repo) {
        if (!issueCache.hasRepo(repo)) {
            return "Repository not synced.";
        }
        
        List<Issue> issues = issueCache.get(repo).stream()
            .filter(i -> "open".equals(i.state()))
            .toList();
        
        long deepWork = issues.stream().filter(i -> hasLabel(i, "deep-work") || hasLabel(i, "priority:critical")).count();
        long quickWin = issues.stream().filter(i -> hasLabel(i, "quick-win") || hasLabel(i, "size:S")).count();
        long maintenance = issues.stream().filter(i -> hasLabel(i, "maintenance") || hasLabel(i, "chore")).count();
        long total = issues.size();
        long other = total - deepWork - quickWin - maintenance;
        
        return String.format("""
            Issue Distribution:
            - Deep Work: %d (should be ≤1 active)
            - Quick Wins: %d (should be ≤3 per day)
            - Maintenance: %d (should be ≤3 per day)
            - Other/Deferred: %d
            - Total Open: %d
            """, deepWork, quickWin, maintenance, other, total);
    }

    private String formatIssue(Issue issue) {
        String labels = issue.labels() == null ? "" : 
            issue.labels().stream().map(Issue.Label::name).collect(Collectors.joining(", "));
        String assignees = issue.assignees() == null ? "unassigned" :
            issue.assignees().stream().map(Issue.Assignee::login).collect(Collectors.joining(", "));
        return String.format("#%d: %s [%s] @%s", issue.number(), issue.title(), labels, assignees);
    }

    private boolean hasCriticalLabel(Issue issue) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase("priority:critical") || 
                          l.name().equalsIgnoreCase("urgent"));
    }

    private boolean isMysteryMeat(Issue issue) {
        // Empty body or very short title with no labels = mystery meat
        boolean emptyBody = issue.body() == null || issue.body().trim().length() < 20;
        boolean vagueTitle = issue.title() != null && issue.title().length() < 15;
        boolean noLabels = issue.labels() == null || issue.labels().isEmpty();
        return (emptyBody && vagueTitle) || (noLabels && emptyBody);
    }

    private boolean hasLabel(Issue issue, String labelName) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(labelName));
    }
}
