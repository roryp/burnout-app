# Burnout-as-a-Service

An **MCP App** + Java backend that demonstrates **AI-powered burnout prevention** using LangChain4j's agentic patterns, the **3-3-3 day structure**, and Azure OpenAI.

## ✨ Features

- **🤖 LLM-Driven Supervisor** - Uses Azure OpenAI (gpt-4o) with `@Tool` annotations to intelligently rebalance workloads
- **📊 3-3-3 Day Structure** - Automatically classifies issues into Deep Work, Quick Wins, and Maintenance
- **🛡️ Protective AI** - Detects stress signals and provides personalized wellness recommendations
- **📈 Chaos Scoring** - Measures workload chaos (context switching, mystery meat issues, after-hours work)
- **🎡 MCP App Visualization** - Interactive burnout wheel that works in any MCP-compliant viewer (VS Code, Claude Desktop, Cursor)

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- GitHub CLI (`gh`) installed and authenticated:
  ```bash
  gh auth login
  gh auth status  # Should show ✓ Logged in with 'repo' scope
  ```
- VS Code with GitHub Copilot
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
java -jar target/burnout-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Or build and run:
```bash
cd backend
mvn clean package -DskipTests
java -jar target/burnout-backend-0.0.1-SNAPSHOT.jar
```

### 3. Build the MCP App

```bash
cd mcp-app
npm install
npm run build
```

### 4. Configure VS Code MCP

The `.vscode/mcp.json` is already configured. Open VS Code in this workspace and the MCP server will be available.

### 5. Use the MCP Tools

In VS Code Copilot Chat, first sync your issues:
- **"Sync issues for owner/repo"** - Fetches from GitHub and syncs to backend

Then explore your workload:
- **"Show my burnout wheel for owner/repo"** - Displays the 3-3-3 visualization
- **"What's my stress score for owner/repo?"** - Quick stress check
- **"Reshape my day for owner/repo"** - AI analyzes and optimizes your workload

> **Note**: Replace `owner/repo` with your actual GitHub repository (e.g., `roryp/my-project`)

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  MCP App (Node.js + TypeScript)                                      │
│  - MCP server with stdio transport                                   │
│  - Tools: show_burnout_wheel, reshape_day, get_stress_score         │
│  - Works in VS Code, Claude Desktop, Cursor, etc.                   │
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
│  │  - ... 9 tools total                                            ││
│  └─────────────────────────────────────────────────────────────────┘│
│  - Issue sync cache                                                  │
│  - Chaos metrics (deterministic scoring)                             │
│  - 3-3-3 compliance analysis                                         │
│  - Azure OpenAI integration                                          │
└─────────────────────────────────────────────────────────────────────┘
```

## MCP Tools

| Tool | Description |
|------|-------------|
| `show_burnout_wheel` | Display your 3-3-3 day plan with metrics |
| `reshape_day` | AI-powered workload optimization |
| `get_stress_score` | Quick stress level check |
| `sync_issues` | Sync issues from GitHub to backend |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/issues/sync` | Sync issues from MCP app |
| GET | `/api/stress?repo=...&userId=...` | Get stress analysis |
| POST | `/api/reshape` | Run full reshape workflow |

## The 3-3-3 Day Structure

- **1 Deep Work** - One critical, focused task (2+ hours)
- **3 Quick Wins** - Small, completable items (<30 min each)
- **3 Maintenance** - Routine upkeep tasks

Everything else gets **deferred** to protect your focus.

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
```

The LLM decides which tools to call based on the stress analysis, creating intelligent workload management.

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

## Project Structure

```
burnout-app/
├── backend/                    # Java Spring Boot backend
│   ├── src/main/java/         # LangChain4j agents, services
│   └── pom.xml
├── mcp-app/                   # MCP App (Node.js)
│   ├── src/
│   │   ├── index.ts           # MCP server with tools
│   │   ├── backend-client.ts  # Backend API integration
│   │   ├── demo-data.ts       # Fallback demo data
│   │   └── ui/                # SVG wheel visualization
│   ├── package.json
│   └── tsconfig.json
├── scripts/                   # Utility scripts
│   ├── seed-issues.sh         # Create demo issues
│   └── setup-labels.sh        # Setup GitHub labels
├── .vscode/
│   └── mcp.json               # MCP server configuration
└── README.md
```

## Configuration

### Environment Variables (`.env`)

```env
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_DEPLOYMENT=gpt-4o
```

### MCP Configuration (`.vscode/mcp.json`)

```json
{
  "servers": {
    "burnout-app": {
      "type": "stdio",
      "command": "node",
      "args": ["${workspaceFolder}/mcp-app/dist/index.js"],
      "env": {
        "BACKEND_URL": "http://localhost:8080",
        "GITHUB_TOKEN": ""
      }
    }
  }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **MCP tools disabled** | Reload VS Code (Ctrl+Shift+P → "Developer: Reload Window") |
| **Sync fails with auth error** | Run `gh auth login` and ensure `repo` scope is granted |
| **Backend returns 400** | Make sure backend is running: `java -jar target/burnout-backend-0.0.1-SNAPSHOT.jar` |
| **GITHUB_TOKEN conflicts** | The mcp.json sets `GITHUB_TOKEN: ""` to use keyring auth instead |
| **Issues not showing** | Run sync_issues first, then show_burnout_wheel |

## License

MIT
