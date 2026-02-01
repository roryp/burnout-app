package com.demo.burnout.agent.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Workload Rebalancer Agent - Uses tools to reorganize developer's workload.
 * This AI service has access to burnout prevention tools and can invoke them to reduce stress.
 * 
 * @deprecated Use {@link BurnoutAgents} sub-agents with AgenticServices.supervisorBuilder() instead.
 */
@Deprecated
public interface WorkloadRebalancerAgent {

    @SystemMessage("""
        You are a workload optimization expert focused on preventing developer burnout.
        You have access to tools that can modify GitHub issues to reduce stress.
        
        Your goal: Reduce the developer's stress score to below 50 using the available tools.
        
        Strategy:
        1. If stress is CRITICAL (>70): Immediately defer or delegate non-essential issues
        2. If not 3-3-3 compliant: Reclassify issues to achieve 1 deep work, 3 quick wins, 3 maintenance
        3. If too many assigned (>7): Delegate or defer until under 7
        4. If no deep work focus: Mark the most critical/architectural issue as deep work
        5. If mystery meat issues exist: Flag them as needing scope
        6. If after-hours detected: Suggest a break
        7. If high context switching: Recommend calendar blocking
        
        Use the tools wisely - each action has a cost. Aim for maximum stress reduction with minimum changes.
        Maximum 5 tool calls per session.
        
        After using tools, provide a summary of what you did and why.
        """)
    @UserMessage("""
        Rebalance this developer's workload to reduce stress.
        
        Current State:
        - Stress Score: {{stressScore}}/100 ({{stressLevel}})
        - Total Assigned: {{totalAssigned}} issues
        - Deep Work: {{deepWorkCount}} (need exactly 1)
        - Quick Wins: {{quickWinCount}} (max 3)  
        - Maintenance: {{maintenanceCount}} (max 3)
        - 3-3-3 Compliant: {{is333Compliant}}
        - Chaos Score: {{chaosScore}}/10
        - After Hours Activity: {{hasAfterHours}}
        - Mystery Meat Issues: {{mysteryMeatCount}}
        
        Available Issues:
        {{issueList}}
        
        Use the available tools to:
        1. Reduce stress score below 50
        2. Achieve 3-3-3 compliance
        3. Protect the developer's focus time
        
        Explain each action you take and why.
        """)
    String rebalanceWorkload(
        @V("stressScore") int stressScore,
        @V("stressLevel") String stressLevel,
        @V("totalAssigned") int totalAssigned,
        @V("deepWorkCount") int deepWorkCount,
        @V("quickWinCount") int quickWinCount,
        @V("maintenanceCount") int maintenanceCount,
        @V("is333Compliant") boolean is333Compliant,
        @V("chaosScore") double chaosScore,
        @V("hasAfterHours") boolean hasAfterHours,
        @V("mysteryMeatCount") int mysteryMeatCount,
        @V("issueList") String issueList
    );
}
