package com.demo.burnout.agent;

import com.demo.burnout.goap.GoapActionPlan;
import com.demo.burnout.goap.Goal;
import com.demo.burnout.model.WorldState;
import org.springframework.stereotype.Service;

/**
 * Explainer Agent - provides human-friendly narration for GOAP plans.
 * 
 * NOTE: This is a stub implementation. In production, this would use LangChain4j
 * with an LLM (Azure OpenAI, etc.) to generate natural language explanations.
 * The LLM does NOT make decisions - it only explains deterministic results.
 */
@Service
public class ExplainerAgent {

    /**
     * Generate human-friendly explanation for a GOAP action plan.
     * Falls back to structured output if LLM is unavailable.
     */
    public String explain(WorldState state, GoapActionPlan plan, String primaryGoal) {
        try {
            return generateExplanation(state, plan, primaryGoal);
        } catch (Exception e) {
            return "See details below.";
        }
    }

    private String generateExplanation(WorldState state, GoapActionPlan plan, String primaryGoal) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("## Day Reshape Summary\n\n");
        
        // Stress assessment
        int stress = state.calculateStressScore();
        if (stress >= 70) {
            sb.append("🔴 **Critical stress detected.** ");
        } else if (stress >= 50) {
            sb.append("🟡 **Elevated stress levels.** ");
        } else {
            sb.append("🟢 **Stress levels manageable.** ");
        }
        
        sb.append("Current stress score: ").append(stress).append("/100\n\n");
        
        // 3-3-3 compliance
        if (!state.is333Compliant()) {
            sb.append("⚠️ Your workload exceeds the 3-3-3 structure. ");
            sb.append("You have ").append(state.deepWorkCount()).append(" deep work items (max 1), ");
            sb.append(state.quickWinCount()).append(" quick wins (max 3), ");
            sb.append("and ").append(state.maintenanceCount()).append(" maintenance tasks (max 3).\n\n");
        } else {
            sb.append("✅ You're within the 3-3-3 structure. Good balance!\n\n");
        }
        
        // Actions explanation
        if (!plan.isEmpty()) {
            sb.append("### Recommended Actions\n\n");
            sb.append("The following ").append(plan.actions().size()).append(" actions will reduce your stress from ");
            sb.append(plan.initialStressScore()).append(" to ").append(plan.expectedStressScore()).append(":\n\n");
            
            for (var action : plan.actions()) {
                sb.append("- **").append(action.name()).append("** (cost: ").append(action.cost(state)).append(")\n");
            }
        } else {
            sb.append("No actions needed - your day is already well-structured! 🎉\n");
        }
        
        return sb.toString();
    }

    /**
     * Factory method for creating ExplainerAgent with LLM integration.
     * In production, this would configure LangChain4j with Azure OpenAI.
     */
    public static ExplainerAgent withLlm(String apiKey, String endpoint) {
        // TODO: Implement LangChain4j integration
        // For now, return basic implementation
        return new ExplainerAgent();
    }
}
