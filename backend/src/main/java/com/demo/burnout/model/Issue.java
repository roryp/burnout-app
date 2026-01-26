package com.demo.burnout.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

/**
 * GitHub Issue record - fields we analyze from synced issues.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assignee(String login) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Milestone(String title, Instant dueOn) {}
}
