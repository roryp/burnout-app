import * as vscode from 'vscode';
import { DemoConfig } from '../config';
import { fetchIssues } from '../api/ghCli';
import { syncIssues, getChaosScore, getFridayScore } from '../api/backend';
import { isDemoModeEnabled } from '../commands/demoMode';
import { setPollingInterval, getPollingInterval } from '../commands/reshape';

let chaosStatusBar: vscode.StatusBarItem;
let fridayStatusBar: vscode.StatusBarItem;

export function createStatusBars(context: vscode.ExtensionContext): void {
    // Chaos status bar
    chaosStatusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    chaosStatusBar.text = "$(sync) Chaos: --";
    chaosStatusBar.tooltip = "Chaos score (click to refresh)";
    chaosStatusBar.command = "burnout.refreshChaos";
    chaosStatusBar.show();
    context.subscriptions.push(chaosStatusBar);
    
    // Friday status bar
    fridayStatusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 99);
    fridayStatusBar.text = "$(rocket) Friday: --";
    fridayStatusBar.tooltip = "Friday deploy readiness";
    fridayStatusBar.show();
    context.subscriptions.push(fridayStatusBar);
    
    // Start polling
    startPolling();
}

function startPolling(): void {
    // Initial update
    updateChaosStatus();
    
    // Poll every 10 seconds (unless demo mode)
    const interval = setInterval(() => {
        if (!isDemoModeEnabled()) {
            updateChaosStatus();
        }
    }, 10000);
    
    setPollingInterval(interval);
}

export async function updateChaosStatus(): Promise<void> {
    try {
        const issues = await fetchIssues(DemoConfig.REPO);
        await syncIssues(DemoConfig.REPO, issues);
        
        const chaos = await getChaosScore(DemoConfig.REPO);
        
        if (chaos.status === "not_synced") {
            chaosStatusBar.text = "$(sync) Syncing...";
            return;
        }
        
        chaosStatusBar.text = `$(alert) Chaos: ${chaos.score}/10`;
        
        if (chaos.score > 7) {
            chaosStatusBar.backgroundColor = new vscode.ThemeColor("statusBarItem.errorBackground");
        } else if (chaos.score > 4) {
            chaosStatusBar.backgroundColor = new vscode.ThemeColor("statusBarItem.warningBackground");
        } else {
            chaosStatusBar.backgroundColor = undefined;
        }
        
        // Update Friday score too
        const friday = await getFridayScore(DemoConfig.REPO, DemoConfig.USER_ID);
        
        if (friday.score >= 80) {
            fridayStatusBar.text = `$(rocket) Friday: ${friday.score}%`;
            fridayStatusBar.backgroundColor = undefined;
        } else if (friday.score >= 50) {
            fridayStatusBar.text = `$(warning) Friday: ${friday.score}%`;
            fridayStatusBar.backgroundColor = new vscode.ThemeColor("statusBarItem.warningBackground");
        } else {
            fridayStatusBar.text = `$(error) Friday: ${friday.score}%`;
            fridayStatusBar.backgroundColor = new vscode.ThemeColor("statusBarItem.errorBackground");
        }
        
    } catch (error) {
        console.error('Failed to update chaos status:', error);
        chaosStatusBar.text = "$(error) Chaos: Error";
    }
}
