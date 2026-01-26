import * as vscode from 'vscode';
import { gh, fetchIssues } from '../api/ghCli';
import { DemoConfig } from '../config';

const REQUIRED_LABELS = [
    'demo:stale-14d', 'demo:after-hours', 'demo:touched-today', 'demo:friday',
    'deep-work', 'quick-win', 'maintenance',
    'urgent', 'priority:critical',
    'focus', 'deferred'
];

export async function preflightCheck(): Promise<void> {
    const outputChannel = vscode.window.createOutputChannel('Burnout Preflight');
    outputChannel.show();
    
    const checks: { name: string; check: () => Promise<boolean> }[] = [
        { 
            name: "gh CLI installed", 
            check: async () => {
                try { 
                    await gh(["--version"]); 
                    return true; 
                } catch { 
                    return false; 
                }
            }
        },
        { 
            name: "gh authenticated", 
            check: async () => {
                try { 
                    await gh(["api", "user", "-q", ".login"]); 
                    return true; 
                } catch { 
                    return false; 
                }
            }
        },
        { 
            name: "Repo exists", 
            check: async () => {
                try { 
                    await gh(["repo", "view", DemoConfig.REPO, "--json", "name", "--jq", ".name"]); 
                    return true; 
                } catch { 
                    return false; 
                }
            }
        },
        { 
            name: "Backend reachable", 
            check: async () => {
                try { 
                    const res = await fetch(`${DemoConfig.BACKEND_URL}/actuator/health`);
                    return res.ok; 
                } catch { 
                    return false; 
                }
            }
        },
        { 
            name: "Issues seeded (≥10)", 
            check: async () => {
                try {
                    const issues = await fetchIssues(DemoConfig.REPO);
                    return issues.length >= 10;
                } catch {
                    return false;
                }
            }
        },
        { 
            name: "Required labels exist", 
            check: async () => {
                try {
                    const stdout = await gh(['label', 'list', '-R', DemoConfig.REPO, '--json', 'name']);
                    const existingLabels: string[] = JSON.parse(stdout).map((l: {name: string}) => l.name);
                    const missing = REQUIRED_LABELS.filter(l => !existingLabels.includes(l));
                    
                    if (missing.length > 0) {
                        outputChannel.appendLine(`  Missing labels: ${missing.join(', ')}`);
                        return false;
                    }
                    return true;
                } catch {
                    return false;
                }
            }
        },
    ];

    let allPassed = true;
    outputChannel.appendLine('=== Burnout Extension Preflight Check ===\n');
    
    for (const { name, check } of checks) {
        const passed = await check();
        const icon = passed ? "✅" : "❌";
        outputChannel.appendLine(`${icon} ${name}`);
        if (!passed) allPassed = false;
    }

    outputChannel.appendLine('');
    
    if (allPassed) {
        outputChannel.appendLine('✅ All systems ready!');
        vscode.window.showInformationMessage("✅ Preflight: All systems ready!");
    } else {
        outputChannel.appendLine('❌ Some checks failed. See details above.');
        vscode.window.showErrorMessage("❌ Preflight: Some checks failed. See output.");
    }
}
