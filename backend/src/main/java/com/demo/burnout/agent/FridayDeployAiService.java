package com.demo.burnout.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI Service for Friday deploy confidence analysis.
 * 
 * Part of Phase 5: Friday Deploy Confidence.
 * Provides human-readable assessment of deploy readiness.
 */
public interface FridayDeployAiService {

    @SystemMessage("""
        You are a calm, experienced release engineer providing Friday deploy assessments.
        Your role is to give developers confidence or appropriate caution about deploying.
        
        Assessment criteria:
        1. All critical issues must be resolved
        2. No stale urgent items (>24h without progress)
        3. Deep work for the week should be complete
        4. Chaos score should be low (<5)
        5. No after-hours signals in last 48h
        
        Response style:
        - Be direct but not alarming
        - If score is high (>80): Give confidence, mention what's working
        - If score is medium (50-80): Suggest specific improvements
        - If score is low (<50): Recommend deferring, explain why
        
        Format: Assessment emoji (🟢/🟡/🔴) + 2-3 sentences.
        """)
    @UserMessage("""
        Assess Friday deploy readiness:
        
        Friday Score: {{fridayScore}}/100
        
        Checklist:
        - Critical Issues Open: {{criticalOpen}}
        - Stale Urgents (>24h): {{staleUrgents}}
        - Deep Work Complete: {{deepWorkDone}}
        - Chaos Score: {{chaosScore}}/10
        - After-Hours Last 48h: {{hasAfterHours}}
        - Compliance Score: {{complianceScore}}/100
        
        Current stress level: {{stressLevel}}
        
        Provide your deploy recommendation.
        """)
    String assessDeployReadiness(
        @V("fridayScore") int fridayScore,
        @V("criticalOpen") int criticalOpen,
        @V("staleUrgents") int staleUrgents,
        @V("deepWorkDone") boolean deepWorkDone,
        @V("chaosScore") double chaosScore,
        @V("hasAfterHours") boolean hasAfterHours,
        @V("complianceScore") int complianceScore,
        @V("stressLevel") String stressLevel
    );
}
