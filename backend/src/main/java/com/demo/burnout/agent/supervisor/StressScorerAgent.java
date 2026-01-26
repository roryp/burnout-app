package com.demo.burnout.agent.supervisor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Stress Scorer Agent - Analyzes developer workload and calculates stress score.
 * This is a specialized AI service that the supervisor can invoke to assess current stress levels.
 */
public interface StressScorerAgent {

    @SystemMessage("""
        You are a developer stress assessment expert.
        Analyze the workload metrics and calculate a stress score from 0-100.
        
        Stress indicators:
        - High total assigned issues (>7 is concerning, >10 is critical)
        - Missing 3-3-3 compliance (should have 1 deep work, 3 quick wins, 3 maintenance max)
        - High chaos score (>5 is concerning, >8 is critical)
        - After-hours activity indicates overtime
        - High context switching (>5 issues touched today is concerning)
        - Stale urgent issues indicate blocked work
        
        Scoring guidelines:
        - 0-30: Low stress, healthy workload
        - 31-50: Moderate stress, manageable
        - 51-70: Elevated stress, intervention recommended
        - 71-100: Critical stress, immediate action required
        """)
    @UserMessage("""
        Analyze this developer's workload and provide a stress assessment:
        
        Assigned Issues: {{totalAssigned}}
        Deep Work Items: {{deepWorkCount}} (should be exactly 1)
        Quick Win Items: {{quickWinCount}} (should be max 3)
        Maintenance Items: {{maintenanceCount}} (should be max 3)
        Is 3-3-3 Compliant: {{is333Compliant}}
        Chaos Score: {{chaosScore}} (0-10 scale)
        Issues Touched Today: {{issuesTouchedToday}}
        After Hours Activity: {{hasAfterHours}}
        Stale Issues: {{staleCount}}
        Mystery Meat (unclear scope): {{mysteryMeatCount}}
        
        Respond with a JSON object containing:
        - stressScore: number 0-100
        - stressLevel: "LOW", "MODERATE", "ELEVATED", or "CRITICAL"
        - topConcerns: array of string concerns
        - recommendation: brief recommendation string
        """)
    String assessStress(
        @V("totalAssigned") int totalAssigned,
        @V("deepWorkCount") int deepWorkCount,
        @V("quickWinCount") int quickWinCount,
        @V("maintenanceCount") int maintenanceCount,
        @V("is333Compliant") boolean is333Compliant,
        @V("chaosScore") double chaosScore,
        @V("issuesTouchedToday") int issuesTouchedToday,
        @V("hasAfterHours") boolean hasAfterHours,
        @V("staleCount") int staleCount,
        @V("mysteryMeatCount") int mysteryMeatCount
    );
}
