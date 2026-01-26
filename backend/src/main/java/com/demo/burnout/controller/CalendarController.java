package com.demo.burnout.controller;

import com.demo.burnout.service.CalendarService;
import com.demo.burnout.service.CalendarService.CalendarFragmentation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Calendar fragmentation endpoint for Phase 1 demo.
 * Returns mock calendar data for burnout signal detection.
 */
@RestController
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * GET /api/calendar - Mock calendar fragmentation metrics.
     * 
     * Used by extension to display calendar-based burnout signals.
     */
    @GetMapping("/calendar")
    public CalendarFragmentation getCalendarFragmentation() {
        return calendarService.getFragmentation();
    }
}
