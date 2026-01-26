package com.demo.burnout.service;

import com.demo.burnout.model.Issue;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IssueCache {
    private final Map<String, CachedIssues> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();

    public void put(String repo, List<Issue> issues, Instant fetchedAt) {
        cache.put(repo, new CachedIssues(issues, fetchedAt));
        versions.computeIfAbsent(repo, r -> new AtomicLong()).incrementAndGet();
    }

    public List<Issue> get(String repo) {
        CachedIssues c = cache.get(repo);
        return c == null ? List.of() : c.issues();
    }

    public boolean hasRepo(String repo) {
        return cache.containsKey(repo);
    }

    public long getVersion(String repo) {
        return versions.getOrDefault(repo, new AtomicLong(0)).get();
    }

    record CachedIssues(List<Issue> issues, Instant fetchedAt) {}
}
