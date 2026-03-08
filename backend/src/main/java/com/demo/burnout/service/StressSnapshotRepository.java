package com.demo.burnout.service;

import com.demo.burnout.model.StressSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface StressSnapshotRepository extends JpaRepository<StressSnapshot, Long> {

    List<StressSnapshot> findByUserIdAndRepoOrderByRecordedAtDesc(String userId, String repo);

    List<StressSnapshot> findByRepoOrderByRecordedAtDesc(String repo);

    List<StressSnapshot> findByRecordedAtBetweenOrderByRecordedAtAsc(Instant from, Instant to);

    List<StressSnapshot> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            String userId, Instant from, Instant to);
}
