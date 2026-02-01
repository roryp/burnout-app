package com.demo.burnout.agent.supervisor;

import com.demo.burnout.goap.GitHubMutationPlan;
import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Burnout Supervisor Service - LLM-driven workload management using the Supervisor pattern.
 * 
 * This replaces the deterministic GOAPPlanner with an agentic approach where:
 * 1. The LLM scores the developer's stress level
 * 2. The LLM decides which tools to invoke to reduce stress
 * 3. Tool calls generate GitHub mutations to rebalance workload
 * 
 * The supervisor coordinates subagents:
 * - StressScorerAgent: Assesses stress from metrics
 * - WorkloadRebalancerAgent: Uses tools to modify issues
 */
@Service
public class BurnoutSupervisorService {

    private static final Logger log = LoggerFactory.getLogger(BurnoutSupervisorService.class);

    private final ChatModel chatModel;
    private final boolean llmEnabled;

    @Autowired
    public BurnoutSupervisorService(@Autowired(required = false) ChatModel chatModel) {
        this.chatModel = chatModel;
        this.llmEnabled = chatModel != null;
        log.info("BurnoutSupervisorService initialized. LLM enabled: {}", llmEnabled);
    }

    /**
     * Result of supervisor invocation containing explanation and mutation plan.
     */
    public record SupervisorResult(
        String explanation,
        GitHubMutationPlan mutationPlan,
        int estimatedStressScore,
        boolean llmUsed
    ) {
        public static SupervisorResult fallback(String message, int stressScore) {
            return new SupervisorResult(message, GitHubMutationPlan.empty(), stressScore, false);
        }
    }

    /**
     * Run the burnout prevention supervisor on the given workload.
     * 
     * The supervisor will:
     * 1. Analyze the current stress level
     * 2. Invoke tools to reduce stress if needed
     * 3. Return an explanation and mutation plan
     */
    public SupervisorResult preventBurnout(
            WorldState state,
            List<Issue> issues,
            String userId,
            String repo,
            ChaosMetrics chaos) {
        
        if (!llmEnabled) {
            log.warn("LLM not enabled, returning fallback response");
            return generateFallbackResult(state);
        }

        try {
            // Create the mutation tool with access to issues
            BurnoutMutationTool mutationTool = new BurnoutMutationTool(issues, repo);
            
            // Build the WorkloadRebalancerAgent with tools
            WorkloadRebalancerAgent rebalancer = AiServices.builder(WorkloadRebalancerAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();
            
            // Format issues for the prompt
            String issueList = formatIssueList(issues, userId);
            
            // Invoke the rebalancer agent
            log.info("Invoking WorkloadRebalancerAgent for user {} in repo {}", userId, repo);
            
            String explanation = rebalancer.rebalanceWorkload(
                state.calculateStressScore(),
                state.getStressLevel().name(),
                state.totalAssigned(),
                state.deepWorkCount(),
                state.quickWinCount(),
                state.maintenanceCount(),
                state.is333Compliant(),
                chaos.score(),
                state.hasAfterHoursActivity(),
                state.mysteryMeatCount(),
                issueList
            );
            
            // Get the mutation plan from the tool
            GitHubMutationPlan mutationPlan = mutationTool.getMutationPlan();
            
            log.info("Supervisor completed. Actions planned: {}", mutationPlan.actions().size());
            
            // Estimate new stress score based on actions taken
            int estimatedStress = estimateReducedStress(state, mutationPlan);
            
            return new SupervisorResult(explanation, mutationPlan, estimatedStress, true);
            
        } catch (Exception e) {
            log.error("Supervisor invocation failed: {}", e.getMessage(), e);
            return generateFallbackResult(state);
        }
    }

    /**
     * Format the issue list for the LLM prompt.
     */
    private String formatIssueList(List<Issue> issues, String userId) {
        return issues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equals(userId)))
            .map(i -> String.format("- #%d: %s [%s]%s",
                i.number(),
                i.title(),
                i.labels() != null ? i.labels().stream()
                    .map(Issue.Label::name)
                    .collect(Collectors.joining(", ")) : "no labels",
                i.body() == null || i.body().isBlank() ? " (no description)" : ""
            ))
            .collect(Collectors.joining("\n"));
    }

    /**
     * Estimate reduced stress score based on planned mutations.
     */
    private int estimateReducedStress(WorldState state, GitHubMutationPlan plan) {
        int currentStress = state.calculateStressScore();
        int actionCount = plan.actions().size();
        
        // Rough estimate: each action reduces stress by 5-10 points
        int reduction = actionCount * 7;
        return Math.max(0, currentStress - reduction);
    }

    /**
     * Generate fallback result when LLM is unavailable.
     */
    private SupervisorResult generateFallbackResult(WorldState state) {
        int stress = state.calculateStressScore();
        StringBuilder sb = new StringBuilder();
        
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
        
        sb.append("*LLM agents unavailable - using deterministic fallback*");
        
        return SupervisorResult.fallback(sb.toString(), stress);
    }

    public boolean isLlmEnabled() {
        return llmEnabled;
    }
}
