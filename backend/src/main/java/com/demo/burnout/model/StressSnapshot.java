package com.demo.burnout.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persisted stress score snapshot for longitudinal study tracking.
 * Records each stress measurement so trends can be analyzed over time.
 */
@Entity
@Table(name = "stress_snapshots", indexes = {
    @Index(name = "idx_stress_user_repo", columnList = "userId, repo"),
    @Index(name = "idx_stress_timestamp", columnList = "recordedAt")
})
public class StressSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String repo;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private int stressScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StressLevel stressLevel;

    @Column(nullable = false, length = 20)
    private String source;

    private int workloadStress;
    private int chaosStress;
    private int contextSwitchingStress;
    private int clarityStress;
    private int sustainedStress;
    private int afterHoursStress;

    protected StressSnapshot() {}

    public StressSnapshot(String userId, String repo, Instant recordedAt,
                          int stressScore, StressLevel stressLevel, String source,
                          int workloadStress, int chaosStress, int contextSwitchingStress,
                          int clarityStress, int sustainedStress, int afterHoursStress) {
        this.userId = userId;
        this.repo = repo;
        this.recordedAt = recordedAt;
        this.stressScore = stressScore;
        this.stressLevel = stressLevel;
        this.source = source;
        this.workloadStress = workloadStress;
        this.chaosStress = chaosStress;
        this.contextSwitchingStress = contextSwitchingStress;
        this.clarityStress = clarityStress;
        this.sustainedStress = sustainedStress;
        this.afterHoursStress = afterHoursStress;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getRepo() { return repo; }
    public Instant getRecordedAt() { return recordedAt; }
    public int getStressScore() { return stressScore; }
    public StressLevel getStressLevel() { return stressLevel; }
    public String getSource() { return source; }
    public int getWorkloadStress() { return workloadStress; }
    public int getChaosStress() { return chaosStress; }
    public int getContextSwitchingStress() { return contextSwitchingStress; }
    public int getClarityStress() { return clarityStress; }
    public int getSustainedStress() { return sustainedStress; }
    public int getAfterHoursStress() { return afterHoursStress; }
}
