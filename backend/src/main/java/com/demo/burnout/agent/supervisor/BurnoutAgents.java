package com.demo.burnout.agent.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sub-agents for the SUPERVISOR PATTERN using langchain4j-agentic module.
 * Demonstrates autonomous agent orchestration with tool integration for burnout prevention.
 * 
 * Pattern: Supervisor autonomously coordinates DeferAgent, DelegateAgent, ClassifyAgent.
 * Uses AgenticServices.supervisorBuilder() for LLM-based planning and coordination.
 */
public interface BurnoutAgents {

    /**
     * DeferAgent: Handles deferring issues to reduce immediate workload.
     * Uses BurnoutMutationTool for GitHub operations.
     */
    interface DeferAgent {
        @SystemMessage("""
            You are a workload protection agent that defers non-critical issues to reduce immediate stress.
            Use the deferIssue tool to mark issues for next sprint when the developer is overloaded.
            Only defer issues that are not marked as priority:critical or urgent.
            """)
        @UserMessage("""
            Analyze the following issues and defer appropriate ones to reduce stress.
            Current stress score: {{stressScore}}/100
            Target: Reduce assigned count below 7 issues.
            
            Issues to consider:
            {{issueList}}
            
            Use the tools to defer issues, then summarize what you deferred and why.
            """)
        @Agent(description = "A protective agent that defers non-critical issues to next sprint")
        String deferIssues(@V("stressScore") int stressScore, @V("issueList") String issueList);
    }

    /**
     * DelegateAgent: Handles delegating issues to redistribute workload.
     * Uses BurnoutMutationTool for GitHub operations.
     */
    interface DelegateAgent {
        @SystemMessage("""
            You are a workload distribution agent that delegates issues to balance team load.
            Use the delegateIssue tool to mark issues that should be reassigned to other team members.
            Focus on issues without clear ownership or that could benefit from fresh perspective.
            """)
        @UserMessage("""
            Analyze the following issues and delegate appropriate ones to balance workload.
            Current assigned count: {{totalAssigned}} issues (max recommended: 7)
            
            Issues to consider:
            {{issueList}}
            
            Use the tools to mark issues for delegation, then summarize what you delegated and why.
            """)
        @Agent(description = "A distribution agent that delegates issues to balance team workload")
        String delegateIssues(@V("totalAssigned") int totalAssigned, @V("issueList") String issueList);
    }

    /**
     * ClassifyAgent: Handles reclassifying issues for 3-3-3 compliance.
     * Uses BurnoutMutationTool for GitHub operations.
     */
    interface ClassifyAgent {
        @SystemMessage("""
            You are a workload organization agent that classifies issues according to the 3-3-3 structure:
            - 1 deep work item (complex, architectural, requires focus)
            - 3 quick wins (small tasks under 30 minutes)
            - 3 maintenance tasks (tech debt, docs, cleanup)
            
            Use the classification tools:
            - markAsDeepWork for the most critical/architectural issue
            - classifyAsQuickWin for small, quick tasks
            - classifyAsMaintenance for tech debt and cleanup
            """)
        @UserMessage("""
            Organize the following issues to achieve 3-3-3 compliance.
            Current state:
            - Deep work: {{deepWorkCount}} (need exactly 1)
            - Quick wins: {{quickWinCount}} (max 3)
            - Maintenance: {{maintenanceCount}} (max 3)
            
            Issues to classify:
            {{issueList}}
            
            Use the tools to classify issues, then summarize the new structure.
            """)
        @Agent(description = "An organization agent that classifies issues for 3-3-3 structure")
        String classifyIssues(
            @V("deepWorkCount") int deepWorkCount,
            @V("quickWinCount") int quickWinCount,
            @V("maintenanceCount") int maintenanceCount,
            @V("issueList") String issueList
        );
    }

    /**
     * ScopeAgent: Handles flagging unclear issues that need better definition.
     * Uses BurnoutMutationTool for GitHub operations.
     */
    interface ScopeAgent {
        @SystemMessage("""
            You are a clarity agent that identifies issues lacking clear scope or definition.
            Use the addScopeNeeded tool to flag issues that:
            - Have no description or vague descriptions
            - Don't have clear "done" criteria
            - Are ambiguous about requirements
            
            Mystery meat issues create cognitive load and should be clarified before work begins.
            """)
        @UserMessage("""
            Review the following issues and flag those needing scope clarification.
            Current mystery meat count: {{mysteryMeatCount}}
            
            Issues to review:
            {{issueList}}
            
            Use the tools to flag unclear issues, then summarize what needs clarification.
            """)
        @Agent(description = "A clarity agent that flags issues needing scope definition")
        String reviewScope(@V("mysteryMeatCount") int mysteryMeatCount, @V("issueList") String issueList);
    }

    /**
     * WellnessAgent: Handles wellness recommendations when stress is high.
     * Uses BurnoutMutationTool for recommendations.
     */
    interface WellnessAgent {
        @SystemMessage("""
            You are a wellness agent focused on developer health and sustainable productivity.
            When stress indicators are high, recommend:
            - Taking breaks (suggestBreak tool)
            - Slowing issue intake (slowIntake tool)
            - Blocking calendar time for focus (blockCalendarTime tool)
            
            Be supportive and emphasize that rest improves long-term productivity.
            """)
        @UserMessage("""
            Assess wellness needs based on current stress indicators:
            - Stress score: {{stressScore}}/100
            - After hours activity: {{hasAfterHours}}
            - Chaos score: {{chaosScore}}/10
            
            Provide wellness recommendations as needed.
            """)
        @Agent(description = "A wellness agent that provides stress reduction recommendations")
        String assessWellness(
            @V("stressScore") int stressScore,
            @V("hasAfterHours") boolean hasAfterHours,
            @V("chaosScore") double chaosScore
        );
    }

    /**
     * BurnoutSupervisor: Typed interface for the supervisor pattern.
     * The supervisor autonomously plans and coordinates sub-agents.
     */
    interface BurnoutSupervisor {
        @Agent
        String invoke(@V("request") String request);
    }
}
