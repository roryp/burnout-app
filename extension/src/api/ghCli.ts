import { execFile } from "child_process";
import { promisify } from "util";

const execFileAsync = promisify(execFile);

/**
 * Safe gh CLI wrapper (Windows-compatible):
 * - Uses execFile (not exec) to avoid shell injection
 * - Always uses --json for structured output
 * - Handles non-zero exits with friendly error messages
 */
export async function gh(args: string[], cwd?: string): Promise<string> {
    try {
        const { stdout, stderr } = await execFileAsync("gh", args, { 
            cwd, 
            windowsHide: true,
            maxBuffer: 10 * 1024 * 1024
        });
        
        if (stderr && stderr.trim()) {
            console.debug(`[gh CLI warning]: ${stderr.trim()}`);
        }
        
        return stdout;
    } catch (e: unknown) {
        const err = e as { code?: string; stderr?: string; message?: string };
        
        if (err.code === 'ENOENT') {
            throw new Error('gh CLI not found. Install from https://cli.github.com');
        }
        if (err.stderr?.includes('authentication')) {
            throw new Error('gh CLI not authenticated. Run: gh auth login');
        }
        if (err.stderr?.includes('Could not resolve')) {
            throw new Error(`Repository not found or no access: ${args.join(' ')}`);
        }
        
        throw new Error(`gh CLI failed: ${err.message || 'Unknown error'}`);
    }
}

export async function ghJson<T>(args: string[], cwd?: string): Promise<T> {
    const stdout = await gh(args, cwd);
    try {
        return JSON.parse(stdout.trim());
    } catch {
        throw new Error(`Failed to parse gh CLI JSON output: ${stdout.slice(0, 200)}...`);
    }
}

export interface Issue {
    number: number;
    title: string;
    body: string | null;
    labels: { name: string }[];
    assignees: { login: string }[];
    createdAt: string;
    updatedAt: string;
    state: string;
    milestone: { title: string; dueOn: string } | null;
}

export async function fetchIssues(repo: string): Promise<Issue[]> {
    return await ghJson<Issue[]>([
        "issue", "list",
        "-R", repo,
        "--json", "number,title,body,labels,assignees,createdAt,updatedAt,state,milestone",
        "--limit", "100"
    ]);
}

export async function addLabels(repo: string, issueNumber: number, labels: string[]): Promise<void> {
    await gh([
        "issue", "edit", String(issueNumber),
        "-R", repo,
        "--add-label", labels.join(",")
    ]);
}

export async function removeLabels(repo: string, issueNumber: number, labels: string[]): Promise<void> {
    await gh([
        "issue", "edit", String(issueNumber),
        "-R", repo,
        "--remove-label", labels.join(",")
    ]);
}

export async function comment(repo: string, issueNumber: number, body: string): Promise<void> {
    await gh([
        "issue", "comment", String(issueNumber),
        "-R", repo,
        "-b", body
    ]);
}

export async function getIssueLabels(repo: string, issueNumber: number): Promise<string[]> {
    const issue = await ghJson<{ labels: { name: string }[] }>([
        "issue", "view", String(issueNumber),
        "-R", repo,
        "--json", "labels"
    ]);
    return issue.labels.map(l => l.name);
}
