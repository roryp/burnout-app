package com.demo.burnout.agent.supervisor;

import com.demo.burnout.goap.GitHubMutationPlan;
import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Burnout Supervisor Service - LLM-driven workload management using the SUPERVISOR PATTERN.
 * 
 * Uses AgenticServices.supervisorBuilder() from langchain4j-agentic module for
 * autonomous agent orchestration where:
 * 1. The supervisor (plannerModel) analyzes the request and decides which sub-agents to invoke
 * 2. Sub-agents (chatModel) have access to tools and execute specific burnout prevention actions
 * 3. The supervisor summarizes the results
 * 
 * Sub-agents:
 * - DeferAgent: Defers non-critical issues to next sprint
 * - DelegateAgent: Redistributes workload across team
 * - ClassifyAgent: Organizes issues for 3-3-3 compliance
 * - ScopeAgent: Flags unclear issues needing definition
 * - WellnessAgent: Provides stress reduction recommendations
 */
@Service
public class BurnoutSupervisorService {

    private static final Logger log = LoggerFactory.getLogger(BurnoutSupervisorService.class);

    private final ChatModel chatModel;
    private final ChatModel plannerModel;
    private final boolean llmEnabled;

    @Autowired
    public BurnoutSupervisorService(
            @Autowired(required = false) ChatModel chatModel,
            @Autowired(required = false) @Qualifier("plannerModel") ChatModel plannerModel) {
        this.chatModel = chatModel;
        this.plannerModel = plannerModel != null ? plannerModel : chatModel;
        this.llmEnabled = chatModel != null;
        log.info("BurnoutSupervisorService initialized. LLM enabled: {}, Supervisor pattern: {}",
            llmEnabled, plannerModel != null ? "ACTIVE" : "FALLBACK");
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
     * SUPERVISOR PATTERN implementation using AgenticServices.supervisorBuilder():
     * 1. Build sub-agents with chatModel and tools
     * 2. Build supervisor with plannerModel that coordinates sub-agents
     * 3. Supervisor autonomously plans and invokes sub-agents based on the request
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
            
            log.info("Building Supervisor pattern for user {} in repo {}", userId, repo);

            // Build sub-agents using AgenticServices.agentBuilder() with tools
            BurnoutAgents.DeferAgent deferAgent = AgenticServices
                .agentBuilder(BurnoutAgents.DeferAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();
            
            BurnoutAgents.DelegateAgent delegateAgent = AgenticServices
                .agentBuilder(BurnoutAgents.DelegateAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();
            
            BurnoutAgents.ClassifyAgent classifyAgent = AgenticServices
                .agentBuilder(BurnoutAgents.ClassifyAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();
            
            BurnoutAgents.ScopeAgent scopeAgent = AgenticServices
                .agentBuilder(BurnoutAgents.ScopeAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();
            
            BurnoutAgents.WellnessAgent wellnessAgent = AgenticServices
                .agentBuilder(BurnoutAgents.WellnessAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();

            // Build supervisor using AgenticServices.supervisorBuilder() with sub-agents
            // The supervisor uses plannerModel to decide which sub-agents to invoke
            SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(deferAgent, delegateAgent, classifyAgent, scopeAgent, wellnessAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

            log.info("Invoking Supervisor to orchestrate burnout prevention agents");

            // Format issues for the supervisor prompt
            String issueList = formatIssueList(issues, userId);

            // Build the supervisor request with full context
            String supervisorRequest = String.format("""
                Analyze and rebalance this developer's workload to reduce stress.
                
                Current State:
                - Stress Score: %d/100 (%s)
                - Total Assigned: %d issues
                - Deep Work: %d (need exactly 1)
                - Quick Wins: %d (max 3)
                - Maintenance: %d (max 3)
                - 3-3-3 Compliant: %s
                - Chaos Score: %.1f/10
                - After Hours Activity: %s
                - Mystery Meat Issues: %d
                
                Available Issues:
                %s
                
                Goals:
                1. Reduce stress score below 50
                2. Achieve 3-3-3 compliance (1 deep work, 3 quick wins, 3 maintenance)
                3. Protect the developer's focus time
                4. Flag unclear issues for scope clarification
                5. Recommend wellness actions if stress is high
                
                Use the available agents to accomplish these goals.
                """,
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
            
            // Supervisor autonomously plans and executes via sub-agents
            String explanation = supervisor.invoke(supervisorRequest);
            
            // Get the mutation plan from the tool (accumulated from all sub-agent calls)
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
            .filter(i -> "open".equalsIgnoreCase(i.state()) || "OPEN".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equalsIgnoreCase(userId)))
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
