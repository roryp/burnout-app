package com.demo.burnout.util;

import com.demo.burnout.model.ComplianceReport;

/**
 * Single source of truth for the Friday deploy-readiness score (0-100).
 *
 * <p>Designed around four signals that genuinely affect whether it is safe to
 * ship on a Friday afternoon. Each signal is counted at most once -- the
 * previous implementation passed the rolled-up {@code chaos.score} as a cost
 * input alongside individual penalties for after-hours, mystery meat, and
 * urgent-unassigned, which double-counted because those same three signals
 * are also components of {@code chaos.score}. The chaos score remains in the
 * stress breakdown surfaced to the user; it is just not consumed here.</p>
 *
 * <p>The two chaos components intentionally <em>not</em> represented are
 * {@code distinctLabelCount} (better organisation, not a deploy risk) and
 * {@code issuesTouchedToday} (normal dev activity, not a deploy risk).</p>
 *
 * <p>Day-plan compliance uses the 1-3-3-0 structural definition
 * (deepWork &le; 1, quickWins &le; 3, maintenance &le; 3) instead of
 * {@link ComplianceReport#isCompliant()}, which also fires on signals like
 * {@code EXCESSIVE_CONTEXT_SWITCHING} that are already inside the chaos
 * score.</p>
 *
 * <h2>Penalties</h2>
 * <ul>
 *   <li>Day plan not in 1-3-3-0 shape: -25 (primary deploy gate)</li>
 *   <li>More than 3 unscoped issues (empty body): -20</li>
 *   <li>Any unscoped issue (1-3): -10</li>
 *   <li>Any unassigned urgent / priority:critical: -15</li>
 *   <li>Sustained after-hours activity (binary): -10</li>
 * </ul>
 */
public final class FridayScoreFormula {

    private FridayScoreFormula() {}

    /**
     * Compute the Friday deploy-readiness score (0-100).
     *
     * @param dayPlan1330Compliant true when deepWork &le; 1, quickWins &le; 3, maintenance &le; 3
     * @param mysteryMeatByEmptyBody count of open issues whose body is empty (unscoped work)
     * @param urgentUnassignedCount count of issues labelled urgent / priority:critical with no assignee
     * @param afterHoursSignal whether sustained after-hours activity is present (binary signal)
     */
    public static int compute(boolean dayPlan1330Compliant,
                              int mysteryMeatByEmptyBody,
                              int urgentUnassignedCount,
                              boolean afterHoursSignal) {
        int score = 100;
        if (!dayPlan1330Compliant) score -= 25;
        if (mysteryMeatByEmptyBody > 3) score -= 20;
        else if (mysteryMeatByEmptyBody > 0) score -= 10;
        if (urgentUnassignedCount > 0) score -= 15;
        if (afterHoursSignal) score -= 10;
        return Math.max(0, score);
    }

    /**
     * Convenience overload for callers that don't pre-compute the 1-3-3-0
     * boolean. Reads the per-bucket counts from a {@link ComplianceReport}
     * and applies the structural check.
     */
    public static int compute(ComplianceReport compliance,
                              int mysteryMeatByEmptyBody,
                              int urgentUnassignedCount,
                              boolean afterHoursSignal) {
        int deepWork    = compliance.bucketCounts().getOrDefault("deepWork", 0);
        int quickWins   = compliance.bucketCounts().getOrDefault("quickWins", 0);
        int maintenance = compliance.bucketCounts().getOrDefault("maintenance", 0);
        boolean compliant = deepWork <= 1 && quickWins <= 3 && maintenance <= 3;
        return compute(compliant, mysteryMeatByEmptyBody, urgentUnassignedCount, afterHoursSignal);
    }
}
