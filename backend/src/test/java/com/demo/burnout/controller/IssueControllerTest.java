package com.demo.burnout.controller;

import com.demo.burnout.dto.ChaosResponse;
import com.demo.burnout.dto.SyncRequest;
import com.demo.burnout.dto.SyncResponse;
import com.demo.burnout.model.ChaosMetrics;
import com.demo.burnout.model.Issue;
import com.demo.burnout.service.ChaosMetricsService;
import com.demo.burnout.service.IssueCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IssueController.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueCache issueCache;

    @MockitoBean
    private ChaosMetricsService chaosMetricsService;

    private List<Issue> sampleIssues;

    @BeforeEach
    void setUp() {
        sampleIssues = List.of(
                new Issue(
                        1,
                        "Bug in login",
                        "Users cannot log in with OAuth",
                        List.of(new Issue.Label("bug")),
                        List.of(new Issue.Assignee("developer1")),
                        Instant.parse("2024-01-01T10:00:00Z"),
                        Instant.parse("2024-01-02T10:00:00Z"),
                        "open",
                        null
                ),
                new Issue(
                        2,
                        "Feature request",
                        "Add dark mode support",
                        List.of(new Issue.Label("enhancement")),
                        List.of(new Issue.Assignee("developer2")),
                        Instant.parse("2024-01-03T10:00:00Z"),
                        Instant.parse("2024-01-04T10:00:00Z"),
                        "closed",
                        new Issue.Milestone("v1.0", Instant.parse("2024-02-01T00:00:00Z"))
                )
        );
    }

    @Test
    void sync_shouldStoreIssuesInCacheAndReturnSuccessResponse() throws Exception {
        // Given
        SyncRequest request = new SyncRequest("owner/repo", sampleIssues);
        doNothing().when(issueCache).put(anyString(), anyList(), any(Instant.class));

        // When & Then
        mockMvc.perform(post("/api/issues/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.count").value(2));

        verify(issueCache).put(eq("owner/repo"), eq(sampleIssues), any(Instant.class));
    }

    @Test
    void sync_shouldHandleEmptyIssuesList() throws Exception {
        // Given
        SyncRequest request = new SyncRequest("owner/repo", List.of());
        doNothing().when(issueCache).put(anyString(), anyList(), any(Instant.class));

        // When & Then
        mockMvc.perform(post("/api/issues/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.count").value(0));

        verify(issueCache).put(eq("owner/repo"), eq(List.of()), any(Instant.class));
    }

    @Test
    void chaos_shouldReturnMetricsWhenRepoExists() throws Exception {
        // Given
        String repo = "owner/repo";
        ChaosMetrics metrics = new ChaosMetrics(5, 3, 8, true, 2, 75.5);

        when(issueCache.hasRepo(repo)).thenReturn(true);
        when(issueCache.get(repo)).thenReturn(sampleIssues);
        when(chaosMetricsService.calculate(sampleIssues)).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/chaos")
                        .param("repo", repo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.issuesTouchedRecently").value(5))
                .andExpect(jsonPath("$.unresolvedUrgent").value(3))
                .andExpect(jsonPath("$.distinctLabelCount").value(8))
                .andExpect(jsonPath("$.afterHoursSignal").value(true))
                .andExpect(jsonPath("$.mysteryMeatCount").value(2))
                .andExpect(jsonPath("$.score").value(75.5))
                .andExpect(jsonPath("$.schemaVersion").value(1));

        verify(issueCache).hasRepo(repo);
        verify(issueCache).get(repo);
        verify(chaosMetricsService).calculate(sampleIssues);
    }

    @Test
    void chaos_shouldReturnNotSyncedWhenRepoDoesNotExist() throws Exception {
        // Given
        String repo = "owner/nonexistent";
        when(issueCache.hasRepo(repo)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/chaos")
                        .param("repo", repo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_synced"))
                .andExpect(jsonPath("$.issuesTouchedRecently").value(0))
                .andExpect(jsonPath("$.unresolvedUrgent").value(0))
                .andExpect(jsonPath("$.distinctLabelCount").value(0))
                .andExpect(jsonPath("$.afterHoursSignal").value(false))
                .andExpect(jsonPath("$.mysteryMeatCount").value(0))
                .andExpect(jsonPath("$.score").value(-1))
                .andExpect(jsonPath("$.schemaVersion").value(ChaosMetrics.SCHEMA_VERSION));

        verify(issueCache).hasRepo(repo);
        verify(issueCache, never()).get(anyString());
        verify(chaosMetricsService, never()).calculate(anyList());
    }

    @Test
    void chaos_shouldReturnZeroScoreWhenMetricsAreZero() throws Exception {
        // Given
        String repo = "owner/repo";
        ChaosMetrics metrics = new ChaosMetrics(0, 0, 0, false, 0, 0.0);

        when(issueCache.hasRepo(repo)).thenReturn(true);
        when(issueCache.get(repo)).thenReturn(List.of());
        when(chaosMetricsService.calculate(List.of())).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/chaos")
                        .param("repo", repo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.issuesTouchedRecently").value(0))
                .andExpect(jsonPath("$.unresolvedUrgent").value(0))
                .andExpect(jsonPath("$.distinctLabelCount").value(0))
                .andExpect(jsonPath("$.afterHoursSignal").value(false))
                .andExpect(jsonPath("$.mysteryMeatCount").value(0))
                .andExpect(jsonPath("$.score").value(0.0))
                .andExpect(jsonPath("$.schemaVersion").value(1));
    }

    @Test
    void chaos_shouldHandleHighChaosScore() throws Exception {
        // Given
        String repo = "owner/chaotic-repo";
        ChaosMetrics metrics = new ChaosMetrics(50, 20, 25, true, 15, 99.9);

        when(issueCache.hasRepo(repo)).thenReturn(true);
        when(issueCache.get(repo)).thenReturn(sampleIssues);
        when(chaosMetricsService.calculate(sampleIssues)).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/chaos")
                        .param("repo", repo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.issuesTouchedRecently").value(50))
                .andExpect(jsonPath("$.unresolvedUrgent").value(20))
                .andExpect(jsonPath("$.distinctLabelCount").value(25))
                .andExpect(jsonPath("$.afterHoursSignal").value(true))
                .andExpect(jsonPath("$.mysteryMeatCount").value(15))
                .andExpect(jsonPath("$.score").value(99.9))
                .andExpect(jsonPath("$.schemaVersion").value(1));
    }
}