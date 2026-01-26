import * as vscode from 'vscode';
import { DemoConfig } from '../config';
import { fetchIssues } from '../api/ghCli';
import { syncIssues, reshape, ReshapeResponse, GitHubMutationPlan } from '../api/backend';
import { executeMutationPlan } from '../api/mutationExecutor';
import { validateSchemaVersion } from '../api/schemaValidation';
import { showWheelWebview } from '../ui/wheelWebview';

let pendingPlan: GitHubMutationPlan | null = null;
let chaosPollingInterval: NodeJS.Timeout | null = null;

export async function reshapePreview(): Promise<void> {
    const statusBar = vscode.window.setStatusBarMessage('$(loading~spin) Reshaping day...');
    
    try {
        // Fetch and sync issues first
        const issues = await fetchIssues(DemoConfig.REPO);
        await syncIssues(DemoConfig.REPO, issues);
        
        // Call reshape with dryRun=true
        const rawData = await reshape(DemoConfig.REPO, DemoConfig.USER_ID, true);
        
        const data = validateSchemaVersion(rawData, 'ReshapeResponse');
        
        if (data.status === 'not_synced') {
            vscode.window.showWarningMessage('Issues not synced yet. Please wait and try again.');
            return;
        }
        
        // Show wheel visualization
        if (data.dayPlan) {
            showWheelWebview(data.dayPlan);
        }
        
        // Store pending plan for apply
        pendingPlan = data.actionPlan;
        await vscode.commands.executeCommand("setContext", "burnout.hasPendingPlan", true);
        
        const actionCount = data.actionPlan.actions.length;
        vscode.window.showInformationMessage(
            `📋 Preview: ${actionCount} actions planned. Stress: ${data.stressScore} → ${data.expectedStressScore}. Click "Apply" to execute.`
        );
        
    } catch (error) {
        vscode.window.showErrorMessage(`Reshape failed: ${error instanceof Error ? error.message : String(error)}`);
    } finally {
        statusBar.dispose();
    }
}

export async function applyPlan(): Promise<void> {
    if (!pendingPlan || pendingPlan.actions.length === 0) {
        vscode.window.showWarningMessage("No pending plan. Run 'Reshape My Day (Preview)' first.");
        return;
    }
    
    const statusBar = vscode.window.setStatusBarMessage('$(loading~spin) Applying plan...');
    
    // Pause polling during mutations
    const wasPolling = chaosPollingInterval !== null;
    if (wasPolling && chaosPollingInterval) {
        clearInterval(chaosPollingInterval);
        chaosPollingInterval = null;
    }
    
    try {
        await executeMutationPlan(pendingPlan);
        
        // Resync after mutations
        const issues = await fetchIssues(DemoConfig.REPO);
        await syncIssues(DemoConfig.REPO, issues);
        
        vscode.window.showInformationMessage(`✅ Applied ${pendingPlan.actions.length} actions. Day reshaped!`);
    } catch (error) {
        vscode.window.showErrorMessage(`Apply failed: ${error instanceof Error ? error.message : String(error)}`);
    } finally {
        pendingPlan = null;
        await vscode.commands.executeCommand("setContext", "burnout.hasPendingPlan", false);
        statusBar.dispose();
    }
}

export function setPollingInterval(interval: NodeJS.Timeout | null): void {
    chaosPollingInterval = interval;
}

export function getPollingInterval(): NodeJS.Timeout | null {
    return chaosPollingInterval;
}
