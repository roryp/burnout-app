# Burnout-as-a-Service

A VS Code extension + Java backend that demonstrates **AI-powered burnout prevention** using LangChain4j's agentic patterns, the **3-3-3 day structure**, and Azure OpenAI.

## ✨ Features

- **🤖 LLM-Driven Supervisor** - Uses Azure OpenAI (gpt-4o) with `@Tool` annotations to intelligently rebalance workloads
- **📊 3-3-3 Day Structure** - Automatically classifies issues into Deep Work, Quick Wins, and Maintenance
- **🛡️ Protective AI** - Detects stress signals and provides personalized wellness recommendations
- **📈 Chaos Scoring** - Measures workload chaos (context switching, mystery meat issues, after-hours work)
- **🎡 Visual Wheel** - Beautiful webview showing your daily focus plan

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- GitHub CLI (`gh`) installed and authenticated
- VS Code
- Azure OpenAI endpoint (or modify for OpenAI API)

### 1. Configure Azure OpenAI

Create a `.env` file in the project root:

```env
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_DEPLOYMENT=gpt-4o
```

### 2. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

For demo mode with frozen time:
```bash
mvn spring-boot:run -Dspring.profiles.active=demo
```

### 3. Setup Demo Repository

```bash
# Create labels (run from PowerShell, not bash on Windows)
$env:GITHUB_TOKEN = $null  # Use keyring auth
gh label create "deep-work" -R your-username/your-repo --color "d73a4a" --force
gh label create "quick-win" -R your-username/your-repo --color "0e8a16" --force
gh label create "maintenance" -R your-username/your-repo --color "c5def5" --force

# Create sample issues
gh issue create -R your-username/your-repo -t "Refactor auth module" -b "Deep work task" -l "deep-work" --assignee "@me"
gh issue create -R your-username/your-repo -t "Fix typo" -b "Quick fix" -l "quick-win,size:S" --assignee "@me"
gh issue create -R your-username/your-repo -t "Update docs" -b "Routine" -l "maintenance" --assignee "@me"
```

### 4. Run the Extension

```bash
cd extension
npm install
npm run compile
```

Then press **F5** in VS Code to launch the Extension Development Host.

### 5. Demo Commands

- **Burnout: Show 3-3-3 Wheel** - Display your daily focus plan with AI recommendations
- **Burnout: Preflight Check** - Verify all systems ready
- **Burnout: Reshape My Day (Preview)** - Analyze and preview changes
- **Burnout: Apply Plan** - Execute the AI-planned GitHub mutations
- **Burnout: Toggle Demo Mode** - Stop polling for stage demos

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  VS Code Extension (TypeScript)                                      │
│  - ALL GitHub I/O via gh CLI                                         │
│  - Status bar (chaos score, Friday readiness)                        │
│  - 3-3-3 Wheel webview                                               │
└──────────────────────┬──────────────────────────────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Java Backend (Spring Boot + LangChain4j)                            │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │  BurnoutSupervisorService (LLM-Driven Orchestrator)             ││
│  │  - Analyzes developer stress signals                            ││
│  │  - Plans workload rebalancing via @Tool methods                 ││
│  │  - Generates human-readable explanations                        ││
│  └─────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │  BurnoutMutationTool (@Tool annotations)                        ││
│  │  - deferIssue() - Push to next sprint                           ││
│  │  - delegateIssue() - Reassign to balance load                   ││
│  │  - classifyAsQuickWin() - Mark for quick completion             ││
│  │  - markAsDeepWork() - Flag for focused time                     ││
│  │  - addBlockedLabel() - Signal blockers                          ││
│  │  - ... 9 tools total                                            ││
│  └─────────────────────────────────────────────────────────────────┘│
│  - Issue sync cache                                                  │
│  - Chaos metrics (deterministic scoring)                             │
│  - 3-3-3 compliance analysis                                         │
│  - Azure OpenAI integration                                          │
└─────────────────────────────────────────────────────────────────────┘
```

## LangChain4j Agentic Pattern

This project demonstrates the **Supervisor Pattern** using LangChain4j:

```java
// AI Service with tools
public interface WorkloadRebalancerAgent {
    @SystemMessage("""
        You are a workload optimization agent. Analyze stress signals 
        and use tools to rebalance the developer's workload.
        """)
    String rebalanceWorkload(@UserMessage String context);
}

// Tool class with @Tool annotations
public class BurnoutMutationTool {
    @Tool("Defer an issue to the next sprint to reduce immediate workload")
    public String deferIssue(int issueNumber, String reason) { ... }
    
    @Tool("Delegate an issue to another team member")
    public String delegateIssue(int issueNumber) { ... }
}

// Wiring it together
WorkloadRebalancerAgent agent = AiServices.builder(WorkloadRebalancerAgent.class)
    .chatLanguageModel(azureOpenAiModel)
    .tools(burnoutMutationTool)
    .build();
```

The LLM decides which tools to call based on the stress analysis, creating intelligent workload management.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/issues/sync` | Sync issues from extension |
| GET | `/api/chaos?repo=...` | Get chaos score (0-10) |
| GET | `/api/compliance?repo=...&userId=...` | Get 3-3-3 compliance |
| GET | `/api/stress?repo=...&userId=...` | Get GOAP stress analysis |
| POST | `/api/reshape` | Run full reshape workflow |
| GET | `/api/friday-score?repo=...` | Get Friday deploy readiness |

## The 3-3-3 Day Structure

- **1 Deep Work** - One critical, focused task (2+ hours)
- **3 Quick Wins** - Small, completable items (<30 min each)
- **3 Maintenance** - Routine upkeep tasks

Everything else gets **deferred** to protect your focus.

## AI-Powered Features

### Stress Detection
The system analyzes multiple signals:
- **Context Switching** - Issues touched recently
- **Mystery Meat** - Vague issues without clear scope
- **After Hours** - Late night activity
- **Workload Imbalance** - Too many assigned issues

### Intelligent Actions
When stress is detected, the LLM supervisor can:
1. **Defer** non-critical issues to next sprint
2. **Delegate** tasks to balance team load
3. **Classify** issues into the 3-3-3 structure
4. **Block** issues waiting on dependencies
5. **Protect** deep work time

### Protective Messages
The AI generates personalized wellness recommendations:
> "It looks like you've had a busy morning with a lot of context switching. Take a 5-minute break to reset your focus before diving back in."

## Demo Labels

For stage-safe demos, use these labels to simulate time conditions:

- `demo:stale-14d` - Simulate 14-day staleness
- `demo:after-hours` - Simulate late-night work
- `demo:touched-today` - Simulate recent activity
- `demo:friday` - Force Friday deploy scenario

## Configuration

### Environment Variables (`.env`)

```env
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_DEPLOYMENT=gpt-4o
```

### Backend (`application.yml`)

```yaml
azure:
  openai:
    enabled: true
    endpoint: ${AZURE_OPENAI_ENDPOINT}
    api-key: ${AZURE_OPENAI_API_KEY}
    deployment: ${AZURE_OPENAI_DEPLOYMENT}

demo:
  repo: roryp/burnout-app
  userId: roryp
  friday:
    enabled: false
  clock:
    zone: Africa/Johannesburg
```

### Extension (`src/config.ts`)

```typescript
export const DemoConfig = {
    REPO: 'roryp/burnout-app',
    USER_ID: 'roryp',
    BACKEND_URL: 'http://localhost:8080',
    DEMO_MODE: true,
};
```

## Tech Stack

- **Backend**: Spring Boot 3.5, Java 21, LangChain4j 1.0.0-beta3
- **AI**: Azure OpenAI (gpt-4o) with `@Tool` annotations
- **Extension**: TypeScript, VS Code Extension API
- **GitHub**: gh CLI for all repository operations

## API Response Example

```json
{
  "status": "ok",
  "dayPlan": {
    "deepWork": { "number": 26, "title": "Refactor auth module" },
    "quickWins": [...],
    "maintenance": [...]
  },
  "stressScore": 55,
  "stressLevel": "HIGH",
  "agentExplanation": "I deferred 3 issues and delegated 2 to reduce your workload...",
  "protectiveMessage": "Take a 5-minute break to reset your focus.",
  "llmEnabled": true
}
```

## License

MIT
