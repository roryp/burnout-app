package com.demo.burnout.controller;

import com.demo.burnout.model.Issue;
import com.demo.burnout.service.IssueCache;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*")
public class IssueSyncController {
    
    private final IssueCache issueCache;

    public IssueSyncController(IssueCache issueCache) {
        this.issueCache = issueCache;
    }

    @PostMapping("/sync")
    public SyncAck sync(@RequestBody IssueSyncRequest req) {
        if (req.schemaVersion() != IssueSyncRequest.SCHEMA_VERSION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Schema version mismatch: expected " + IssueSyncRequest.SCHEMA_VERSION + 
                ", got " + req.schemaVersion());
        }
        issueCache.put(req.repo(), req.issues(), req.fetchedAt());
        return new SyncAck(req.repo(), req.issues().size(), req.fetchedAt(), issueCache.getVersion(req.repo()));
    }

    public record IssueSyncRequest(String repo, List<Issue> issues, Instant fetchedAt, int schemaVersion) {
        public static final int SCHEMA_VERSION = 1;
    }

    public record SyncAck(String repo, int receivedCount, Instant fetchedAt, long cacheVersion) {}
}
