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
 * 
 * Each sub-agent has @Agent annotation and uses tools from BurnoutMutationTool.
 */
public interface BurnoutAgents {

    /**
     * DeferAgent: Defers non-critical issues to reduce immediate workload.
     * Uses BurnoutMutationTool.deferIssue() for GitHub operations.
     */
    interface DeferAgent {
        @SystemMessage("""
            You are a workload protection agent that defers non-critical issues.
            Use the deferIssue tool to mark issues for next sprint.
            Only defer issues that are NOT priority:critical or urgent.
            """)
        @UserMessage("""
            Defer issue #{{issueNumber}} to reduce stress.
            Issue: {{issueTitle}}
            """)
        @Agent(description = "A protective agent that defers non-critical issues to next sprint")
        String deferIssue(@V("issueNumber") int issueNumber, @V("issueTitle") String issueTitle);
    }

    /**
     * DelegateAgent: Delegates issues to redistribute workload.
     * Uses BurnoutMutationTool.delegateIssue() for GitHub operations.
     */
    interface DelegateAgent {
        @SystemMessage("""
            You are a workload distribution agent that delegates issues.
            Use the delegateIssue tool to mark issues for reassignment.
            """)
        @UserMessage("""
            Delegate issue #{{issueNumber}} to balance workload.
            Issue: {{issueTitle}}
            """)
        @Agent(description = "A distribution agent that delegates issues to balance team workload")
        String delegateIssue(@V("issueNumber") int issueNumber, @V("issueTitle") String issueTitle);
    }

    /**
     * ClassifyAgent: Classifies issues for 3-3-3 compliance.
     * Uses BurnoutMutationTool classification methods.
     */
    interface ClassifyAgent {
        @SystemMessage("""
            You are a workload organization agent that classifies issues for 3-3-3 structure:
            - 1 deep work (markAsDeepWork)
            - 3 quick wins (classifyAsQuickWin) 
            - 3 maintenance (classifyAsMaintenance)
            """)
        @UserMessage("""
            Classify issue #{{issueNumber}} as {{classification}}.
            Issue: {{issueTitle}}
            """)
        @Agent(description = "An organization agent that classifies issues for 3-3-3 structure")
        String classifyIssue(
            @V("issueNumber") int issueNumber, 
            @V("issueTitle") String issueTitle,
            @V("classification") String classification
        );
    }

    /**
     * ScopeAgent: Flags unclear issues needing scope clarification.
     * Uses BurnoutMutationTool.addScopeNeeded() for GitHub operations.
     */
    interface ScopeAgent {
        @SystemMessage("""
            You are a clarity agent that identifies unclear issues.
            Use addScopeNeeded for issues lacking clear scope or "done" criteria.
            """)
        @UserMessage("""
            Flag issue #{{issueNumber}} as needing scope clarification.
            Issue: {{issueTitle}}
            """)
        @Agent(description = "A clarity agent that flags issues needing scope definition")
        String flagForScope(@V("issueNumber") int issueNumber, @V("issueTitle") String issueTitle);
    }

    /**
     * WellnessAgent: Provides stress reduction recommendations.
     * Uses BurnoutMutationTool wellness methods.
     */
    interface WellnessAgent {
        @SystemMessage("""
            You are a wellness agent focused on developer health.
            Use suggestBreak, slowIntake, or blockCalendarTime as needed.
            Be supportive and emphasize sustainable productivity.
            """)
        @UserMessage("""
            Assess wellness needs for stress score {{stressScore}}/100.
            After hours activity: {{hasAfterHours}}
            """)
        @Agent(description = "A wellness agent that provides stress reduction recommendations")
        String assessWellness(@V("stressScore") int stressScore, @V("hasAfterHours") boolean hasAfterHours);
    }
}
