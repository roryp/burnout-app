package com.demo.burnout.dto;

import com.demo.burnout.model.Issue;
import java.util.List;

public record SyncRequest(String repo, List<Issue> issues) {}
