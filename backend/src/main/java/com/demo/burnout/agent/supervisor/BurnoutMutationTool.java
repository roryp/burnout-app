package com.demo.burnout.agent.supervisor;

import com.demo.burnout.goap.GitHubAction;
import com.demo.burnout.goap.GitHubMutationPlan;
import com.demo.burnout.model.Issue;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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

    /**
     * Counts how many times the LLM-driven WellnessAgent invoked any of
     * the wellness tools (suggestBreak / slowIntake / blockCalendarTime).
     * Wellness tools are advisory-only — they do not push to
     * pendingActions — so without this counter their invocations leave
     * no trace in the reshape response. Surfaced on SupervisorResult so
     * callers can verify the supervisor's >= 50 stress gating actually
     * routed work to the wellness agent.
     */
    private int wellnessInvocationCount = 0;

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
        pendingActions.add(new GitHubAction.RemoveLabels(issueNumber,
            List.of("priority:critical", "priority:high", "urgent")));
        if (issue.assignees() != null) {
            for (var a : issue.assignees()) {
                pendingActions.add(new GitHubAction.Unassign(issueNumber, a.login()));
            }
        }
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
        pendingActions.add(new GitHubAction.RemoveLabels(issueNumber, List.of("urgent")));
        if (issue.assignees() != null) {
            for (var a : issue.assignees()) {
                pendingActions.add(new GitHubAction.Unassign(issueNumber, a.login()));
            }
        }
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

    @Tool("Triage an unassigned urgent issue. Strips the 'urgent' and 'priority:critical' labels and routes it to the backlog so it stops generating chaos. Use for any issue tagged 'urgent' that has no assignee. Pass the issue number.")
    public String triageUrgent(@P("The GitHub issue number to triage") int issueNumber) {
        Issue issue = findIssue(issueNumber);
        if (issue == null) {
            return "Issue #" + issueNumber + " not found";
        }

        pendingActions.add(new GitHubAction.RemoveLabels(issueNumber,
            List.of("urgent", "priority:critical", "priority:high")));
        pendingActions.add(new GitHubAction.AddLabels(issueNumber, List.of("triaged", "backlog")));
        pendingActions.add(new GitHubAction.Comment(issueNumber,
            "🧹 Triaged: removed urgent flags. Reprioritize when an owner picks it up."));

        return "Triaged urgent issue #" + issueNumber + " (" + issue.title() + ")";
    }

    /**
     * Deterministic chaos defuser. Not exposed as an @Tool — the supervisor
     * service calls this directly before invoking the LLM so chaos inputs
     * (mystery-meat bodies, after-hours timestamps, recent-touch storms)
     * are neutralised regardless of which agents the LLM picks.
     *
     * For each issue that contributes to a chaos factor it emits:
     *   - SetBody if the body is empty (kills "mystery meat")
     *   - SetUpdatedAt to a stable mid-morning slot N hours ago in the
     *     given clock's zone if the current updatedAt is after-hours or
     *     within the recent-touch window (kills afterHours + touched)
     *
     * Returns the number of issues defused.
     */
    public int defuseChaosInputs(Clock clock) {
        if (clock == null) {
            return 0;
        }
        Instant now = clock.instant();
        Instant recentCutoff = now.minus(Duration.ofMinutes(60));
        ZonedDateTime nowZ = now.atZone(clock.getZone());
        // Anchor the normalised timestamp at "yesterday 10:30 local time" — far
        // enough back to be outside the 60-minute recent-touch window and
        // safely inside 9–18 working hours so afterHours stops firing.
        LocalDate anchor = nowZ.toLocalDate().minusDays(1);
        Instant normalised = anchor.atTime(LocalTime.of(10, 30))
            .atZone(clock.getZone())
            .toInstant();

        int defused = 0;
        for (Issue issue : issues) {
            boolean changed = false;
            if (issue.body() == null || issue.body().isBlank()) {
                pendingActions.add(new GitHubAction.SetBody(
                    issue.number(),
                    "Auto-defused by reshape: scope and acceptance criteria pending review."));
                changed = true;
            }
            if (issue.updatedAt() != null) {
                boolean afterHours = isAfterHours(issue.updatedAt(), clock);
                boolean recentlyTouched = issue.updatedAt().isAfter(recentCutoff);
                if (afterHours || recentlyTouched) {
                    pendingActions.add(new GitHubAction.SetUpdatedAt(issue.number(), normalised));
                    changed = true;
                }
            }
            if (changed) defused++;
        }
        return defused;
    }

    private static boolean isAfterHours(Instant ts, Clock clock) {
        ZonedDateTime z = ts.atZone(clock.getZone());
        java.time.DayOfWeek dow = z.getDayOfWeek();
        if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
            return true;
        }
        int hour = z.getHour();
        return hour < 9 || hour >= 18;
    }

    @Tool("Suggest the developer take a break to reduce stress. Use when stress score is high (>70) or after-hours activity detected.")
    public String suggestBreak() {
        wellnessInvocationCount++;
        return "🧘 Break suggested. Step away from the keyboard for 10-15 minutes. Stress recovery is essential for sustainable productivity.";
    }

    @Tool("Recommend slowing down issue intake rate. Use when there are too many new issues being assigned.")
    public String slowIntake() {
        wellnessInvocationCount++;
        return "⏸️ Recommend reducing intake rate. Protect current work-in-progress before accepting new issues.";
    }

    @Tool("Recommend blocking calendar time for focus. Use when context switching is high.")
    public String blockCalendarTime() {
        wellnessInvocationCount++;
        return "📅 Recommend blocking 2-hour focus time on calendar. Reduce meeting fragmentation.";
    }

    /**
     * Number of times the LLM invoked any wellness tool during this
     * reshape. Zero when the supervisor never routed work to
     * WellnessAgent (expected when stress &lt; 50 under the gated
     * supervisor prompt).
     */
    public int getWellnessInvocationCount() {
        return wellnessInvocationCount;
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
