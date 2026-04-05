package com.demo.burnout.controller;

import com.demo.burnout.model.StressLevel;
import com.demo.burnout.model.StressSnapshot;
import com.demo.burnout.service.StressSnapshotRepository;
import com.demo.burnout.service.StressSnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Study data export endpoints for the burnout research study.
 * Placed under /demo/** so they are accessible without auth for researchers.
 */
@RestController
@RequestMapping("/demo/api/study")
@CrossOrigin(origins = "*")
public class StudyExportController {

    private final StressSnapshotService snapshotService;
    private final StressSnapshotRepository snapshotRepository;

    public StudyExportController(StressSnapshotService snapshotService,
                                 StressSnapshotRepository snapshotRepository) {
        this.snapshotService = snapshotService;
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping("/snapshots")
    public List<StressSnapshot> getSnapshots(
            @RequestParam(required = false) String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        if (userId != null && !userId.isBlank()) {
            return snapshotService.getExportDataForUser(userId, fromInstant, toInstant);
        }
        return snapshotService.getExportData(fromInstant, toInstant);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String userId) {

        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<StressSnapshot> data = (userId != null && !userId.isBlank())
                ? snapshotService.getExportDataForUser(userId, fromInstant, toInstant)
                : snapshotService.getExportData(fromInstant, toInstant);

        StringBuilder csv = new StringBuilder();
        csv.append("id,userId,repo,recordedAt,stressScore,stressLevel,source,")
           .append("workload,chaos,contextSwitching,clarity,sustained,afterHours,")
           .append("selfReportedScore,selfReportedNote\n");

        for (StressSnapshot s : data) {
            csv.append(s.getId()).append(',')
               .append(s.getUserId()).append(',')
               .append(s.getRepo()).append(',')
               .append(s.getRecordedAt()).append(',')
               .append(s.getStressScore()).append(',')
               .append(s.getStressLevel()).append(',')
               .append(s.getSource()).append(',')
               .append(s.getWorkloadStress()).append(',')
               .append(s.getChaosStress()).append(',')
               .append(s.getContextSwitchingStress()).append(',')
               .append(s.getClarityStress()).append(',')
               .append(s.getSustainedStress()).append(',')
               .append(s.getAfterHoursStress()).append(',')
               .append(s.getSelfReportedScore() != null ? s.getSelfReportedScore() : "").append(',')
               .append(escapeCsv(s.getSelfReportedNote())).append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stress-snapshots.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Reset all stress snapshots. Used before re-seeding demo data.
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSnapshots() {
        long count = snapshotRepository.count();
        snapshotRepository.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    /**
     * Seed dummy stress snapshots for demo purposes.
     * Clears existing snapshots first, then creates 14 days of data
     * for 5 simulated participants with realistic stress curves.
     * roryp starts high (~95) and stays high — the real reshape creates the dramatic drop.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedDummyData() {
        snapshotRepository.deleteAllInBatch();
        Random rng = new Random(42);
        String repo = "roryp/burnout-app";
        // roryp starts high (95) and stays high — the real reshape will create the dramatic drop
        String[] users = {"alice", "bob", "carol", "dave", "roryp"};
        int[] baselines = {25, 55, 70, 40, 95};       // starting stress
        int[] deltas    = {-1,  2, -3,  0,  0};        // daily drift

        Instant now = Instant.now();
        Instant start = now.minus(java.time.Duration.ofDays(14));
        List<StressSnapshot> snapshots = new ArrayList<>();

        for (int u = 0; u < users.length; u++) {
            int score = baselines[u];
            for (int day = 0; day < 14; day++) {
                // 1-2 snapshots per day
                int checksPerDay = 1 + rng.nextInt(2);
                for (int c = 0; c < checksPerDay; c++) {
                    long offsetSeconds = (long) day * 86400 + 9 * 3600 + rng.nextInt(8 * 3600);
                    Instant ts = start.plusSeconds(offsetSeconds);
                    int jitter = rng.nextInt(11) - 5; // -5 to +5
                    int finalScore = Math.max(5, Math.min(95, score + jitter));
                    StressLevel level = finalScore >= 70 ? StressLevel.HIGH
                            : finalScore >= 40 ? StressLevel.MODERATE : StressLevel.LOW;
                    String source = rng.nextBoolean() ? "stress" : "reshape";
                    int workload = Math.max(0, finalScore / 4 + rng.nextInt(5) - 2);
                    int chaos = Math.max(0, finalScore / 5 + rng.nextInt(4) - 1);
                    int context = Math.max(0, finalScore / 6 + rng.nextInt(3));
                    int clarity = Math.max(0, rng.nextInt(Math.max(1, finalScore / 8)));
                    int sustained = Math.max(0, rng.nextInt(Math.max(1, finalScore / 7)));
                    int afterHrs = rng.nextInt(3);

                    snapshots.add(new StressSnapshot(users[u], repo, ts,
                            finalScore, level, source,
                            workload, chaos, context, clarity, sustained, afterHrs));
                }
                score = Math.max(5, Math.min(95, score + deltas[u]));
            }
        }
        snapshotRepository.saveAll(snapshots);

        return ResponseEntity.ok(Map.of(
                "seeded", snapshots.size(),
                "users", List.of(users),
                "days", 14));
    }
}
