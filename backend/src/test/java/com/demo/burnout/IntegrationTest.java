package com.demo.burnout;

import com.demo.burnout.controller.ChaosController;
import com.demo.burnout.controller.IssueSyncController;
import com.demo.burnout.controller.ReshapeController;
import com.demo.burnout.model.Issue;
import com.demo.burnout.service.IssueCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IssueCache issueCache;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void healthEndpointWorks() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void chaosEndpointReturnsNotSyncedWhenEmpty() {
        ResponseEntity<ChaosController.ChaosResponse> response = restTemplate.getForEntity(
            baseUrl + "/api/chaos?repo=test/repo", ChaosController.ChaosResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("not_synced", response.getBody().status());
    }

    @Test
    void syncAndChaosFlow() {
        String repo = "test/sync-test";
        
        // Create test issues
        List<Issue> issues = List.of(
            new Issue(1, "Urgent bug", "", 
                List.of(new Issue.Label("urgent")), 
                List.of(new Issue.Assignee("testuser")),
                Instant.now().minusSeconds(100000), Instant.now(), "open", null),
            new Issue(2, "Quick fix", "- [ ] Do the thing",
                List.of(new Issue.Label("quick-win")),
                List.of(new Issue.Assignee("testuser")),
                Instant.now(), Instant.now(), "open", null)
        );
        
        // Sync issues
        IssueSyncController.IssueSyncRequest syncReq = 
            new IssueSyncController.IssueSyncRequest(repo, issues, Instant.now(), 1);
        
        ResponseEntity<IssueSyncController.SyncAck> syncResponse = restTemplate.postForEntity(
            baseUrl + "/api/issues/sync", syncReq, IssueSyncController.SyncAck.class);
        
        assertEquals(HttpStatus.OK, syncResponse.getStatusCode());
        assertEquals(2, syncResponse.getBody().receivedCount());
        
        // Get chaos score
        ResponseEntity<ChaosController.ChaosResponse> chaosResponse = restTemplate.getForEntity(
            baseUrl + "/api/chaos?repo=" + repo, ChaosController.ChaosResponse.class);
        
        assertEquals(HttpStatus.OK, chaosResponse.getStatusCode());
        assertEquals("ok", chaosResponse.getBody().status());
        assertTrue(chaosResponse.getBody().score() >= 0);
    }

    @Test
    void reshapeEndpointWorks() {
        String repo = "test/reshape-test";
        String userId = "testuser";
        
        // Seed with issues
        List<Issue> issues = List.of(
            new Issue(1, "Critical task", "Deep work item",
                List.of(new Issue.Label("priority:critical"), new Issue.Label("architecture")),
                List.of(new Issue.Assignee(userId)),
                Instant.now(), Instant.now(), "open", null),
            new Issue(2, "Quick task", "- [ ] Simple fix\nDone when: tests pass",
                List.of(new Issue.Label("quick-win"), new Issue.Label("size:S")),
                List.of(new Issue.Assignee(userId)),
                Instant.now(), Instant.now(), "open", null),
            new Issue(3, "Docs update", "Update README",
                List.of(new Issue.Label("documentation"), new Issue.Label("maintenance")),
                List.of(new Issue.Assignee(userId)),
                Instant.now(), Instant.now(), "open", null)
        );
        
        issueCache.put(repo, issues, Instant.now());
        
        // Call reshape
        ReshapeController.ReshapeRequest req = new ReshapeController.ReshapeRequest(repo, userId, true);
        ResponseEntity<ReshapeController.ReshapeResponse> response = restTemplate.postForEntity(
            baseUrl + "/api/reshape", req, ReshapeController.ReshapeResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ok", response.getBody().status());
        assertNotNull(response.getBody().dayPlan());
        assertTrue(response.getBody().fridayScore() >= 0);
    }

    @Test
    void fridayScoreEndpointWorks() {
        String repo = "test/friday-test";
        
        // Seed with low-chaos issues
        List<Issue> issues = List.of(
            new Issue(1, "Simple task", "Clear scope defined",
                List.of(new Issue.Label("quick-win")),
                List.of(new Issue.Assignee("testuser")),
                Instant.now(), Instant.now(), "open", null)
        );
        
        issueCache.put(repo, issues, Instant.now());
        
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/api/friday-score?repo=" + repo, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("score"));
    }
}
