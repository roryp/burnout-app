package com.demo.burnout.goap;

import java.time.Instant;
import java.util.List;

/**
 * GitHub mutations that the extension will execute.
 */
public sealed interface GitHubAction permits 
    GitHubAction.AddLabels, 
    GitHubAction.RemoveLabels, 
    GitHubAction.Comment,
    GitHubAction.Unassign,
    GitHubAction.SetBody,
    GitHubAction.SetUpdatedAt {
    
    int issueNumber();
    String type();

    record AddLabels(int issueNumber, List<String> labels) implements GitHubAction {
        @Override public String type() { return "AddLabels"; }
    }

    record RemoveLabels(int issueNumber, List<String> labels) implements GitHubAction {
        @Override public String type() { return "RemoveLabels"; }
    }

    record Comment(int issueNumber, String body) implements GitHubAction {
        @Override public String type() { return "Comment"; }
    }

    record Unassign(int issueNumber, String login) implements GitHubAction {
        @Override public String type() { return "Unassign"; }
    }

    /**
     * Demo-only: rewrite the issue body to defuse "mystery meat" chaos.
     * Real GitHub sync would translate this to a description PR or
     * comment-prompt, never an in-place body rewrite.
     */
    record SetBody(int issueNumber, String body) implements GitHubAction {
        @Override public String type() { return "SetBody"; }
    }

    /**
     * Demo-only: rewrite the in-memory updatedAt timestamp so chaos
     * metrics that depend on after-hours / recently-touched counts
     * stop firing. Has no real GitHub API equivalent.
     */
    record SetUpdatedAt(int issueNumber, Instant updatedAt) implements GitHubAction {
        @Override public String type() { return "SetUpdatedAt"; }
    }
}
