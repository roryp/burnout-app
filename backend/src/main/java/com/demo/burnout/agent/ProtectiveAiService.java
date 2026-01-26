package com.demo.burnout.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI Service for detecting emotional signals and providing protective responses.
 * 
 * Part of Phase 4: Emotionally Supportive Agent (Plutchik model).
 * Detects stress signals and generates appropriate protective messaging.
 */
public interface ProtectiveAiService {

    @SystemMessage("""
        You are a protective AI companion focused on developer wellbeing.
        Your role is to detect signs of burnout and provide gentle, supportive interventions.
        
        Emotional signals to watch for (Plutchik wheel):
        - Frustration: Rapid context switching, many blocked items
        - Exhaustion: After-hours activity, sustained high chaos
        - Overwhelm: Too many critical items, no clear priorities
        - Anxiety: Approaching deadlines without progress
        
        Response principles:
        1. Validate feelings without being patronizing
        2. Suggest concrete protective actions
        3. Be brief - stressed people don't read walls of text
        4. Never guilt or shame - focus on self-care
        5. Include one actionable suggestion
        
        Format: 1-2 sentences of acknowledgment + 1 protective suggestion.
        """)
    @UserMessage("""
        Analyze this developer's state and provide a protective response:
        
        Signals:
        - Consecutive High-Stress Days: {{consecutiveHighDays}}
        - After-Hours Activity: {{hasAfterHours}}
        - Context Switches Today: {{contextSwitches}}
        - Blocked Items: {{blockedCount}}
        - Issues Touched Recently: {{recentlyTouched}}
        - Current Stress: {{stressScore}}/100
        
        Day: {{dayOfWeek}}, Time: {{timeOfDay}}
        
        If stress signals are high, suggest a protective action.
        If signals are normal, respond with brief encouragement.
        """)
    String generateProtectiveResponse(
        @V("consecutiveHighDays") int consecutiveHighDays,
        @V("hasAfterHours") boolean hasAfterHours,
        @V("contextSwitches") int contextSwitches,
        @V("blockedCount") int blockedCount,
        @V("recentlyTouched") int recentlyTouched,
        @V("stressScore") int stressScore,
        @V("dayOfWeek") String dayOfWeek,
        @V("timeOfDay") String timeOfDay
    );
}
