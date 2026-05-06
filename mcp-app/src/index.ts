#!/usr/bin/env node
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
import { config } from './config.js';
import {
  getReshapeData,
  getStressScore,
  syncIssues,
  type ReshapeResponse,
  type StressResponse,
  type Issue,
  type MutationOutcome,
} from './backend-client.js';
import { getDemoReshapeData, getDemoStressData } from './demo-data.js';
import { generateWheelUI } from './ui/burnout-wheel.js';
import { generateFlamegraphUI } from './ui/burnout-flamegraph.js';

// Create MCP server
const server = new McpServer({
  name: 'burnout-app',
  version: '1.0.0',
});

// Helper to log to stderr (stdout is for MCP protocol)
function log(message: string) {
  console.error(`[burnout-app] ${message}`);
}

// ============================================================================
// Formatting helpers
// ============================================================================

function stressIndicator(score: number): string {
  if (score < 0) return '⚪';
  if (score < 30) return '🟢';
  if (score < 60) return '🟡';
  return '🔴';
}

function levelFromScore(score: number): string {
  if (score < 0) return 'UNKNOWN';
  if (score < 30) return 'LOW';
  if (score < 60) return 'MODERATE';
  return 'HIGH';
}

function formatBreakdown(breakdown: Record<string, number> | undefined): string {
  if (!breakdown) return '';
  const labels: Record<string, string> = {
    workload: 'Workload',
    chaos: 'Chaos',
    contextSwitching: 'Context Switching',
    clarity: 'Clarity',
    sustained: 'Sustained Load',
    afterHours: 'After Hours',
  };
  const lines = Object.entries(breakdown)
    .filter(([, v]) => typeof v === 'number')
    .map(([k, v]) => `- ${labels[k] ?? k}: ${v}`);
  return lines.length ? `\n**Breakdown**\n${lines.join('\n')}` : '';
}

// ============================================================================
// UI Resource: Burnout Wheel
// ============================================================================

server.resource(
  'burnout-wheel-ui',
  'ui://burnout-app/wheel',
  {
    description: 'Interactive 3-3-3 day structure wheel visualization',
    mimeType: 'text/html;profile=mcp-app',
  },
  async (uri) => {
    log(`📱 resources/read called for: ${uri.href}`);
    return {
      contents: [{
        uri: uri.href,
        mimeType: 'text/html;profile=mcp-app',
        text: generateWheelUI(),
        _meta: {
          ui: {
            csp: {},
            prefersBorder: false,
          },
        },
      }],
    };
  }
);

// ============================================================================
// UI Resource: Burnout Flamegraph
// ============================================================================

server.resource(
  'burnout-flamegraph-ui',
  'ui://burnout-app/flamegraph',
  {
    description: 'Interactive flamegraph visualization showing issue stress levels',
    mimeType: 'text/html;profile=mcp-app',
  },
  async (uri) => {
    log(`📱 resources/read called for: ${uri.href}`);
    return {
      contents: [{
        uri: uri.href,
        mimeType: 'text/html;profile=mcp-app',
        text: generateFlamegraphUI(),
        _meta: {
          ui: {
            csp: {},
            prefersBorder: false,
          },
        },
      }],
    };
  }
);

// ============================================================================
// Tool: Show Burnout Wheel (read-only flamegraph view, dry-run)
// ============================================================================

server.registerTool(
  'show_burnout_wheel',
  {
    description:
      'Display an interactive flamegraph visualization for a GitHub repository. Read-only — does not modify any issues.',
    inputSchema: {
      repo: z.string().describe('GitHub repository in owner/repo format'),
    },
    _meta: {
      ui: {
        resourceUri: 'ui://burnout-app/flamegraph',
        visibility: ['model', 'app'],
      },
    },
  },
  async ({ repo }) => {
    if (!repo) {
      return {
        content: [{ type: 'text' as const, text: '❌ Please specify a repository (e.g., "show burnout wheel for owner/repo")' }],
      };
    }
    log(`show_burnout_wheel called for ${repo}`);

    let data: ReshapeResponse;
    let isDemo = false;

    try {
      data = await getReshapeData(repo, undefined, false);
      log(`Backend returned data with stress score: ${data.stressScore}`);

      if (data.stressScore < 0) {
        log('Backend returned not-synced, using demo mode');
        data = getDemoReshapeData(repo);
        isDemo = true;
      }
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : String(error);
      log(`Backend unavailable, using demo data: ${errorMsg}`);
      data = getDemoReshapeData(repo);
      isDemo = true;
    }

    const plan = data.dayPlan;
    const deepWork = plan?.deepWork
      ? `🎯 **Deep Work**: #${plan.deepWork.number} - ${plan.deepWork.title}`
      : '🎯 **Deep Work**: None';
    const quickWins = `⚡ **Quick Wins** (${plan?.quickWins?.length || 0}): ${plan?.quickWins?.slice(0, 3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const maintenance = `🔧 **Maintenance** (${plan?.maintenance?.length || 0}): ${plan?.maintenance?.slice(0, 3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const deferred = `📦 **Deferred** (${plan?.deferred?.length || 0})`;
    const indicator = stressIndicator(data.stressScore);
    const level = data.stressLevel ?? levelFromScore(data.stressScore);

    const summary = [
      `## 📊 3-3-3 Day Plan for ${repo}`,
      '',
      deepWork,
      quickWins,
      maintenance,
      deferred,
      '',
      `${indicator} **Stress Score**: ${data.stressScore}/100 (${level})`,
      `🎉 **Friday Score**: ${data.fridayScore}%`,
      typeof data.expectedStressScore === 'number' && data.expectedStressScore !== data.stressScore
        ? `🔮 **Projected after reshape**: ${data.expectedStressScore}/100 (drop of ${Math.max(0, data.stressScore - data.expectedStressScore)})`
        : '',
      '',
      isDemo ? '*Demo data - sync the repo for real issues*' : '',
    ].filter(Boolean).join('\n');

    return {
      content: [{
        type: 'text' as const,
        text: summary,
      }],
      structuredContent: {
        repo,
        dayPlan: data.dayPlan,
        stressScore: data.stressScore,
        stressLevel: level,
        expectedStressScore: data.expectedStressScore,
        fridayScore: data.fridayScore,
        agentExplanation: data.agentExplanation,
        isDemo,
      },
    };
  }
);

// ============================================================================
// Tool: Reshape Day (deterministic pre-pass + LangChain4j supervisor)
// ============================================================================

server.tool(
  'reshape_day',
  'Run the deterministic pre-pass (triageUrgent + defuseChaosInputs) then the LangChain4j supervisor with 6 sub-agents. Applies the resulting GitHub label/comment changes. Surfaces BEFORE→AFTER stress drop.',
  {
    repo: z.string().describe('GitHub repository in owner/repo format'),
  },
  async ({ repo }, extra) => {
    if (!repo) {
      return {
        content: [{ type: 'text', text: '❌ Please specify a repository (e.g., "reshape day for owner/repo")' }],
      };
    }
    log(`reshape_day called for ${repo}`);

    const progressToken = (extra as any)._meta?.progressToken;
    const sendProgress = async (progress: number, message: string) => {
      if (progressToken === undefined) return;
      try {
        await (extra as any).sendNotification?.({
          method: 'notifications/progress',
          params: { progressToken, progress, message },
        });
      } catch {
        // best-effort — clients can drop progress
      }
    };

    await sendProgress(0, `🔄 Analyzing ${repo}...`);
    await sendProgress(25, '🧹 Deterministic pre-pass (triage + chaos defuser)...');
    await sendProgress(50, '🤖 Supervisor + 6 sub-agents...');

    let data: ReshapeResponse & { mutationOutcome?: MutationOutcome };
    let isDemo = false;

    try {
      data = await getReshapeData(repo, undefined, true);
      if (data.stressScore < 0) {
        log('Backend returned not-synced, using demo mode');
        data = getDemoReshapeData(repo);
        isDemo = true;
      }
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      data = getDemoReshapeData(repo);
      isDemo = true;
    }

    await sendProgress(100, '✅ Plan ready!');

    const totalActions = data.actionPlan?.actions?.length || 0;
    const outcome = data.mutationOutcome;
    const before = data.stressScore;
    const after = typeof data.expectedStressScore === 'number' ? data.expectedStressScore : before;
    const drop = Math.max(0, before - after);
    const beforeLevel = data.stressLevel ?? levelFromScore(before);
    const afterLevel = levelFromScore(after);
    const llmTag = data.llmEnabled === false ? ' *(deterministic fallback — LLM unavailable)*' : '';
    const triaged = data.deterministicTriageCount ?? 0;
    const defused = data.deterministicDefuseCount ?? 0;
    const afterHours = data.afterHoursIssues ?? 0;
    const prePassLine = (triaged > 0 || defused > 0)
      ? `🧹 **Deterministic pre-pass**: triaged ${triaged} unassigned-urgent · defused ${defused} chaos input(s)`
      : '';
    const afterHoursLine = afterHours > 0
      ? `🌙 **After-hours issues (before reshape)**: ${afterHours}`
      : '';

    const summary = [
      `## 📊 Reshape Complete — ${repo}${llmTag}`,
      '',
      `${stressIndicator(before)} **Before**: ${before}/100 (${beforeLevel})`,
      `${stressIndicator(after)} **After**:  ${after}/100 (${afterLevel})`,
      drop > 0 ? `📉 **Drop**: -${drop} points` : '',
      prePassLine,
      afterHoursLine,
      '',
      data.dayPlan.deepWork
        ? `🎯 **Deep Work**: #${data.dayPlan.deepWork.number} - ${data.dayPlan.deepWork.title}`
        : '🎯 **Deep Work**: None assigned',
      `⚡ **Quick Wins**: ${data.dayPlan.quickWins.length}`,
      `🔧 **Maintenance**: ${data.dayPlan.maintenance.length}`,
      `📦 **Deferred**: ${data.dayPlan.deferred.length}`,
      '',
      `🎉 **Friday Score**: ${data.fridayScore}%`,
      '',
      isDemo
        ? '*⚠️ Demo data — backend not synced or unreachable*'
        : outcome
          ? `✨ **GitHub mutations**: ${outcome.applied} applied, ${outcome.skipped} skipped, ${outcome.failed} failed (of ${totalActions} planned)`
          : totalActions > 0
            ? `📝 **Planned mutations**: ${totalActions} (none executed — dry run)`
            : '',
      data.protectiveTriggered && data.protectiveMessage
        ? `\n💚 **Protective**: ${data.protectiveMessage}`
        : '',
    ].filter(Boolean).join('\n');

    return {
      content: [{
        type: 'text',
        text: summary,
      }],
      structuredContent: {
        repo,
        dayPlan: data.dayPlan,
        stressScore: before,
        expectedStressScore: after,
        stressLevel: beforeLevel,
        fridayScore: data.fridayScore,
        agentExplanation: data.agentExplanation,
        mutations: data.actionPlan?.actions || [],
        mutationOutcome: outcome,
        llmEnabled: data.llmEnabled !== false,
        deterministicTriageCount: triaged,
        deterministicDefuseCount: defused,
        afterHoursIssues: afterHours,
        isDemo,
      },
      _meta: {
        ui: {
          resourceUri: 'ui://burnout-app/flamegraph',
        },
      },
    };
  }
);

// ============================================================================
// Tool: Get Stress Score
// ============================================================================

server.tool(
  'get_stress_score',
  'Get a quick stress score (0-100) and 6-metric breakdown (workload, chaos, context switching, clarity, sustained load, after hours).',
  {
    repo: z.string().describe('GitHub repository in owner/repo format'),
  },
  async ({ repo }) => {
    if (!repo) {
      return {
        content: [{ type: 'text', text: '❌ Please specify a repository' }],
      };
    }
    log(`get_stress_score called for ${repo}`);

    let data: StressResponse;
    let isDemo = false;

    try {
      data = await getStressScore(repo);
      if (data.stressScore < 0) {
        data = getDemoStressData();
        isDemo = true;
      }
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      data = getDemoStressData();
      isDemo = true;
    }

    const score = data.stressScore ?? 0;
    const level = data.stressLevel ?? levelFromScore(score);
    const indicator = stressIndicator(score);
    const projected =
      typeof data.expectedStressScore === 'number' &&
      data.expectedStressScore !== score
        ? `\n🔮 **Projected after reshape**: ${data.expectedStressScore}/100`
        : '';
    const compliance =
      typeof data.is333Compliant === 'boolean'
        ? `\n✅ **3-3-3 Compliant**: ${data.is333Compliant ? 'yes' : 'no'}`
        : '';
    const demoNote = isDemo ? '\n\n*⚠️ Demo data — backend not synced or unreachable*' : '';

    const text = [
      `${indicator} **Stress Score**: ${score}/100 (${level})`,
      formatBreakdown(data.breakdown),
      projected,
      compliance,
      demoNote,
    ].filter(Boolean).join('');

    return {
      content: [{ type: 'text', text }],
      structuredContent: {
        repo,
        stressScore: score,
        stressLevel: level,
        breakdown: data.breakdown,
        expectedStressScore: data.expectedStressScore,
        is333Compliant: data.is333Compliant,
        isDemo,
      },
    };
  }
);

// ============================================================================
// Tool: Sync Issues
// ============================================================================

server.tool(
  'sync_issues',
  'Fetch open issues from GitHub via gh CLI and sync them to the backend. Required before using other tools. Works with public and private repos.',
  {
    repo: z.string().describe('GitHub repository in owner/repo format'),
  },
  async ({ repo }) => {
    if (!repo) {
      return {
        content: [{ type: 'text', text: '❌ Please specify a repository' }],
      };
    }
    log(`sync_issues called for ${repo}`);

    let issues: Issue[];

    try {
      issues = await syncIssues(repo);
      return {
        content: [{
          type: 'text',
          text: `✅ Synced ${issues.length} issues from ${repo}`,
        }],
        structuredContent: { repo, count: issues.length },
      };
    } catch (error) {
      const err = error as Error;
      const errorMessage = err.message || String(error);
      log(`Sync failed: ${errorMessage}`);

      // gh CLI errors
      if (errorMessage.includes('gh') || errorMessage.includes('Command failed')) {
        return {
          content: [{
            type: 'text',
            text: `❌ Failed to fetch issues from GitHub: ${errorMessage}\n\nMake sure:\n1. gh CLI is installed\n2. You're authenticated (run: gh auth login)\n3. The repo exists and you have access`,
          }],
        };
      }

      return {
        content: [{
          type: 'text',
          text: `❌ Failed to sync issues: ${errorMessage}\n\nMake sure the backend is running at ${config.backendUrl}`,
        }],
      };
    }
  }
);

// ============================================================================
// Start Server
// ============================================================================

async function main() {
  log('Starting burnout-app MCP server...');
  log(`Backend URL: ${config.backendUrl}`);

  const transport = new StdioServerTransport();
  await server.connect(transport);

  log('Server connected and ready');
}

main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
