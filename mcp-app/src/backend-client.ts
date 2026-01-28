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

export async function callBackend<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${config.backendUrl}${endpoint}`;
  console.error(`[Backend] Calling ${url}`);
  
  const response = await fetch(url, {
    headers: { 
      'Accept': 'application/json',
      'Content-Type': 'application/json',
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

export async function syncIssues(repo: string): Promise<Issue[]> {
  // Fetch issues from GitHub using gh CLI
  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);
  
  const { stdout } = await execAsync(
    `gh issue list --repo ${repo} --state open --json number,title,labels,assignees --limit 100`
  );
  
  const ghIssues = JSON.parse(stdout) as Array<{
    number: number;
    title: string;
    labels: Array<{ name: string }>;
    assignees: Array<{ login: string }>;
  }>;
  
  const issues: Issue[] = ghIssues.map(i => ({
    number: i.number,
    title: i.title,
    labels: i.labels.map(l => l.name),
    state: 'open',
  }));
  
  // Push to backend
  await callBackend('/api/issues/sync', {
    method: 'POST',
    body: JSON.stringify({
      repo,
      issues,
      fetchedAt: new Date().toISOString(),
      schemaVersion: 1,
    }),
  });
  
  return issues;
}
