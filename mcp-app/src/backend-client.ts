import { config } from './config.js';

// ============================================================================
// Types — match backend Java records exactly.
// Backend Issue: number, title, body, labels: [{name}], assignees: [{login}],
//                createdAt, updatedAt, state, milestone
// ============================================================================

export interface IssueLabel {
  name: string;
}

export interface IssueAssignee {
  login: string;
}

export interface Issue {
  number: number;
  title: string;
  body?: string;
  labels?: IssueLabel[] | string[]; // backend returns objects; demo data may use strings
  assignees?: IssueAssignee[];
  createdAt?: string;
  updatedAt?: string;
  state?: string;
  /** Demo-only synthetic field (not on backend record). */
  complexity?: number;
}

export interface DayPlan {
  deepWork: Issue | null;
  quickWins: Issue[];
  maintenance: Issue[];
  deferred: Issue[];
}

/**
 * Mirrors backend `GitHubAction` sealed interface — 6 record types. Five are
 * actively emitted: AddLabels, RemoveLabels, Comment, Unassign, SetBody.
 * The sixth, SetUpdatedAt, is legacy: retained for backward compatibility with
 * persisted plans but no longer emitted by any reshape path under the
 * "acknowledge-don't-erase" rule (real after-hours / recent-touch timestamps
 * are preserved). Handler below silently skips it.
 */
export interface GitHubAction {
  type: 'AddLabels' | 'RemoveLabels' | 'Comment' | 'Unassign' | 'SetBody' | 'SetUpdatedAt';
  issueNumber: number;
  labels?: string[];
  body?: string;
  login?: string;
  updatedAt?: string;
}

export interface ChaosMetrics {
  score?: number;
  unassignedUrgentCount?: number;
  highPriorityCount?: number;
  staleUrgentCount?: number;
  recentlyTouchedCount?: number;
  afterHoursSignal?: boolean;
}

export interface ComplianceReport {
  isCompliant?: boolean;
  violations?: string[];
}

export interface ReshapeResponse {
  status?: string;
  dayPlan: DayPlan;
  actionPlan?: {
    repo: string;
    actions: GitHubAction[];
  };
  chaos?: ChaosMetrics;
  compliance?: ComplianceReport;
  stressScore: number;
  stressLevel?: string;
  /** Supervisor's projected stress AFTER mutations apply. Key for BEFORE→AFTER demo. */
  expectedStressScore?: number;
  fridayScore: number;
  agentExplanation: string;
  protectiveTriggered?: boolean;
  protectiveMessage?: string;
  llmEnabled?: boolean;
  /** Number of unassigned-urgent issues that the deterministic pre-pass triaged before the LLM ran. */
  deterministicTriageCount?: number;
  /** Number of issues whose body or updatedAt was normalised by the deterministic chaos defuser. */
  deterministicDefuseCount?: number;
  /** Issues with updatedAt outside 9 AM–6 PM in the active timezone (or weekends). */
  afterHoursIssues?: number;
}

export interface StressResponse {
  stressScore: number;
  stressLevel: string;
  breakdown?: Record<string, number>;
  is333Compliant?: boolean;
  initialStressScore?: number;
  expectedStressScore?: number;
}

// ============================================================================
// GitHub token resolution (gh CLI keyring → env fallback)
// ============================================================================

let cachedToken: string | null = null;

async function getGitHubToken(): Promise<string> {
  if (cachedToken) {
    return cachedToken;
  }

  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);

  // Try gh CLI keyring first (preferred — has full scopes)
  try {
    const env = { ...process.env, GITHUB_TOKEN: '' };
    const { stdout } = await execAsync('gh auth token', { env });
    const token = stdout.trim();
    if (token) {
      cachedToken = token;
      console.error('[Auth] Retrieved GitHub token from gh CLI (keyring)');
      return cachedToken;
    }
  } catch {
    // fall through to env var (e.g. Codespaces GITHUB_TOKEN)
  }

  // Fallback: GITHUB_TOKEN env var (Codespaces, CI, etc.)
  const envToken = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
  if (envToken) {
    cachedToken = envToken;
    console.error('[Auth] Retrieved GitHub token from environment variable');
    return cachedToken;
  }

  console.error('[Auth] No GitHub token found. Run: gh auth login');
  throw new Error('Not authenticated with GitHub. Run: gh auth login');
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
    signal: AbortSignal.timeout(60_000), // supervisor + LLM can take 30s+
    ...options,
  });

  if (!response.ok) {
    throw new Error(`Backend returned ${response.status}: ${response.statusText}`);
  }

  return response.json() as Promise<T>;
}

// ============================================================================
// Mutation execution via gh CLI
// Handles all 6 GitHubAction record types from the backend.
// ============================================================================

export interface MutationOutcome {
  applied: number;
  skipped: number;
  failed: number;
}

async function executeMutations(repo: string, actions: GitHubAction[]): Promise<MutationOutcome> {
  const { exec, execFile } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);
  const execFileAsync = promisify(execFile);

  // Prefer keyring token (full scopes); fall back to env (Codespaces GITHUB_TOKEN)
  let env: NodeJS.ProcessEnv = { ...process.env, GITHUB_TOKEN: '' };
  try {
    await execAsync('gh auth token', { env });
  } catch {
    env = { ...process.env };
  }

  const outcome: MutationOutcome = { applied: 0, skipped: 0, failed: 0 };
  // Cache labels we've already created/verified to avoid spamming `gh label create`.
  const ensuredLabels = new Set<string>();

  for (const action of actions) {
    try {
      switch (action.type) {
        case 'AddLabels': {
          if (!action.labels?.length) { outcome.skipped++; break; }
          for (const label of action.labels) {
            if (ensuredLabels.has(label)) continue;
            try {
              await execAsync(
                `gh label create "${label}" --repo ${repo} --force --color auto`,
                { env }
              );
            } catch {
              // already exists — fine
            }
            ensuredLabels.add(label);
          }
          const labelsArg = action.labels.map(l => `--add-label "${l}"`).join(' ');
          await execAsync(
            `gh issue edit ${action.issueNumber} --repo ${repo} ${labelsArg}`,
            { env }
          );
          console.error(`[Mutations] +labels [${action.labels.join(', ')}] on #${action.issueNumber}`);
          outcome.applied++;
          break;
        }

        case 'RemoveLabels': {
          if (!action.labels?.length) { outcome.skipped++; break; }
          const labelsArg = action.labels.map(l => `--remove-label "${l}"`).join(' ');
          await execAsync(
            `gh issue edit ${action.issueNumber} --repo ${repo} ${labelsArg}`,
            { env }
          );
          console.error(`[Mutations] -labels [${action.labels.join(', ')}] on #${action.issueNumber}`);
          outcome.applied++;
          break;
        }

        case 'Comment': {
          if (!action.body) { outcome.skipped++; break; }
          await execFileAsync(
            'gh',
            ['issue', 'comment', String(action.issueNumber), '--repo', repo, '--body', action.body],
            { env }
          );
          console.error(`[Mutations] comment on #${action.issueNumber}`);
          outcome.applied++;
          break;
        }

        case 'Unassign': {
          if (!action.login) { outcome.skipped++; break; }
          await execAsync(
            `gh issue edit ${action.issueNumber} --repo ${repo} --remove-assignee "${action.login}"`,
            { env }
          );
          console.error(`[Mutations] unassign @${action.login} from #${action.issueNumber}`);
          outcome.applied++;
          break;
        }

        case 'SetBody': {
          // Demo-only chaos defuser. The backend rewrites empty bodies in IssueCache;
          // on real GitHub we surface it as a scope-clarification comment instead.
          if (!action.body) { outcome.skipped++; break; }
          await execFileAsync(
            'gh',
            [
              'issue', 'comment', String(action.issueNumber), '--repo', repo,
              '--body', `🤖 Scope clarification needed:\n\n${action.body}`,
            ],
            { env }
          );
          console.error(`[Mutations] scope-prompt comment on #${action.issueNumber} (SetBody → comment)`);
          outcome.applied++;
          break;
        }

        case 'SetUpdatedAt': {
          // Legacy demo-only timestamp normalization — no GitHub API equivalent and
          // no longer emitted by current reshape paths (acknowledge-don't-erase).
          // Skip silently for backward compatibility with any persisted plans.
          console.error(`[Mutations] skip SetUpdatedAt on #${action.issueNumber} (legacy, no-op)`);
          outcome.skipped++;
          break;
        }

        default: {
          console.error(`[Mutations] unknown action type: ${(action as any).type}`);
          outcome.skipped++;
        }
      }
    } catch (error) {
      console.error(`[Mutations] FAILED ${action.type} on #${action.issueNumber}: ${error}`);
      outcome.failed++;
    }
  }

  console.error(
    `[Mutations] Done — applied=${outcome.applied} skipped=${outcome.skipped} failed=${outcome.failed}`
  );
  return outcome;
}

// ============================================================================
// Public API
// ============================================================================

export async function getReshapeData(
  repo: string,
  userId?: string,
  applyMutations: boolean = false
): Promise<ReshapeResponse & { mutationOutcome?: MutationOutcome }> {
  const effectiveUserId = userId || repo.split('/')[0];
  const response = await callBackend<ReshapeResponse>('/api/reshape', {
    method: 'POST',
    body: JSON.stringify({ repo, userId: effectiveUserId, dryRun: !applyMutations }),
  });

  // Execute mutations if requested and available
  if (applyMutations && response.actionPlan?.actions?.length) {
    console.error(
      `[Mutations] Executing ${response.actionPlan.actions.length} GitHub mutations...`
    );
    const mutationOutcome = await executeMutations(repo, response.actionPlan.actions);
    return { ...response, mutationOutcome };
  }

  return response;
}

export async function getStressScore(repo: string, userId?: string): Promise<StressResponse> {
  const effectiveUserId = userId || repo.split('/')[0];
  return callBackend<StressResponse>(
    `/api/stress?repo=${encodeURIComponent(repo)}&userId=${encodeURIComponent(effectiveUserId)}`
  );
}

// Backend Issue payload (matches Java Issue record)
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
  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);

  console.error(`[Sync] Fetching issues from GitHub for ${repo}...`);

  // Prefer keyring token (full scopes); fall back to env var (Codespaces)
  let env: NodeJS.ProcessEnv = { ...process.env, GITHUB_TOKEN: '' };
  let stdout = '';
  let stderr = '';
  try {
    const r = await execAsync(
      `gh issue list --repo ${repo} --state open --json number,title,body,labels,assignees,createdAt,updatedAt,state --limit 100`,
      { env }
    );
    stdout = r.stdout; stderr = r.stderr;
  } catch {
    const r = await execAsync(
      `gh issue list --repo ${repo} --state open --json number,title,body,labels,assignees,createdAt,updatedAt,state --limit 100`
    );
    stdout = r.stdout; stderr = r.stderr;
  }

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

  // Format for backend (keep labels/assignees as objects to match Java records)
  const backendIssues: BackendIssue[] = ghIssues.map(i => ({
    number: i.number,
    title: i.title,
    body: i.body,
    labels: i.labels,
    assignees: i.assignees,
    createdAt: i.createdAt,
    updatedAt: i.updatedAt,
    state: i.state || 'open',
  }));

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

  // Return MCP-friendly format (object labels preserved for downstream UI)
  return ghIssues.map(i => ({
    number: i.number,
    title: i.title,
    labels: i.labels,
    assignees: i.assignees,
    state: i.state || 'open',
  }));
}
