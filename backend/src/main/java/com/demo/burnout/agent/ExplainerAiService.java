package com.demo.burnout.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI Service for explaining GOAP action plans in human-friendly terms.
 * 
 * This agent ONLY explains deterministic results - it does NOT make decisions.
 * All planning is done by GOAPPlanner; this agent provides natural language narration.
 */
public interface ExplainerAiService {

    @SystemMessage("""
        You are a supportive productivity coach integrated into a burnout prevention system.
        Your role is to explain action plans in a warm, encouraging, and clear manner.
        
        Key principles:
        1. Be concise but empathetic - developers are busy and stressed
        2. Explain the "why" behind each recommendation
        3. Use the 3-3-3 framework: 1 deep work, 3 quick wins, 3 maintenance items
        4. Acknowledge stress signals without being alarmist
        5. End with an encouraging note about the benefits of the plan
        
        Format your response with:
        - A brief stress assessment (1-2 sentences)
        - Summary of key actions (bullet points)
        - Expected outcome (1 sentence)
        
        Never suggest actions beyond what's in the plan - the plan is already optimized.
        """)
    @UserMessage("""
        Explain this burnout prevention plan to a developer:
        
        Current State:
        - Stress Score: {{stressScore}}/100 ({{stressLevel}})
        - Deep Work Items: {{deepWorkCount}}
        - Quick Wins: {{quickWinCount}}
        - Maintenance Tasks: {{maintenanceCount}}
        - Total Assigned: {{totalAssigned}} issues
        - Is 3-3-3 Compliant: {{is333Compliant}}
        
        Primary Goal: {{primaryGoal}}
        
        Planned Actions ({{actionCount}} total):
        {{actionsList}}
        
        Expected Stress After: {{expectedStress}}/100
        """)
    String explainPlan(
        @V("stressScore") int stressScore,
        @V("stressLevel") String stressLevel,
        @V("deepWorkCount") int deepWorkCount,
        @V("quickWinCount") int quickWinCount,
        @V("maintenanceCount") int maintenanceCount,
        @V("totalAssigned") int totalAssigned,
        @V("is333Compliant") boolean is333Compliant,
        @V("primaryGoal") String primaryGoal,
        @V("actionCount") int actionCount,
        @V("actionsList") String actionsList,
        @V("expectedStress") int expectedStress
    );
}
