package com.demo.burnout.service;

import org.springframework.stereotype.Service;

/**
 * Mock calendar fragmentation service for Phase 1 demo.
 * Returns deterministic fragmentation metrics.
 */
@Service
public class CalendarService {

    /**
     * Get mock calendar fragmentation metrics.
     * In production, this would integrate with Google Calendar / Outlook.
     */
    public CalendarFragmentation getFragmentation() {
        // Deterministic mock data for demo
        return new CalendarFragmentation(
            3,    // meetings today
            45,   // total meeting minutes
            120,  // largest free block (minutes)
            2,    // context switches (between different meeting types)
            0.65  // fragmentation score (0 = contiguous, 1 = highly fragmented)
        );
    }

    /**
     * Calendar fragmentation metrics for burnout detection.
     * 
     * @param meetingsToday Number of meetings scheduled today
     * @param totalMeetingMinutes Sum of all meeting durations
     * @param largestFreeBlock Longest uninterrupted work block (minutes)
     * @param contextSwitches Number of transitions between meeting types
     * @param fragmentationScore 0.0-1.0 score (higher = more fragmented = worse)
     */
    public record CalendarFragmentation(
        int meetingsToday,
        int totalMeetingMinutes,
        int largestFreeBlock,
        int contextSwitches,
        double fragmentationScore
    ) {
        public static final int SCHEMA_VERSION = 1;
        
        public int schemaVersion() { 
            return SCHEMA_VERSION; 
        }
        
        public boolean isDeepWorkPossible() {
            return largestFreeBlock >= 90; // Need at least 90 min for deep work
        }
    }
}
