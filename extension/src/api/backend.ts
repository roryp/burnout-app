import { DemoConfig } from '../config';
import { Issue } from '../constants/demoLabels';

const BACKEND_URL = DemoConfig.BACKEND_URL;

export interface SyncAck {
    repo: string;
    receivedCount: number;
    fetchedAt: string;
    cacheVersion: number;
}

export interface ChaosResponse {
    status: string;
    score: number;
    issuesTouchedRecently: number;
    unresolvedUrgent: number;
    distinctLabelCount: number;
    afterHoursSignal: boolean;
    mysteryMeatCount: number;
    schemaVersion: number;
}

export interface FridayScoreResponse {
    score: number;
    status: string;
    chaosScore: number;
    unresolvedUrgent: number;
    afterHoursSignal: boolean;
    schemaVersion: number;
}

export interface ReshapeResponse {
    status: string;
    dayPlan: DayStructure | null;
    actionPlan: GitHubMutationPlan;
    goapActions: GoapActionSummary[];
    chaos: ChaosMetrics;
    compliance: ComplianceReport;
    stressScore: number;
    stressLevel: string;
    expectedStressScore: number;
    fridayScore: number;
    schemaVersion: number;
}

export interface DayStructure {
    deepWork: Issue | null;
    quickWins: Issue[];
    maintenance: Issue[];
    deferred: Issue[];
}

export interface GitHubMutationPlan {
    repo: string;
    actions: GitHubAction[];
}

export type GitHubAction = 
    | { type: 'AddLabels'; issueNumber: number; labels: string[] }
    | { type: 'RemoveLabels'; issueNumber: number; labels: string[] }
    | { type: 'Comment'; issueNumber: number; body: string };

export interface GoapActionSummary {
    id: string;
    name: string;
    cost: number;
}

export interface ChaosMetrics {
    issuesTouchedRecently: number;
    unresolvedUrgent: number;
    distinctLabelCount: number;
    afterHoursSignal: boolean;
    mysteryMeatCount: number;
    score: number;
}

export interface ComplianceReport {
    userId: string;
    isCompliant: boolean;
    violations: Violation[];
    bucketCounts: Record<string, number>;
    complianceScore: number;
}

export interface Violation {
    type: string;
    severity: string;
    message: string;
    recommendation: string;
    field: string;
}

export async function syncIssues(repo: string, issues: Issue[]): Promise<SyncAck> {
    const response = await fetch(`${BACKEND_URL}/api/issues/sync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            repo,
            issues,
            fetchedAt: new Date().toISOString(),
            schemaVersion: 1
        })
    });
    
    if (!response.ok) {
        throw new Error(`Sync failed: ${response.statusText}`);
    }
    
    return response.json() as Promise<SyncAck>;
}

export async function getChaosScore(repo: string): Promise<ChaosResponse> {
    const response = await fetch(`${BACKEND_URL}/api/chaos?repo=${encodeURIComponent(repo)}`);
    if (!response.ok) {
        throw new Error(`Failed to get chaos score: ${response.statusText}`);
    }
    return response.json() as Promise<ChaosResponse>;
}

export async function getFridayScore(repo: string, userId?: string): Promise<FridayScoreResponse> {
    let url = `${BACKEND_URL}/api/friday-score?repo=${encodeURIComponent(repo)}`;
    if (userId) {
        url += `&userId=${encodeURIComponent(userId)}`;
    }
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`Failed to get Friday score: ${response.statusText}`);
    }
    return response.json() as Promise<FridayScoreResponse>;
}

export async function reshape(repo: string, userId: string, dryRun: boolean): Promise<ReshapeResponse> {
    const response = await fetch(`${BACKEND_URL}/api/reshape`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ repo, userId, dryRun })
    });
    
    if (!response.ok) {
        throw new Error(`Reshape failed: ${response.statusText}`);
    }
    
    return response.json() as Promise<ReshapeResponse>;
}
