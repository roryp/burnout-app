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
  async (uri) => ({
    contents: [{
      uri: uri.href,
      mimeType: 'text/html;profile=mcp-app',
      text: generateWheelUI(),
    }],
  })
);

// ============================================================================
// Tool: Show Day Plan (with UI visualization)
// ============================================================================

server.tool(
  'show_burnout_wheel',
  {
    repo: z.string().optional().describe('GitHub repository (owner/repo). Defaults to roryp/burnout-app'),
  },
  async ({ repo }) => {
    const targetRepo = repo || config.defaultRepo;
    log(`show_burnout_wheel called for ${targetRepo}`);
    
    let data: ReshapeResponse;
    let isDemo = false;
    
    try {
      data = await getReshapeData(targetRepo);
      log(`Backend returned data with stress score: ${data.stressScore}`);
    } catch (error) {
      log(`Backend unavailable, using demo data: ${error}`);
      data = getDemoReshapeData(targetRepo);
      isDemo = true;
    }
    
    // Build text summary
    const plan = data.dayPlan;
    const deepWork = plan?.deepWork ? `🎯 **Deep Work**: #${plan.deepWork.number} - ${plan.deepWork.title}` : '🎯 **Deep Work**: None';
    const quickWins = `⚡ **Quick Wins** (${plan?.quickWins?.length || 0}): ${plan?.quickWins?.slice(0,3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const maintenance = `🔧 **Maintenance** (${plan?.maintenance?.length || 0}): ${plan?.maintenance?.slice(0,3).map(i => `#${i.number}`).join(', ') || 'None'}`;
    const stress = data.stressScore < 30 ? '🟢' : data.stressScore < 60 ? '🟡' : '🔴';
    
    const summary = [
      `## 📊 3-3-3 Day Plan for ${targetRepo}`,
      '',
      deepWork,
      quickWins,
      maintenance,
      '',
      `${stress} **Stress Score**: ${data.stressScore}/100`,
      `🎉 **Friday Score**: ${data.fridayScore}%`,
      '',
      isDemo ? '*⚠️ Demo data - backend not available*' : '',
      '',
      data.agentExplanation || '',
    ].filter(Boolean).join('\n');
    
    return {
      content: [{
        type: 'text',
        text: summary,
      }],
    };
  }
);

// ============================================================================
// Tool: Reshape Day (AI agent analysis)
// ============================================================================

server.tool(
  'reshape_day',
  {
    repo: z.string().optional().describe('GitHub repository (owner/repo)'),
  },
  async ({ repo }, extra) => {
    const targetRepo = repo || config.defaultRepo;
    log(`reshape_day called for ${targetRepo}`);
    
    // Progress notification
    const progressToken = (extra as any)._meta?.progressToken;
    
    if (progressToken !== undefined) {
      await (extra as any).sendNotification?.({
        method: 'notifications/progress',
        params: { progressToken, progress: 0, message: `🔄 Analyzing ${targetRepo}...` },
      });
    }
    
    let data: ReshapeResponse;
    let isDemo = false;
    
    try {
      data = await getReshapeData(targetRepo);
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      data = getDemoReshapeData(targetRepo);
      isDemo = true;
    }
    
    if (progressToken !== undefined) {
      await (extra as any).sendNotification?.({
        method: 'notifications/progress',
        params: { progressToken, progress: 100, message: `✅ Plan ready!` },
      });
    }
    
    const summary = [
      `## 📊 3-3-3 Day Plan for ${targetRepo}`,
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
        repo: targetRepo,
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
    repo: z.string().optional().describe('GitHub repository (owner/repo)'),
  },
  async ({ repo }) => {
    const targetRepo = repo || config.defaultRepo;
    log(`get_stress_score called for ${targetRepo}`);
    
    let data: StressResponse;
    let isDemo = false;
    
    try {
      data = await getStressScore(targetRepo);
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
    repo: z.string().optional().describe('GitHub repository (owner/repo)'),
  },
  async ({ repo }) => {
    const targetRepo = repo || config.defaultRepo;
    log(`sync_issues called for ${targetRepo}`);
    
    let issues: Issue[];
    let isDemo = false;
    
    try {
      issues = await syncIssues(targetRepo);
    } catch (error) {
      log(`Backend unavailable: ${error}`);
      issues = getDemoIssues();
      isDemo = true;
    }
    
    const demoNote = isDemo ? ' *(demo data)*' : '';
    
    return {
      content: [{
        type: 'text',
        text: `📋 Synced ${issues.length} issues from ${targetRepo}${demoNote}`,
      }],
    };
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
