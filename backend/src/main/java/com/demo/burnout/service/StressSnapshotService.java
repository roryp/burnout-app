package com.demo.burnout.service;

import com.demo.burnout.model.StressLevel;
import com.demo.burnout.model.StressSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class StressSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(StressSnapshotService.class);

    private final StressSnapshotRepository repository;
    private final Clock clock;

    public StressSnapshotService(StressSnapshotRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(String userId, String repo, int stressScore, StressLevel stressLevel,
                       String source, Map<String, Integer> breakdown) {
        StressSnapshot snapshot = new StressSnapshot(
            userId, repo, clock.instant(),
            stressScore, stressLevel, source,
            breakdown.getOrDefault("workload", 0),
            breakdown.getOrDefault("chaos", 0),
            breakdown.getOrDefault("contextSwitching", 0),
            breakdown.getOrDefault("clarity", 0),
            breakdown.getOrDefault("sustained", 0),
            breakdown.getOrDefault("afterHours", 0)
        );
        repository.save(snapshot);
        log.debug("Recorded stress snapshot: user={}, repo={}, score={}, source={}",
                userId, repo, stressScore, source);
    }

    public List<StressSnapshot> getHistory(String userId, String repo) {
        return repository.findByUserIdAndRepoOrderByRecordedAtDesc(userId, repo);
    }

    public List<StressSnapshot> getExportData(Instant from, Instant to) {
        return repository.findByRecordedAtBetweenOrderByRecordedAtAsc(from, to);
    }

    public List<StressSnapshot> getExportDataForUser(String userId, Instant from, Instant to) {
        return repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to);
    }
}
