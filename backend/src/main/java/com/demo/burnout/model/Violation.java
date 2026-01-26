package com.demo.burnout.model;

import java.util.List;

public record Violation(
    ViolationType type,
    Severity severity,
    String message,
    List<Issue> affectedIssues,
    String recommendation,
    String field
) {}
