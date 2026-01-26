# Burnout-as-a-Service

A VS Code extension + Java backend that demonstrates burnout prevention through accessibility-inspired heuristics, the **3-3-3 day structure**, and GOAP-based action planning.

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- GitHub CLI (`gh`) installed and authenticated
- VS Code

### 1. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

For demo mode with frozen time:
```bash
mvn spring-boot:run -Dspring.profiles.active=demo
```

### 2. Setup Demo Repository

```bash
# Create repo (if needed)
gh repo create roryp/burnout-demo --public --confirm

# Create labels
bash scripts/setup-labels.sh roryp/burnout-demo

# Seed chaotic issues
bash scripts/seed-issues.sh roryp/burnout-demo
```

### 3. Run the Extension

```bash
cd extension
npm install
npm run compile
```

Then press F5 in VS Code to launch the extension in debug mode.

### 4. Demo Commands

- **Burnout: Preflight Check** - Verify all systems ready
- **Burnout: Reshape My Day (Preview)** - Analyze and preview 3-3-3 plan
- **Burnout: Apply Plan** - Execute the planned GitHub mutations
- **Burnout: Toggle Demo Mode** - Stop polling for stage demos
- **Burnout: Show 3-3-3 Wheel** - Display day structure visualization

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
│  Java Backend (Spring Boot)                                          │
│  - Issue sync cache                                                  │
│  - Chaos metrics (deterministic scoring)                             │
│  - 3-3-3 compliance analysis                                         │
│  - GOAP planner (cost-based greedy)                                  │
│  - NO direct GitHub calls                                            │
└─────────────────────────────────────────────────────────────────────┘
```

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

- **1 Deep Work** - One critical, focused task
- **3 Quick Wins** - Small, completable items (<30 min each)
- **3 Maintenance** - Routine upkeep tasks

Everything else gets **deferred** to protect your focus.

## Demo Labels

For stage-safe demos, use these labels to simulate time conditions:

- `demo:stale-14d` - Simulate 14-day staleness
- `demo:after-hours` - Simulate late-night work
- `demo:touched-today` - Simulate recent activity
- `demo:friday` - Force Friday deploy scenario

## GOAP Planner

The Goal-Oriented Action Planning (GOAP) system:

1. **Goals**: PreventBurnout, Achieve333Compliance, ProtectDeepWork, etc.
2. **Actions**: DeferIssue, MarkDeepWorkFocus, ReclassifyAsQuickWin, etc.
3. **Planner**: Greedy cost-based selection to satisfy goals

All decisions are deterministic. The LLM (if configured) only explains results.

## Configuration

### Backend (`application.yml`)

```yaml
demo:
  repo: roryp/burnout-demo
  userId: roryp
  friday:
    enabled: false  # Set true to force Friday mode
  clock:
    zone: America/New_York
```

### Extension (`src/config.ts`)

```typescript
export const DemoConfig = {
    REPO: 'roryp/burnout-demo',
    USER_ID: 'roryp',
    BACKEND_URL: 'http://localhost:8080',
};
```

## License

MIT
