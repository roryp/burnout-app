import * as vscode from 'vscode';
import { DemoConfig } from '../config';
import { setPollingInterval, getPollingInterval } from './reshape';

let demoModeEnabled = false;
let demoRefreshButton: vscode.StatusBarItem | undefined;

export function toggleDemoMode(): void {
    demoModeEnabled = !demoModeEnabled;
    
    if (demoModeEnabled) {
        // Stop polling
        const interval = getPollingInterval();
        if (interval) {
            clearInterval(interval);
            setPollingInterval(null);
        }
        
        // Show manual refresh indicator
        vscode.commands.executeCommand("setContext", "burnout.demoMode", true);
        
        vscode.window.showInformationMessage("🎭 Demo Mode ON: Polling stopped, using manual refresh");
    } else {
        // Resume normal operation (polling will be restarted by status bar)
        vscode.commands.executeCommand("setContext", "burnout.demoMode", false);
        
        vscode.window.showInformationMessage("Demo Mode OFF: Resuming normal operation");
    }
}

export function isDemoModeEnabled(): boolean {
    return demoModeEnabled;
}
