import * as vscode from 'vscode';
import { DayStructure } from '../api/backend';

let wheelPanel: vscode.WebviewPanel | undefined;

export function showWheelWebview(dayPlan: DayStructure): void {
    if (wheelPanel) {
        wheelPanel.reveal(vscode.ViewColumn.Beside);
    } else {
        wheelPanel = vscode.window.createWebviewPanel(
            'burnoutWheel',
            '3-3-3 Day Structure',
            vscode.ViewColumn.Beside,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );
        
        wheelPanel.onDidDispose(() => {
            wheelPanel = undefined;
        });
    }
    
    wheelPanel.webview.html = getWheelHtml(dayPlan);
}

function getWheelHtml(dayPlan: DayStructure): string {
    const deepWork = dayPlan.deepWork ? [dayPlan.deepWork] : [];
    const quickWins = dayPlan.quickWins || [];
    const maintenance = dayPlan.maintenance || [];
    const deferred = dayPlan.deferred || [];
    
    const data = [
        { label: 'Deep Work', count: deepWork.length, color: '#d73a4a', items: deepWork },
        { label: 'Quick Wins', count: quickWins.length, color: '#0e8a16', items: quickWins },
        { label: 'Maintenance', count: maintenance.length, color: '#c5def5', items: maintenance },
    ];

    return `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: var(--vscode-editor-background);
            color: var(--vscode-editor-foreground);
            padding: 20px;
            margin: 0;
        }
        h1 {
            font-size: 1.5em;
            margin-bottom: 20px;
            text-align: center;
        }
        .wheel-container {
            display: flex;
            justify-content: center;
            margin-bottom: 30px;
        }
        .donut {
            width: 200px;
            height: 200px;
        }
        .legend {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .legend-color {
            width: 20px;
            height: 20px;
            border-radius: 4px;
        }
        .bucket {
            margin-top: 20px;
            padding: 15px;
            border-radius: 8px;
            background: var(--vscode-editor-inactiveSelectionBackground);
        }
        .bucket h2 {
            margin: 0 0 10px 0;
            font-size: 1.1em;
        }
        .issue {
            padding: 8px 12px;
            margin: 5px 0;
            background: var(--vscode-editor-background);
            border-radius: 4px;
            border-left: 4px solid;
        }
        .issue-deep { border-color: #d73a4a; }
        .issue-quick { border-color: #0e8a16; }
        .issue-maint { border-color: #1d76db; }
        .deferred-section {
            margin-top: 20px;
            padding: 10px;
            opacity: 0.7;
        }
        .deferred-section h3 {
            font-size: 0.9em;
            margin-bottom: 10px;
        }
        svg text {
            fill: var(--vscode-editor-foreground);
        }
    </style>
</head>
<body>
    <h1>🎯 Today's 3-3-3 Plan</h1>
    
    <div class="wheel-container">
        <svg class="donut" viewBox="0 0 100 100">
            ${generateDonutChart(data)}
        </svg>
    </div>
    
    <div class="legend">
        ${data.map(d => `
            <div class="legend-item">
                <div class="legend-color" style="background: ${d.color}"></div>
                <span>${d.label}: ${d.count}</span>
            </div>
        `).join('')}
    </div>
    
    <div class="bucket">
        <h2>🎯 Deep Work (${deepWork.length}/1)</h2>
        ${deepWork.map(i => `<div class="issue issue-deep">#${i.number}: ${i.title}</div>`).join('') || '<div class="issue">No deep work assigned</div>'}
    </div>
    
    <div class="bucket">
        <h2>⚡ Quick Wins (${quickWins.length}/3)</h2>
        ${quickWins.map(i => `<div class="issue issue-quick">#${i.number}: ${i.title}</div>`).join('') || '<div class="issue">No quick wins</div>'}
    </div>
    
    <div class="bucket">
        <h2>🔧 Maintenance (${maintenance.length}/3)</h2>
        ${maintenance.map(i => `<div class="issue issue-maint">#${i.number}: ${i.title}</div>`).join('') || '<div class="issue">No maintenance tasks</div>'}
    </div>
    
    ${deferred.length > 0 ? `
        <div class="deferred-section">
            <h3>📦 Deferred (${deferred.length} items protected for later)</h3>
        </div>
    ` : ''}
</body>
</html>`;
}

function generateDonutChart(data: { label: string; count: number; color: string }[]): string {
    const total = data.reduce((sum, d) => sum + Math.max(1, d.count), 0);
    let currentAngle = 0;
    const cx = 50, cy = 50, r = 35;
    
    const paths = data.map(d => {
        const value = Math.max(1, d.count);
        const angle = (value / total) * 360;
        const startAngle = currentAngle;
        const endAngle = currentAngle + angle;
        currentAngle = endAngle;
        
        const start = polarToCartesian(cx, cy, r, startAngle);
        const end = polarToCartesian(cx, cy, r, endAngle);
        const largeArc = angle > 180 ? 1 : 0;
        
        return `<path d="M ${cx} ${cy} L ${start.x} ${start.y} A ${r} ${r} 0 ${largeArc} 1 ${end.x} ${end.y} Z" fill="${d.color}" />`;
    });
    
    // Inner circle for donut effect
    paths.push(`<circle cx="${cx}" cy="${cy}" r="20" fill="var(--vscode-editor-background)" />`);
    paths.push(`<text x="${cx}" y="${cy + 5}" text-anchor="middle" font-size="12">3-3-3</text>`);
    
    return paths.join('\n');
}

function polarToCartesian(cx: number, cy: number, r: number, angleInDegrees: number): { x: number; y: number } {
    const angleInRadians = (angleInDegrees - 90) * Math.PI / 180;
    return {
        x: cx + r * Math.cos(angleInRadians),
        y: cy + r * Math.sin(angleInRadians)
    };
}
