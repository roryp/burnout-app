package com.demo.burnout.agent;

import com.demo.burnout.goap.GoapActionPlan;
import com.demo.burnout.model.ComplianceReport;
import com.demo.burnout.model.WorldState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Agent Orchestrator - coordinates all AI agents for burnout prevention.
 * 
 * This service orchestrates the following pattern:
 * 1. Deterministic services calculate metrics and GOAP plans
 * 2. AI agents provide human-friendly explanations and emotional support
 * 
 * The agents NEVER make decisions - they only explain and support.
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final ExplainerAiService explainerAiService;
    private final ProtectiveAiService protectiveAiService;
    private final FridayDeployAiService fridayDeployAiService;
    private final Clock clock;
    private final boolean llmEnabled;

    @Autowired
    public AgentOrchestrator(
            @Autowired(required = false) ExplainerAiService explainerAiService,
            @Autowired(required = false) ProtectiveAiService protectiveAiService,
            @Autowired(required = false) FridayDeployAiService fridayDeployAiService,
            Clock clock) {
        this.explainerAiService = explainerAiService;
        this.protectiveAiService = protectiveAiService;
        this.fridayDeployAiService = fridayDeployAiService;
        this.clock = clock;
        this.llmEnabled = explainerAiService != null;
        
        log.info("AgentOrchestrator initialized. LLM enabled: {}", llmEnabled);
    }

    /**
     * Generate a human-friendly explanation of a GOAP action plan.
     */
    public String explainPlan(WorldState state, GoapActionPlan plan, String primaryGoal) {
        if (!llmEnabled) {
            return generateFallbackExplanation(state, plan, primaryGoal);
        }

        try {
            String actionsList = plan.actions().isEmpty() 
                ? "No actions needed"
                : plan.actions().stream()
                    .map(a -> "- " + a.name())
                    .collect(Collectors.joining("\n"));

            return explainerAiService.explainPlan(
                state.calculateStressScore(),
                state.getStressLevel().name(),
                state.deepWorkCount(),
                state.quickWinCount(),
                state.maintenanceCount(),
                state.totalAssigned(),
                state.is333Compliant(),
                primaryGoal,
                plan.actions().size(),
                actionsList,
                plan.expectedStressScore()
            );
        } catch (Exception e) {
            log.warn("LLM call failed, using fallback: {}", e.getMessage());
            return generateFallbackExplanation(state, plan, primaryGoal);
        }
    }

    /**
     * Generate a protective response based on stress signals.
     */
    public ProtectiveResponse generateProtectiveResponse(WorldState state, int consecutiveHighDays) {
        LocalDateTime now = LocalDateTime.now(clock);
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String timeOfDay = now.getHour() < 12 ? "morning" : now.getHour() < 17 ? "afternoon" : "evening";

        if (!llmEnabled) {
            return generateFallbackProtectiveResponse(state, consecutiveHighDays, dayOfWeek);
        }

        try {
            String message = protectiveAiService.generateProtectiveResponse(
                consecutiveHighDays,
                state.hasAfterHoursActivity(),
                (int) state.contextSwitchCount(),
                state.blockedCount(),
                (int) state.contextSwitchCount(),
                state.calculateStressScore(),
                dayOfWeek,
                timeOfDay
            );
            return new ProtectiveResponse(shouldTriggerProtection(state, consecutiveHighDays), message);
        } catch (Exception e) {
            log.warn("LLM call failed, using fallback: {}", e.getMessage());
            return generateFallbackProtectiveResponse(state, consecutiveHighDays, dayOfWeek);
        }
    }

    /**
     * Generate Friday deploy readiness assessment.
     */
    public String assessFridayDeploy(WorldState state, ComplianceReport compliance, 
                                      int fridayScore, int criticalOpen, int staleUrgents,
                                      boolean deepWorkDone, double chaosScore) {
        if (!llmEnabled) {
            return generateFallbackFridayAssessment(fridayScore, criticalOpen, staleUrgents);
        }

        try {
            return fridayDeployAiService.assessDeployReadiness(
                fridayScore,
                criticalOpen,
                staleUrgents,
                deepWorkDone,
                chaosScore,
                state.hasAfterHoursActivity(),
                compliance.complianceScore(),
                state.getStressLevel().name()
            );
        } catch (Exception e) {
            log.warn("LLM call failed, using fallback: {}", e.getMessage());
            return generateFallbackFridayAssessment(fridayScore, criticalOpen, staleUrgents);
        }
    }

    /**
     * Check if LLM agents are enabled.
     */
    public boolean isLlmEnabled() {
        return llmEnabled;
    }

    // ======================== Fallback Implementations ========================

    private String generateFallbackExplanation(WorldState state, GoapActionPlan plan, String primaryGoal) {
        StringBuilder sb = new StringBuilder();
        
        int stress = state.calculateStressScore();
        if (stress >= 70) {
            sb.append("🔴 **Critical stress detected.** ");
        } else if (stress >= 50) {
            sb.append("🟡 **Elevated stress levels.** ");
        } else {
            sb.append("🟢 **Stress levels manageable.** ");
        }
        sb.append("Current stress score: ").append(stress).append("/100\n\n");
        
        if (!state.is333Compliant()) {
            sb.append("⚠️ Your workload exceeds the 3-3-3 structure. ");
            sb.append("You have ").append(state.deepWorkCount()).append(" deep work items (max 1), ");
            sb.append(state.quickWinCount()).append(" quick wins (max 3), ");
            sb.append("and ").append(state.maintenanceCount()).append(" maintenance tasks (max 3).\n\n");
        } else {
            sb.append("✅ You're within the 3-3-3 structure. Good balance!\n\n");
        }
        
        if (!plan.isEmpty()) {
            sb.append("### Recommended Actions\n\n");
            sb.append("The following ").append(plan.actions().size()).append(" actions will reduce stress from ");
            sb.append(plan.initialStressScore()).append(" to ").append(plan.expectedStressScore()).append(":\n\n");
            for (var action : plan.actions()) {
                sb.append("- **").append(action.name()).append("**\n");
            }
        } else {
            sb.append("No actions needed - your day is already well-structured! 🎉\n");
        }
        
        return sb.toString();
    }

    private ProtectiveResponse generateFallbackProtectiveResponse(WorldState state, int consecutiveHighDays, String dayOfWeek) {
        boolean shouldProtect = shouldTriggerProtection(state, consecutiveHighDays);
        
        if (!shouldProtect) {
            return new ProtectiveResponse(false, "You're doing well! Keep up the balanced approach. 💪");
        }
        
        StringBuilder message = new StringBuilder();
        
        if (consecutiveHighDays >= 3) {
            message.append("You've had elevated stress for ").append(consecutiveHighDays)
                   .append(" days. Consider taking a short break or delegating some tasks. ");
        }
        
        if (state.hasAfterHoursActivity()) {
            message.append("I noticed after-hours activity - try to protect your personal time. ");
        }
        
        if (state.calculateStressScore() >= 70) {
            message.append("Your stress level is high. The most impactful thing you can do right now is defer one non-critical item.");
        }
        
        if (message.length() == 0) {
            message.append("Your workload is heavy today. Remember: sustainable pace > heroic effort.");
        }
        
        return new ProtectiveResponse(true, message.toString().trim());
    }

    private String generateFallbackFridayAssessment(int fridayScore, int criticalOpen, int staleUrgents) {
        if (fridayScore >= 80) {
            return "🟢 **Deploy with confidence!** Your score of " + fridayScore + "/100 indicates a well-managed week. " +
                   "Critical items are resolved and stress levels are low.";
        } else if (fridayScore >= 50) {
            return "🟡 **Proceed with caution.** Score: " + fridayScore + "/100. " +
                   (criticalOpen > 0 ? "You have " + criticalOpen + " critical issues open. " : "") +
                   (staleUrgents > 0 ? staleUrgents + " urgent items haven't been updated recently. " : "") +
                   "Consider addressing these before deploying.";
        } else {
            return "🔴 **Consider deferring to Monday.** Score: " + fridayScore + "/100. " +
                   "Multiple risk factors detected. A Monday deploy gives you buffer time if issues arise.";
        }
    }

    private boolean shouldTriggerProtection(WorldState state, int consecutiveHighDays) {
        return consecutiveHighDays >= 2 
            || state.hasAfterHoursActivity() 
            || state.calculateStressScore() >= 70
            || state.totalAssigned() > 10;
    }

    /**
     * Protective response record.
     */
    public record ProtectiveResponse(boolean triggered, String message) {}
}
