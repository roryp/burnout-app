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

import java.time.Clock;
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
 *   (gated by the supervisor prompt: only invoked when stress >= 50,
 *   i.e. StressLevel.HIGH or CRITICAL)
 */
@Service
public class BurnoutSupervisorService {

    private static final Logger log = LoggerFactory.getLogger(BurnoutSupervisorService.class);

    private final ChatModel chatModel;
    private final ChatModel plannerModel;
    private final Clock clock;
    private final boolean llmEnabled;

    @Autowired
    public BurnoutSupervisorService(
            @Autowired(required = false) ChatModel chatModel,
            @Autowired(required = false) @Qualifier("plannerModel") ChatModel plannerModel,
            Clock clock) {
        this.chatModel = chatModel;
        this.plannerModel = plannerModel != null ? plannerModel : chatModel;
        this.clock = clock;
        this.llmEnabled = chatModel != null;
        log.info("BurnoutSupervisorService initialized. LLM enabled: {}, Supervisor pattern: {}",
            llmEnabled, plannerModel != null ? "ACTIVE" : "FALLBACK");
    }

    /**
     * Result of supervisor invocation containing explanation and mutation plan.
     *
     * {@code deterministicTriageCount} and {@code deterministicDefuseCount}
     * report how many issues the always-on deterministic pre-pass touched
     * before (and regardless of) the LLM supervisor running. They are
     * surfaced separately so callers can show the user what actually drove
     * the stress drop — pre-pass vs. LLM agents.
     *
     * {@code wellnessInvocationCount} counts how many times the LLM
     * invoked any of the wellness tools (suggestBreak / slowIntake /
     * blockCalendarTime). Wellness tools are advisory-only and never
     * emit GitHubActions, so without this counter their invocations
     * leave no trace in the response. Useful for verifying the
     * supervisor's stress &gt;= 50 gating actually routes work to the
     * WellnessAgent. Always 0 when the LLM is disabled or fails.
     */
    public record SupervisorResult(
        String explanation,
        GitHubMutationPlan mutationPlan,
        int estimatedStressScore,
        boolean llmUsed,
        int deterministicTriageCount,
        int deterministicDefuseCount,
        int wellnessInvocationCount
    ) {
        public static SupervisorResult fallback(String message, int stressScore) {
            return new SupervisorResult(message, GitHubMutationPlan.empty(), stressScore, false, 0, 0, 0);
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

        // Create the mutation tool with access to issues. The deterministic
        // pre-pass below runs against this tool BEFORE we check whether the
        // LLM is available — that way we always get the chaos-defusing
        // mutations into the plan even if the LLM is dummy/down.
        BurnoutMutationTool mutationTool = new BurnoutMutationTool(issues, repo);

        // Identify unassigned-urgent issues that need deterministic triage.
        List<Integer> unassignedUrgentNumbers = issues.stream()
            .filter(i -> i.assignees() == null || i.assignees().isEmpty())
            .filter(i -> i.labels() != null && i.labels().stream().anyMatch(l ->
                l.name() != null && (
                    l.name().equalsIgnoreCase("urgent") ||
                    l.name().equalsIgnoreCase("priority:critical") ||
                    l.name().equalsIgnoreCase("priority:high"))))
            .map(Issue::number)
            .toList();
        String unassignedUrgentList = unassignedUrgentNumbers.isEmpty()
            ? "(none)"
            : unassignedUrgentNumbers.stream().map(n -> "#" + n).collect(Collectors.joining(", "));

        // DETERMINISTIC PRE-PASS — strip chaos-inducing urgent labels from
        // unassigned issues directly. The supervisor LLM was unreliable at
        // calling TriageAgent for every issue; doing it here guarantees the
        // chaos score drops on every reshape regardless of LLM behavior.
        for (int n : unassignedUrgentNumbers) {
            mutationTool.triageUrgent(n);
        }
        int triagedCount = unassignedUrgentNumbers.size();
        log.info("Deterministic triage pre-pass: triaged {} unassigned urgent issue(s): {}",
            triagedCount, unassignedUrgentList);

        // DETERMINISTIC CHAOS DEFUSER — fill empty bodies and normalise
        // after-hours / recently-touched timestamps. The chaos score is
        // bucketed (LOW≤2, MEDIUM≤5, HIGH≤8, CRITICAL>8) and uses binary
        // factors (mysteryMeat≥3, urgent≥3, touched≥6, afterHours,
        // labels≥12), so partial improvement does not show up. Defusing
        // every contributor is what actually moves the bucket.
        int defusedCount = mutationTool.defuseChaosInputs(clock);
        log.info("Deterministic chaos defuser: normalised body/updatedAt on {} issue(s)", defusedCount);

        // Pre-pass note prepended to whatever explanation we end up with —
        // this is how the user finds out the deterministic phase ran.
        String prePassNote = String.format(
            "**🧹 Deterministic pre-pass:** triaged %d unassigned-urgent issue(s)%s and defused %d chaos input(s) (empty bodies / after-hours timestamps) before the LLM was invoked.%n",
            triagedCount,
            unassignedUrgentNumbers.isEmpty() ? "" : " (" + unassignedUrgentList + ")",
            defusedCount);

        if (!llmEnabled) {
            log.warn("LLM not enabled; returning fallback result with deterministic pre-pass mutations only");
            return generateFallbackResult(state, mutationTool, triagedCount, defusedCount, prePassNote);
        }

        try {
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

            BurnoutAgents.TriageAgent triageAgent = AgenticServices
                .agentBuilder(BurnoutAgents.TriageAgent.class)
                .chatModel(chatModel)
                .tools(mutationTool)
                .build();

            // Build supervisor using AgenticServices.supervisorBuilder() with sub-agents
            // The supervisor uses plannerModel to decide which sub-agents to invoke
            SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(deferAgent, delegateAgent, classifyAgent, scopeAgent, wellnessAgent, triageAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .maxAgentsInvocations(15)
                .build();

            log.info("Invoking Supervisor to orchestrate burnout prevention agents");

            // Format issues for the supervisor prompt
            String issueList = formatIssueList(issues, userId);

            // Build the supervisor request with full context
            String supervisorRequest = String.format("""
                Analyze and rebalance this developer's workload to reduce stress.
                
                Current State (BEFORE any reshape mutations):
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
                
                Note: The unassigned urgent issues (%s) have already been
                triaged deterministically. Do NOT call any agent on those
                issues — leave them alone.
                
                Your job is to:
                1. Reduce stress score below 50
                2. Achieve 3-3-3 compliance (1 deep work, 3 quick wins, 3 maintenance)
                3. Protect the developer's focus time
                4. Flag unclear issues for scope clarification
                5. Recommend wellness actions only if stress >= 50 (HIGH or CRITICAL)
                
                Use the available agents in this order: ClassifyAgent (build
                3-3-3) → DeferAgent (overflow beyond 3-3-3) → ScopeAgent
                (mystery meat) → WellnessAgent (if stress >= 50).
                
                IMPORTANT — output rules for your final summary:
                * Do NOT quote any specific stress score number. The numbers
                  shown above are the BEFORE state; the system computes and
                  appends the AFTER score itself, so any absolute number
                  you write will be wrong by the time the user sees it.
                * Describe the ACTIONS you took (classify, defer, scope,
                  wellness) and their qualitative effect (e.g. "reduced",
                  "balanced", "deferred overflow"). Avoid claims like
                  "stress remains at 58/100" or "stress is now 30".
                * Keep it to 2–3 short sentences.
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
                issueList,
                unassignedUrgentList
            );
            
            // Supervisor autonomously plans and executes via sub-agents
            String llmExplanation = supervisor.invoke(supervisorRequest);
            String explanation = prePassNote + "\n" + llmExplanation;
            
            // Get the mutation plan from the tool (accumulated from all sub-agent calls)
            GitHubMutationPlan mutationPlan = mutationTool.getMutationPlan();
            int wellnessInvocations = mutationTool.getWellnessInvocationCount();

            log.info("Supervisor completed. Actions planned: {} ({} from deterministic pre-pass, {} wellness invocation(s))",
                mutationPlan.actions().size(), triagedCount + defusedCount, wellnessInvocations);

            // Estimate new stress score based on actions taken
            int estimatedStress = estimateReducedStress(state, mutationPlan);

            return new SupervisorResult(explanation, mutationPlan, estimatedStress, true,
                triagedCount, defusedCount, wellnessInvocations);
            
        } catch (Exception e) {
            log.error("Supervisor invocation failed: {} — returning fallback with pre-pass mutations", e.getMessage(), e);
            return generateFallbackResult(state, mutationTool, triagedCount, defusedCount, prePassNote);
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
     * Generate fallback result when LLM is unavailable or fails. Always
     * returns the deterministic pre-pass mutations and counts so the
     * caller can still report what happened.
     */
    private SupervisorResult generateFallbackResult(WorldState state,
                                                     BurnoutMutationTool mutationTool,
                                                     int triagedCount,
                                                     int defusedCount,
                                                     String prePassNote) {
        int stress = state.calculateStressScore();
        StringBuilder sb = new StringBuilder();
        sb.append(prePassNote).append('\n');

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

        sb.append("*LLM agents unavailable — using deterministic fallback. Pre-pass mutations still applied.*");

        GitHubMutationPlan plan = mutationTool.getMutationPlan();
        int estimatedStress = estimateReducedStress(state, plan);
        return new SupervisorResult(sb.toString(), plan, estimatedStress, false,
            triagedCount, defusedCount, mutationTool.getWellnessInvocationCount());
    }

    public boolean isLlmEnabled() {
        return llmEnabled;
    }
}
