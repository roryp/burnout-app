package com.demo.burnout.model;

import java.time.Instant;
import java.util.List;

/**
 * GitHub Issue record - fields we analyze from synced issues.
 */
public record Issue(
    int number,
    String title,
    String body,
    List<Label> labels,
    List<Assignee> assignees,
    Instant createdAt,
    Instant updatedAt,
    String state,
    Milestone milestone
) {
    public record Label(String name) {}
    public record Assignee(String login) {}
    public record Milestone(String title, Instant dueOn) {}
}
