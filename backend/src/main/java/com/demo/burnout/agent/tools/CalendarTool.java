package com.demo.burnout.agent.tools;

import com.demo.burnout.service.CalendarService;
import com.demo.burnout.service.CalendarService.CalendarFragmentation;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * LangChain4j Tool for reading calendar fragmentation data.
 * 
 * Provides agents with calendar-based burnout signals.
 */
@Component
public class CalendarTool {

    private final CalendarService calendarService;

    public CalendarTool(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Tool("Get calendar fragmentation analysis to assess focus time availability")
    public String getCalendarFragmentation() {
        CalendarFragmentation frag = calendarService.getFragmentation();
        
        return String.format("""
            Calendar Analysis:
            - Meetings Today: %d
            - Total Meeting Time: %d minutes
            - Largest Free Block: %d minutes
            - Context Switches: %d
            - Fragmentation Score: %.0f%% (%s)
            - Deep Work Possible: %s
            
            %s
            """,
            frag.meetingsToday(),
            frag.totalMeetingMinutes(),
            frag.largestFreeBlock(),
            frag.contextSwitches(),
            frag.fragmentationScore() * 100,
            frag.fragmentationScore() > 0.7 ? "HIGHLY FRAGMENTED" : 
                frag.fragmentationScore() > 0.4 ? "MODERATELY FRAGMENTED" : "GOOD",
            frag.isDeepWorkPossible() ? "Yes ✓" : "No ⚠️ (need 90+ min block)",
            frag.isDeepWorkPossible() 
                ? "You have time for focused deep work." 
                : "Consider declining optional meetings or blocking time."
        );
    }
}
