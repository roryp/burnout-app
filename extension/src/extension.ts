import * as vscode from 'vscode';
import { preflightCheck } from './commands/preflight';
import { reshapePreview, applyPlan } from './commands/reshape';
import { toggleDemoMode } from './commands/demoMode';
import { createStatusBars, updateChaosStatus } from './ui/statusBar';
import { showWheelWebview } from './ui/wheelWebview';
import { DemoConfig } from './config';
import { fetchIssues } from './api/ghCli';
import { syncIssues, reshape } from './api/backend';

export function activate(context: vscode.ExtensionContext) {
    console.log('Burnout Prevention extension activated');
    
    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('burnout.preflight', preflightCheck),
        vscode.commands.registerCommand('burnout.reshapePreview', reshapePreview),
        vscode.commands.registerCommand('burnout.applyPlan', applyPlan),
        vscode.commands.registerCommand('burnout.toggleDemoMode', toggleDemoMode),
        vscode.commands.registerCommand('burnout.refreshChaos', updateChaosStatus),
        vscode.commands.registerCommand('burnout.showWheel', async () => {
            try {
                const issues = await fetchIssues(DemoConfig.REPO);
                await syncIssues(DemoConfig.REPO, issues);
                const result = await reshape(DemoConfig.REPO, DemoConfig.USER_ID, true);
                if (result.dayPlan) {
                    showWheelWebview(result.dayPlan);
                }
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to show wheel: ${error instanceof Error ? error.message : String(error)}`);
            }
        })
    );
    
    // Initialize status bars
    createStatusBars(context);
    
    // Set initial context
    vscode.commands.executeCommand("setContext", "burnout.hasPendingPlan", false);
    vscode.commands.executeCommand("setContext", "burnout.demoMode", false);
}

export function deactivate() {
    console.log('Burnout Prevention extension deactivated');
}
