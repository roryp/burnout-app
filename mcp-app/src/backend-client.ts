import { config } from './config.js';

export interface Issue {
  number: number;
  title: string;
  labels?: string[];
  complexity?: number;
  state?: string;
}

export interface DayPlan {
  deepWork: Issue | null;
  quickWins: Issue[];
  maintenance: Issue[];
  deferred: Issue[];
}

export interface ReshapeResponse {
  dayPlan: DayPlan;
  stressScore: number;
  fridayScore: number;
  agentExplanation: string;
  actionPlan?: {
    actions: Array<{
      type: string;
      issueNumber: number;
      label: string;
    }>;
  };
}

export interface StressResponse {
  stressScore: number;
  stressLevel: string;
  is333Compliant?: boolean;
  initialStressScore?: number;
  expectedStressScore?: number;
}

// Cache the GitHub token to avoid calling gh auth token repeatedly
let cachedToken: string | null = null;

async function getGitHubToken(): Promise<string> {
  if (cachedToken) {
    return cachedToken;
  }

  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);

  try {
    const { stdout } = await execAsync('gh auth token');
    cachedToken = stdout.trim();
    console.error('[Auth] Retrieved GitHub token from gh CLI');
    return cachedToken;
  } catch (error) {
    console.error('[Auth] Failed to get GitHub token. Make sure gh CLI is authenticated.');
    throw new Error('Not authenticated with GitHub. Run: gh auth login');
  }
}

export async function callBackend<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${config.backendUrl}${endpoint}`;
  console.error(`[Backend] Calling ${url}`);
  
  // Get GitHub token for authentication
  const token = await getGitHubToken();
  
  const response = await fetch(url, {
    headers: { 
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    signal: AbortSignal.timeout(30000),
    ...options,
  });
  
  if (!response.ok) {
    throw new Error(`Backend returned ${response.status}: ${response.statusText}`);
  }
  
  return response.json() as Promise<T>;
}

export async function getReshapeData(repo: string, userId?: string): Promise<ReshapeResponse> {
  // Extract owner from repo as default userId
  const effectiveUserId = userId || repo.split('/')[0];
  return callBackend<ReshapeResponse>('/api/reshape', {
    method: 'POST',
    body: JSON.stringify({ repo, userId: effectiveUserId, dryRun: true }),
  });
}

export async function getStressScore(repo: string, userId?: string): Promise<StressResponse> {
  const effectiveUserId = userId || repo.split('/')[0];
  return callBackend<StressResponse>(`/api/stress?repo=${encodeURIComponent(repo)}&userId=${encodeURIComponent(effectiveUserId)}`);
}

// Backend Issue format (matches Java Issue record)
interface BackendIssue {
  number: number;
  title: string;
  body?: string;
  labels: Array<{ name: string }>;
  assignees: Array<{ login: string }>;
  createdAt?: string;
  updatedAt?: string;
  state: string;
}

export async function syncIssues(repo: string): Promise<Issue[]> {
  // Fetch issues from GitHub using gh CLI
  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);
  
  console.error(`[Sync] Fetching issues from GitHub for ${repo}...`);
  
  const { stdout, stderr } = await execAsync(
    `gh issue list --repo ${repo} --state open --json number,title,body,labels,assignees,createdAt,updatedAt,state --limit 100`
  );
  
  if (stderr) {
    console.error(`[Sync] gh CLI stderr: ${stderr}`);
  }
  
  const ghIssues = JSON.parse(stdout) as Array<{
    number: number;
    title: string;
    body: string;
    labels: Array<{ name: string }>;
    assignees: Array<{ login: string }>;
    createdAt: string;
    updatedAt: string;
    state: string;
  }>;
  
  console.error(`[Sync] Fetched ${ghIssues.length} issues from GitHub`);
  
  // Format for backend (keep labels as objects, not strings)
  const backendIssues: BackendIssue[] = ghIssues.map(i => ({
    number: i.number,
    title: i.title,
    body: i.body,
    labels: i.labels, // Keep as [{name: "..."}] format
    assignees: i.assignees, // Keep as [{login: "..."}] format
    createdAt: i.createdAt,
    updatedAt: i.updatedAt,
    state: i.state || 'open',
  }));
  
  // Push to backend
  console.error(`[Sync] Pushing ${backendIssues.length} issues to backend...`);
  await callBackend('/api/issues/sync', {
    method: 'POST',
    body: JSON.stringify({
      repo,
      issues: backendIssues,
      fetchedAt: new Date().toISOString(),
      schemaVersion: 1,
    }),
  });
  
  console.error(`[Sync] Successfully synced ${backendIssues.length} issues`);
  
  // Return simplified format for MCP response
  return ghIssues.map(i => ({
    number: i.number,
    title: i.title,
    labels: i.labels.map(l => l.name),
    state: i.state || 'open',
  }));
}
