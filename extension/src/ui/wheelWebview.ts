import * as vscode from 'vscode';
import { DayStructure } from '../api/backend';
import { Issue } from '../constants/demoLabels';

let wheelPanel: vscode.WebviewPanel | undefined;

export function showWheelWebview(dayPlan: DayStructure): void {
    if (wheelPanel) {
        wheelPanel.reveal(vscode.ViewColumn.Beside);
        wheelPanel.webview.postMessage({ type: 'updateData', data: dayPlan });
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
        
        wheelPanel.webview.html = getWheelHtml(dayPlan);
    }
}

function escapeHtml(text: string): string {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function getWheelHtml(dayPlan: DayStructure): string {
    const deepWork = dayPlan.deepWork ? [dayPlan.deepWork] : [];
    const quickWins = dayPlan.quickWins || [];
    const maintenance = dayPlan.maintenance || [];
    const deferred = dayPlan.deferred || [];

    return `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- D3.js Library -->
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        * {
            box-sizing: border-box;
        }
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
            align-items: center;
            margin-bottom: 30px;
            position: relative;
        }
        #donut-chart {
            width: 280px;
            height: 280px;
        }
        /* D3 arc transitions */
        .arc path {
            transition: transform 0.2s ease-out, filter 0.2s ease-out;
            cursor: pointer;
        }
        .arc:hover path {
            filter: brightness(1.2) drop-shadow(0 0 8px rgba(255,255,255,0.3));
        }
        /* Tooltip */
        .tooltip {
            position: fixed;
            background: var(--vscode-editorWidget-background, #252526);
            border: 1px solid var(--vscode-editorWidget-border, #454545);
            border-radius: 6px;
            padding: 12px;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.3s ease;
            z-index: 1000;
            max-width: 300px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        }
        .tooltip.visible {
            opacity: 1;
        }
        .tooltip-title {
            font-weight: 600;
            margin-bottom: 8px;
            font-size: 14px;
        }
        .tooltip-item {
            font-size: 12px;
            padding: 4px 0;
            border-bottom: 1px solid var(--vscode-editorWidget-border, #333);
        }
        .tooltip-item:last-child {
            border-bottom: none;
        }
        .legend {
            display: flex;
            justify-content: center;
            gap: 20px;
            margin-bottom: 20px;
            flex-wrap: wrap;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            padding: 6px 12px;
            border-radius: 4px;
            transition: background 0.3s;
        }
        .legend-item:hover {
            background: var(--vscode-list-hoverBackground);
        }
        .legend-color {
            width: 16px;
            height: 16px;
            border-radius: 4px;
        }
        .bucket {
            margin-top: 20px;
            padding: 15px;
            border-radius: 8px;
            background: var(--vscode-editor-inactiveSelectionBackground);
            transition: all 0.3s ease;
            opacity: 0;
            transform: translateY(10px);
        }
        .bucket.visible {
            opacity: 1;
            transform: translateY(0);
        }
        .bucket.highlight {
            transform: scale(1.02);
            box-shadow: 0 0 20px rgba(255,255,255,0.1);
        }
        .bucket h2 {
            margin: 0 0 12px 0;
            font-size: 1.1em;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .bucket-count {
            font-size: 0.85em;
            opacity: 0.7;
        }
        .issue {
            padding: 10px 14px;
            margin: 6px 0;
            background: var(--vscode-editor-background);
            border-radius: 6px;
            border-left: 4px solid;
            cursor: pointer;
            transition: all 0.3s ease;
            opacity: 0;
            transform: translateX(-20px);
        }
        .issue.visible {
            opacity: 1;
            transform: translateX(0);
        }
        .issue:hover {
            background: var(--vscode-list-hoverBackground);
            transform: translateX(4px);
        }
        .issue-deep { border-color: #d73a4a; }
        .issue-quick { border-color: #0e8a16; }
        .issue-maint { border-color: #1d76db; }
        .issue-number {
            font-weight: 600;
            margin-right: 8px;
            opacity: 0.7;
        }
        .deferred-section {
            margin-top: 20px;
            padding: 12px;
            opacity: 0.7;
            border-radius: 8px;
            background: var(--vscode-editor-inactiveSelectionBackground);
        }
        .deferred-section h3 {
            font-size: 0.95em;
            margin-bottom: 12px;
        }
        .center-text {
            font-size: 14px;
            font-weight: 600;
            fill: var(--vscode-editor-foreground);
        }
        .center-subtext {
            font-size: 10px;
            fill: var(--vscode-descriptionForeground);
        }
    </style>
</head>
<body>
    <h1>🎯 Today's 3-3-3 Plan</h1>
    
    <div class="wheel-container">
        <div id="donut-chart"></div>
        <div class="tooltip" id="tooltip"></div>
    </div>
    
    <div class="legend" id="legend"></div>
    
    <div id="buckets-container">
        <div class="bucket" id="bucket-deep" data-category="deep">
            <h2>🎯 Deep Work <span class="bucket-count">(${deepWork.length}/1)</span></h2>
            <div class="issues-list">
                ${deepWork.map((i, idx) => `
                    <div class="issue issue-deep" data-delay="${idx}">
                        <span class="issue-number">#${i.number}</span>${escapeHtml(i.title)}
                    </div>
                `).join('') || '<div class="issue issue-deep visible">No deep work assigned</div>'}
            </div>
        </div>
        
        <div class="bucket" id="bucket-quick" data-category="quick">
            <h2>⚡ Quick Wins <span class="bucket-count">(${quickWins.length}/3)</span></h2>
            <div class="issues-list">
                ${quickWins.map((i, idx) => `
                    <div class="issue issue-quick" data-delay="${idx}">
                        <span class="issue-number">#${i.number}</span>${escapeHtml(i.title)}
                    </div>
                `).join('') || '<div class="issue issue-quick visible">No quick wins</div>'}
            </div>
        </div>
        
        <div class="bucket" id="bucket-maint" data-category="maintenance">
            <h2>🔧 Maintenance <span class="bucket-count">(${maintenance.length}/3)</span></h2>
            <div class="issues-list">
                ${maintenance.map((i, idx) => `
                    <div class="issue issue-maint" data-delay="${idx}">
                        <span class="issue-number">#${i.number}</span>${escapeHtml(i.title)}
                    </div>
                `).join('') || '<div class="issue issue-maint visible">No maintenance tasks</div>'}
            </div>
        </div>
        
        ${deferred.length > 0 ? `
            <div class="deferred-section">
                <h3>📦 Deferred (${deferred.length} items protected for later)</h3>
                <div class="issues-list">
                    ${deferred.slice(0, 5).map((i, idx) => `
                        <div class="issue" style="border-color: #666" data-delay="${idx}">
                            <span class="issue-number">#${i.number}</span>${escapeHtml(i.title)}
                        </div>
                    `).join('')}
                    ${deferred.length > 5 ? `<div class="issue visible" style="border-color: #666; opacity: 0.6">...and ${deferred.length - 5} more</div>` : ''}
                </div>
            </div>
        ` : ''}
    </div>

    <script>
        // Chart data
        const chartData = {
            deepWork: ${JSON.stringify(deepWork.map(i => ({ number: i.number, title: i.title })))},
            quickWins: ${JSON.stringify(quickWins.map(i => ({ number: i.number, title: i.title })))},
            maintenance: ${JSON.stringify(maintenance.map(i => ({ number: i.number, title: i.title })))},
            deferred: ${JSON.stringify(deferred.map(i => ({ number: i.number, title: i.title })))}
        };

        const categories = [
            { key: 'deep', label: 'Deep Work', color: '#d73a4a', items: chartData.deepWork, max: 1, icon: '🎯' },
            { key: 'quick', label: 'Quick Wins', color: '#0e8a16', items: chartData.quickWins, max: 3, icon: '⚡' },
            { key: 'maint', label: 'Maintenance', color: '#1d76db', items: chartData.maintenance, max: 3, icon: '🔧' }
        ];

        // D3.js Donut Chart Setup
        const width = 280;
        const height = 280;
        const radius = Math.min(width, height) / 2;
        const innerRadius = radius * 0.55;

        const svg = d3.select('#donut-chart')
            .append('svg')
            .attr('width', width)
            .attr('height', height)
            .append('g')
            .attr('transform', 'translate(' + (width / 2) + ',' + (height / 2) + ')');

        const tooltip = document.getElementById('tooltip');

        // Pie generator - ensure at least 1 for each segment to show all buckets
        const pie = d3.pie()
            .value(function(d) { return Math.max(1, d.items.length); })
            .sort(null)
            .padAngle(0.02);

        // Arc generators
        const arc = d3.arc()
            .innerRadius(innerRadius)
            .outerRadius(radius - 10);

        const arcHover = d3.arc()
            .innerRadius(innerRadius)
            .outerRadius(radius);

        // Create arc groups
        const arcs = svg.selectAll('.arc')
            .data(pie(categories))
            .enter()
            .append('g')
            .attr('class', 'arc');

        // Add paths with 800ms entry animation
        arcs.append('path')
            .attr('fill', function(d) { return d.data.color; })
            .style('opacity', 0)
            .transition()
            .duration(800)
            .ease(d3.easeCubicOut)
            .style('opacity', 1)
            .attrTween('d', function(d) {
                const interpolate = d3.interpolate({ startAngle: 0, endAngle: 0 }, d);
                return function(t) {
                    return arc(interpolate(t));
                };
            });

        // Add hover interactions after entry animation completes
        setTimeout(function() {
            arcs.selectAll('path')
                .on('mouseenter', function(event, d) {
                    // Scale up arc
                    d3.select(this)
                        .transition()
                        .duration(200)
                        .attr('d', arcHover);
                    
                    // Build tooltip content
                    const items = d.data.items;
                    let content = '<div class="tooltip-title">' + d.data.icon + ' ' + d.data.label + ' (' + items.length + '/' + d.data.max + ')</div>';
                    if (items.length > 0) {
                        items.slice(0, 5).forEach(function(i) {
                            content += '<div class="tooltip-item">#' + i.number + ': ' + i.title + '</div>';
                        });
                        if (items.length > 5) {
                            content += '<div class="tooltip-item">...and ' + (items.length - 5) + ' more</div>';
                        }
                    } else {
                        content += '<div class="tooltip-item">No items</div>';
                    }
                    
                    tooltip.innerHTML = content;
                    tooltip.classList.add('visible');
                    tooltip.style.left = (event.clientX + 15) + 'px';
                    tooltip.style.top = (event.clientY - 10) + 'px';
                    
                    // Highlight bucket
                    const bucket = document.getElementById('bucket-' + d.data.key);
                    if (bucket) bucket.classList.add('highlight');
                })
                .on('mousemove', function(event) {
                    tooltip.style.left = (event.clientX + 15) + 'px';
                    tooltip.style.top = (event.clientY - 10) + 'px';
                })
                .on('mouseleave', function(event, d) {
                    d3.select(this)
                        .transition()
                        .duration(200)
                        .attr('d', arc);
                    
                    tooltip.classList.remove('visible');
                    const bucket = document.getElementById('bucket-' + d.data.key);
                    if (bucket) bucket.classList.remove('highlight');
                });
        }, 850);

        // Center text
        svg.append('text')
            .attr('class', 'center-text')
            .attr('text-anchor', 'middle')
            .attr('dy', '-0.2em')
            .text('3-3-3');

        svg.append('text')
            .attr('class', 'center-subtext')
            .attr('text-anchor', 'middle')
            .attr('dy', '1.2em')
            .text('Focus Plan');

        // Build legend with hover interactions
        const legend = d3.select('#legend');
        categories.forEach(function(cat) {
            const item = legend.append('div')
                .attr('class', 'legend-item')
                .on('mouseenter', function() {
                    const bucket = document.getElementById('bucket-' + cat.key);
                    if (bucket) bucket.classList.add('highlight');
                })
                .on('mouseleave', function() {
                    const bucket = document.getElementById('bucket-' + cat.key);
                    if (bucket) bucket.classList.remove('highlight');
                });
            
            item.append('div')
                .attr('class', 'legend-color')
                .style('background', cat.color);
            
            item.append('span')
                .text(cat.label + ': ' + cat.items.length + '/' + cat.max);
        });

        // Animate buckets and issues in with staggered delays
        setTimeout(function() {
            document.querySelectorAll('.bucket').forEach(function(bucket, idx) {
                setTimeout(function() {
                    bucket.classList.add('visible');
                }, idx * 150);
            });
        }, 300);

        setTimeout(function() {
            document.querySelectorAll('.issue').forEach(function(issue, idx) {
                setTimeout(function() {
                    issue.classList.add('visible');
                }, idx * 80);
            });
        }, 600);

        // Handle data updates from VS Code
        window.addEventListener('message', function(event) {
            const message = event.data;
            if (message.type === 'updateData') {
                location.reload();
            }
        });
    </script>
</body>
</html>`;
}
