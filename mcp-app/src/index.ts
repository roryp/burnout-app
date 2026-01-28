#!/usr/bin/env node
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
import { config } from './config.js';
import { getReshapeData, getStressScore, syncIssues, type ReshapeResponse, type StressResponse, type Issue } from './backend-client.js';
import { getDemoReshapeData, getDemoStressData, getDemoIssues } from './demo-data.js';
import { generateWheelUI } from './ui/burnout-wheel.js';

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
// Tool: Show Day Plan (with UI visualization)
// ============================================================================

server.registerTool(
  'show_burnout_wheel',
  {
    description: 'Display an interactive 3-3-3 day structure wheel for a GitHub repository. Shows deep work, quick wins, maintenance tasks, and stress score.',
    inputSchema: {
      repo: z.string().describe('GitHub repository in owner/repo format'),
    },
    _meta: {
      ui: {
        resourceUri: 'ui://burnout-app/wheel',
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
      data = await getReshapeData(repo);
      log(`Backend returned data with stress score: ${data.stressScore}`);
      
      // If backend returns empty data (not synced), fall back to demo
      if (!data.dayPlan || data.stressScore < 0) {
        log('Backend returned empty data, using demo mode');
        data = getDemoReshapeData(repo);
        isDemo = true;
      }
    } catch (error) {
      log(`Backend unavailable, using demo data: ${error}`);
      data = getDemoReshapeData(repo);
      isDemo = true;
    }
    
    // Build text summary for the model
    const plan = data.dayPlan;
    const deepWork = plan?.deepWork ? `🎯 **Deep Work**: #${plan.deepWork.number} - ${plan.deepWork.title}` : '🎯 **Deep Work**: None';
    const quickWins = `⚡ **Quick Wins** (${plan?.quickWins?.length || 0}): ${plan?.quickWins?.slice(0,3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const maintenance = `🔧 **Maintenance** (${plan?.maintenance?.length || 0}): ${plan?.maintenance?.slice(0,3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const stress = data.stressScore < 30 ? '🟢' : data.stressScore < 60 ? '🟡' : '🔴';
    
    const summary = [
      `## 📊 3-3-3 Day Plan for ${repo}`,
      '',
      deepWork,
      quickWins,
      maintenance,
      '',
      `${stress} **Stress Score**: ${data.stressScore}/100`,
      `🎉 **Friday Score**: ${data.fridayScore}%`,
      '',
      isDemo ? '*Demo data - connect backend for real issues*' : '',
    ].filter(Boolean).join('\n');
    
    return {
      content: [{
        type: 'text' as const,
        text: summary,
      }],
      // SEP-1865: structuredContent for the UI panel
      structuredContent: {
        repo,
        dayPlan: data.dayPlan,
        stressScore: data.stressScore,
        fridayScore: data.fridayScore,
        agentExplanation: data.agentExplanation,
        isDemo,
      },
    };
  }
);

// ============================================================================
// Tool: Reshape Day (AI agent analysis)
// ============================================================================

server.tool(
  'reshape_day',
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
    
    // Progress notification
    const progressToken = (extra as any)._meta?.progressToken;
    
    if (progressToken !== undefined) {
      await (extra as any).sendNotification?.({
        method: 'notifications/progress',
        params: { progressToken, progress: 0, message: `🔄 Analyzing ${repo}...` },
      });
    }
    
    let data: ReshapeResponse;
    let isDemo = false;
    
    try {
      data = await getReshapeData(repo);
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      data = getDemoReshapeData(repo);
      isDemo = true;
    }
    
    if (progressToken !== undefined) {
      await (extra as any).sendNotification?.({
        method: 'notifications/progress',
        params: { progressToken, progress: 100, message: `✅ Plan ready!` },
      });
    }
    
    const summary = [
      `## 📊 3-3-3 Day Plan for ${repo}`,
      '',
      data.dayPlan.deepWork 
        ? `🎯 **Deep Work**: #${data.dayPlan.deepWork.number} - ${data.dayPlan.deepWork.title}`
        : '🎯 **Deep Work**: None assigned',
      `⚡ **Quick Wins**: ${data.dayPlan.quickWins.length} tasks`,
      `🔧 **Maintenance**: ${data.dayPlan.maintenance.length} tasks`,
      `📦 **Deferred**: ${data.dayPlan.deferred.length} tasks`,
      '',
      `**Stress Score**: ${data.stressScore}/100`,
      `**Friday Score**: ${data.fridayScore}%`,
      '',
      isDemo ? '*⚠️ Demo data - backend not available*' : '',
    ].filter(Boolean).join('\n');
    
    return {
      content: [{
        type: 'text',
        text: summary,
      }],
      structuredContent: {
        repo,
        dayPlan: data.dayPlan,
        stressScore: data.stressScore,
        fridayScore: data.fridayScore,
        agentExplanation: data.agentExplanation,
        mutations: data.actionPlan?.actions || [],
        isDemo,
      },
      _meta: {
        ui: {
          resourceUri: 'ui://burnout-app/wheel',
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
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      data = getDemoStressData();
      isDemo = true;
    }
    
    const score = data.stressScore ?? 0;
    const level = data.stressLevel ?? 'UNKNOWN';
    const indicator = score < 30 ? '🟢' : score < 60 ? '🟡' : '🔴';
    const demoNote = isDemo ? ' *(demo data)*' : '';
    
    return {
      content: [{
        type: 'text',
        text: `${indicator} **Stress Score**: ${score}/100 (${level})${demoNote}`,
      }],
    };
  }
);

// ============================================================================
// Tool: Sync Issues
// ============================================================================

server.tool(
  'sync_issues',
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
    let errorMessage = '';
    
    try {
      issues = await syncIssues(repo);
      return {
        content: [{
          type: 'text',
          text: `✅ Synced ${issues.length} issues from ${repo}`,
        }],
      };
    } catch (error) {
      const err = error as Error;
      errorMessage = err.message || String(error);
      log(`Sync failed: ${errorMessage}`);
      
      // Check if it's a gh CLI error
      if (errorMessage.includes('gh') || errorMessage.includes('Command failed')) {
        return {
          content: [{
            type: 'text',
            text: `❌ Failed to fetch issues from GitHub: ${errorMessage}\n\nMake sure:\n1. gh CLI is installed\n2. You're authenticated (run: gh auth login)\n3. The repo exists and you have access`,
          }],
        };
      }
      
      // Backend error
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
