package com.demo.burnout.goap;

import java.util.List;

/**
 * GitHub mutations that the extension will execute.
 */
public sealed interface GitHubAction permits 
    GitHubAction.AddLabels, 
    GitHubAction.RemoveLabels, 
    GitHubAction.Comment {
    
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
}
