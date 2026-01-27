package com.demo.burnout.dto;

import com.demo.burnout.model.ChaosMetrics;

public record ChaosResponse(
        String status,
        long issuesTouchedRecently,
        long unresolvedUrgent,
        int distinctLabelCount,
        boolean afterHoursSignal,
        int mysteryMeatCount,
        double score,
        int schemaVersion
) {
    public static ChaosResponse notSynced() {
        return new ChaosResponse(
                "not_synced",
                0,
                0,
                0,
                false,
                0,
                -1,
                ChaosMetrics.SCHEMA_VERSION
        );
    }

    public static ChaosResponse from(ChaosMetrics m) {
        return new ChaosResponse(
                "ok",
                m.issuesTouchedRecently(),
                m.unresolvedUrgent(),
                m.distinctLabelCount(),
                m.afterHoursSignal(),
                m.mysteryMeatCount(),
                m.score(),
                m.schemaVersion()
        );
    }
}