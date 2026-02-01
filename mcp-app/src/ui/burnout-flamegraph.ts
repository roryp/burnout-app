// Burnout Flamegraph UI - Horizontal stacked bars like a real flamegraph
// Each issue is a bar, mouseover shows stress, click opens in VS Code GitHub extension

export function generateFlamegraphUI(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="dark light">
  <title>Burnout Flamegraph</title>
  <style>
    :root {
      --bg: #0d0d0d;
      --bg-gradient: linear-gradient(135deg, #0d0d0d 0%, #1a1a2e 50%, #16213e 100%);
      --text: #ffffff;
      --text-muted: #94a3b8;
      --card-bg: rgba(255,255,255,0.05);
      --card-border: rgba(255,255,255,0.1);
      
      --deep-work: linear-gradient(90deg, #00f260, #0575e6);
      --quick-wins: linear-gradient(90deg, #f857a6, #ff5858);
      --maintenance: linear-gradient(90deg, #f7971e, #ffd200);
      --deferred: linear-gradient(90deg, #8e2de2, #4a00e0);
      
      --stress-low: #00f260;
      --stress-moderate: #f7971e;
      --stress-high: #ff5858;
    }
    
    @media (prefers-color-scheme: light) {
      :root {
        --bg: #f8fafc;
        --bg-gradient: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #cbd5e1 100%);
        --text: #0f172a;
        --text-muted: #64748b;
        --card-bg: rgba(255,255,255,0.8);
        --card-border: rgba(0,0,0,0.1);
      }
    }
    
    * { box-sizing: border-box; margin: 0; padding: 0; }
    
    body {
      font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: var(--bg-gradient);
      color: var(--text);
      padding: 24px;
      line-height: 1.6;
      min-height: 100vh;
    }
    
    .container { max-width: 1000px; margin: 0 auto; }
    
    .header {
      text-align: center;
      margin-bottom: 32px;
    }
    
    .header h1 {
      font-size: 2rem;
      font-weight: 700;
      margin-bottom: 8px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f857a6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    
    .header .repo {
      color: var(--text-muted);
      font-size: 0.95rem;
      font-weight: 500;
    }
    
    .metrics {
      display: flex;
      gap: 20px;
      justify-content: center;
      margin-bottom: 32px;
      flex-wrap: wrap;
    }
    
    .metric {
      background: var(--card-bg);
      backdrop-filter: blur(20px);
      padding: 16px 24px;
      border-radius: 12px;
      text-align: center;
      min-width: 120px;
      border: 1px solid var(--card-border);
    }
    
    .metric .value {
      font-size: 1.8rem;
      font-weight: 700;
    }
    
    .metric .label {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    
    .metric.stress .value { color: var(--stress-moderate); }
    .metric.friday .value { color: var(--stress-low); }
    
    /* Flamegraph styles */
    .flamegraph {
      background: var(--card-bg);
      backdrop-filter: blur(20px);
      border-radius: 16px;
      padding: 24px;
      border: 1px solid var(--card-border);
    }
    
    .flame-section {
      margin-bottom: 24px;
    }
    
    .flame-section:last-child { margin-bottom: 0; }
    
    .flame-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      font-weight: 600;
      font-size: 0.9rem;
    }
    
    .flame-header .dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }
    
    .flame-header.deep-work .dot { background: var(--deep-work); }
    .flame-header.quick-wins .dot { background: var(--quick-wins); }
    .flame-header.maintenance .dot { background: var(--maintenance); }
    .flame-header.deferred .dot { background: var(--deferred); }
    
    .flame-stack {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .flame-bar {
      height: 44px;
      border-radius: 8px;
      cursor: pointer;
      display: flex;
      align-items: center;
      padding: 0 16px;
      font-size: 1rem;
      font-weight: 600;
      color: #fff;
      position: relative;
      transition: all 0.2s ease;
      text-shadow: 0 2px 4px rgba(0,0,0,0.5), 0 1px 2px rgba(0,0,0,0.3);
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      text-decoration: none;
    }
    
    .flame-bar:hover {
      transform: translateX(4px);
      filter: brightness(1.15);
      box-shadow: 0 4px 20px rgba(0,0,0,0.3);
      color: #fff;
    }
    
    .flame-bar:active {
      transform: scale(0.98);
      opacity: 0.8;
    }
    
    .flame-bar.deep-work { background: var(--deep-work); }
    .flame-bar.quick-wins { background: var(--quick-wins); }
    .flame-bar.maintenance { background: var(--maintenance); }
    .flame-bar.deferred { background: var(--deferred); }
    
    .flame-bar .issue-num {
      opacity: 1;
      margin-right: 10px;
      font-family: 'SF Mono', monospace;
      font-weight: 700;
      background: rgba(0,0,0,0.25);
      padding: 2px 6px;
      border-radius: 4px;
    }
    
    .flame-bar .stress-indicator {
      margin-left: auto;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 0.85rem;
      font-weight: 700;
      background: rgba(0,0,0,0.4);
    }
    
    .flame-bar .stress-indicator.low { color: var(--stress-low); }
    .flame-bar .stress-indicator.moderate { color: var(--stress-moderate); }
    .flame-bar .stress-indicator.high { color: var(--stress-high); }
    
    .empty-message {
      color: var(--text-muted);
      font-style: italic;
      padding: 8px 0;
    }
    
    /* Tooltip */
    .tooltip {
      position: fixed;
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(10px);
      color: #fff;
      padding: 12px 16px;
      border-radius: 10px;
      font-size: 0.85rem;
      pointer-events: none;
      z-index: 1000;
      display: none;
      max-width: 320px;
      box-shadow: 0 20px 40px rgba(0,0,0,0.4);
      border: 1px solid rgba(255,255,255,0.1);
    }
    
    .tooltip.visible { display: block; }
    
    .tooltip .title { font-weight: 600; margin-bottom: 6px; }
    .tooltip .stress-row { 
      display: flex; 
      align-items: center; 
      gap: 8px;
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid rgba(255,255,255,0.1);
    }
    .tooltip .stress-bar {
      flex: 1;
      height: 6px;
      background: rgba(255,255,255,0.2);
      border-radius: 3px;
      overflow: hidden;
    }
    .tooltip .stress-fill {
      height: 100%;
      border-radius: 3px;
      transition: width 0.3s ease;
    }
    .tooltip .stress-fill.low { background: var(--stress-low); }
    .tooltip .stress-fill.moderate { background: var(--stress-moderate); }
    .tooltip .stress-fill.high { background: var(--stress-high); }
    
    .tooltip .click-hint {
      margin-top: 8px;
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    
    .loading {
      text-align: center;
      padding: 80px 20px;
      color: var(--text-muted);
    }
    
    .loading .spinner {
      width: 48px;
      height: 48px;
      border: 3px solid rgba(255,255,255,0.1);
      border-top-color: #667eea;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 16px;
    }
    
    @keyframes spin { to { transform: rotate(360deg); } }
  </style>
</head>
<body>
  <div class="container">
    <div id="loading" class="loading">
      <div class="spinner"></div>
      <div>Loading...</div>
    </div>
    
    <div id="content" style="display: none;">
      <div class="header">
        <h1>Burnout Flamegraph</h1>
        <div class="repo" id="repo-name">-</div>
      </div>
      
      <div class="metrics">
        <div class="metric stress">
          <div class="value" id="stress-score">-</div>
          <div class="label">Stress Score</div>
        </div>
        <div class="metric friday">
          <div class="value" id="friday-score">-</div>
          <div class="label">Friday Score</div>
        </div>
      </div>
      
      <div class="flamegraph" id="flamegraph"></div>
    </div>
  </div>
  
  <div class="tooltip" id="tooltip"></div>
  
  <script>
    let wheelData = null;
    
    const BUCKETS = [
      { key: 'deepWork', name: 'Deep Work', cssClass: 'deep-work', baseStress: 60 },
      { key: 'quickWins', name: 'Quick Wins', cssClass: 'quick-wins', baseStress: 20 },
      { key: 'maintenance', name: 'Maintenance', cssClass: 'maintenance', baseStress: 30 },
      { key: 'deferred', name: 'Deferred', cssClass: 'deferred', baseStress: 10 },
    ];
    
    // Calculate stress for an issue based on various factors
    function calculateIssueStress(issue, bucketKey, globalStress) {
      const bucket = BUCKETS.find(b => b.key === bucketKey);
      let stress = bucket ? bucket.baseStress : 30;
      
      // Add complexity factor
      if (issue.complexity) {
        stress += issue.complexity * 3;
      }
      
      // Add global stress influence
      stress += (globalStress || 30) * 0.3;
      
      // Check for stress signals in labels
      if (issue.labels) {
        issue.labels.forEach(label => {
          const name = typeof label === 'string' ? label : label.name;
          if (name && (name.includes('urgent') || name.includes('critical') || name.includes('blocker'))) {
            stress += 20;
          }
          if (name && name.includes('bug')) {
            stress += 10;
          }
        });
      }
      
      // Cap at 100
      return Math.min(100, Math.round(stress));
    }
    
    function getStressLevel(stress) {
      if (stress < 35) return 'low';
      if (stress < 65) return 'moderate';
      return 'high';
    }
    
    function getStressLabel(stress) {
      if (stress < 35) return 'Low';
      if (stress < 65) return 'Moderate';
      return 'High';
    }
    
    // Open issue - uses MCP ui/open-link for VS Code context, fallback for browser
    function openIssueLink(url) {
      // In MCP app context, use ui/open-link to open URLs (per MCP Apps spec)
      if (window.parent && window.parent !== window) {
        window.parent.postMessage({
          jsonrpc: '2.0',
          id: Date.now(),
          method: 'ui/open-link',
          params: { url: url }
        }, '*');
      } else {
        // Fallback for standalone browser
        window.open(url, '_blank');
      }
    }
    
    function renderFlamegraph() {
      const container = document.getElementById('flamegraph');
      container.innerHTML = '';
      
      BUCKETS.forEach(bucket => {
        let issues = [];
        if (bucket.key === 'deepWork' && wheelData.deepWork) {
          issues = [wheelData.deepWork];
        } else if (wheelData[bucket.key]) {
          issues = wheelData[bucket.key];
        }
        
        const section = document.createElement('div');
        section.className = 'flame-section';
        
        const header = document.createElement('div');
        header.className = 'flame-header ' + bucket.cssClass;
        header.innerHTML = \`<div class="dot"></div>\${bucket.name} (\${issues.length})\`;
        section.appendChild(header);
        
        const stack = document.createElement('div');
        stack.className = 'flame-stack';
        
        if (issues.length === 0) {
          const empty = document.createElement('div');
          empty.className = 'empty-message';
          empty.textContent = 'No issues';
          stack.appendChild(empty);
        } else {
          issues.forEach(issue => {
            const stress = calculateIssueStress(issue, bucket.key, wheelData.stressScore);
            const stressLevel = getStressLevel(stress);
            const githubUrl = \`https://github.com/\${wheelData.repo}/issues/\${issue.number}\`;
            
            // Use div with click handler for MCP app context
            const bar = document.createElement('div');
            bar.className = 'flame-bar ' + bucket.cssClass;
            bar.dataset.number = issue.number;
            bar.dataset.stress = stress;
            bar.dataset.title = issue.title;
            bar.dataset.url = githubUrl;
            bar.innerHTML = \`
              <span class="issue-num">#\${issue.number}</span>
              <span class="issue-title">\${escapeHtml(issue.title)}</span>
              <span class="stress-indicator \${stressLevel}">\${stress}%</span>
            \`;
            
            bar.addEventListener('click', (e) => {
              e.preventDefault();
              openIssueLink(githubUrl);
            });
            bar.addEventListener('mouseenter', (e) => showTooltip(e, issue, stress));
            bar.addEventListener('mousemove', (e) => moveTooltip(e));
            bar.addEventListener('mouseleave', hideTooltip);
            
            stack.appendChild(bar);
          });
        }
        
        section.appendChild(stack);
        container.appendChild(section);
      });
    }
    
    function showTooltip(event, issue, stress) {
      const tooltip = document.getElementById('tooltip');
      const stressLevel = getStressLevel(stress);
      tooltip.innerHTML = \`
        <div class="title">#\${issue.number}: \${escapeHtml(issue.title)}</div>
        <div class="stress-row">
          <span>Stress:</span>
          <div class="stress-bar">
            <div class="stress-fill \${stressLevel}" style="width: \${stress}%"></div>
          </div>
          <span>\${stress}% (\${getStressLabel(stress)})</span>
        </div>
        <div class="click-hint">Click to open in GitHub</div>
      \`;
      moveTooltip(event);
      tooltip.classList.add('visible');
    }
    
    function moveTooltip(event) {
      const tooltip = document.getElementById('tooltip');
      tooltip.style.left = (event.clientX + 15) + 'px';
      tooltip.style.top = (event.clientY + 15) + 'px';
    }
    
    function hideTooltip() {
      document.getElementById('tooltip').classList.remove('visible');
    }
    
    function escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text || '';
      return div.innerHTML;
    }
    
    function updateUI(data) {
      wheelData = normalizeData(data);
      
      document.getElementById('loading').style.display = 'none';
      document.getElementById('content').style.display = 'block';
      
      document.getElementById('repo-name').textContent = wheelData.repo || 'Unknown';
      document.getElementById('stress-score').textContent = (wheelData.stressScore ?? '-') + '/100';
      document.getElementById('friday-score').textContent = (wheelData.fridayScore ?? '-') + '%';
      
      renderFlamegraph();
    }
    
    function normalizeData(data) {
      const plan = data.dayPlan || data;
      return {
        repo: data.repo || plan.repo,
        deepWork: plan.deepWork,
        quickWins: plan.quickWins || [],
        maintenance: plan.maintenance || [],
        deferred: plan.deferred || [],
        stressScore: data.stressScore ?? plan.stressScore,
        fridayScore: data.fridayScore ?? plan.fridayScore,
      };
    }
    
    function loadDemoData() {
      updateUI({
        repo: 'demo/burnout-app',
        dayPlan: {
          deepWork: { number: 42, title: 'Implement caching layer', complexity: 8, labels: [{ name: 'critical' }] },
          quickWins: [
            { number: 18, title: 'Fix typo in README', complexity: 1 },
            { number: 23, title: 'Update dependencies', complexity: 2 },
            { number: 31, title: 'Add unit test', complexity: 2 },
          ],
          maintenance: [
            { number: 15, title: 'Refactor auth module', complexity: 5 },
            { number: 19, title: 'Database migration', complexity: 4 },
            { number: 27, title: 'CI pipeline update', complexity: 3 },
          ],
          deferred: [
            { number: 8, title: 'Major feature X', complexity: 13 },
            { number: 12, title: 'Performance audit', complexity: 8 },
          ],
        },
        stressScore: 35,
        fridayScore: 78,
      });
    }
    
    // Handle messages from MCP host
    const pendingRequests = new Map();
    let requestId = 0;
    let isStandalone = true;
    
    function sendRequest(method, params) {
      return new Promise((resolve, reject) => {
        if (window.parent === window) {
          reject(new Error('No parent frame'));
          return;
        }
        
        const id = ++requestId;
        const timeout = setTimeout(() => {
          pendingRequests.delete(id);
          reject(new Error('Request timeout'));
        }, 500);
        
        pendingRequests.set(id, { resolve, reject, timeout });
        window.parent.postMessage({ jsonrpc: '2.0', id, method, params: params || {} }, '*');
      });
    }
    
    function sendNotification(method, params) {
      window.parent.postMessage({ jsonrpc: '2.0', method, params: params || {} }, '*');
    }
    
    window.addEventListener('message', (event) => {
      const msg = event.data;
      if (!msg?.jsonrpc) return;
      
      // Handle responses to our requests
      if (msg.id !== undefined && pendingRequests.has(msg.id)) {
        const { resolve, reject, timeout } = pendingRequests.get(msg.id);
        clearTimeout(timeout);
        pendingRequests.delete(msg.id);
        msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result);
        return;
      }
      
      // Handle notifications from host
      if (msg.method === 'ui/notifications/tool-input') {
        const args = msg.params?.arguments || {};
        if (args.dayPlan || args.deepWork || args.quickWins || args.repo) {
          updateUI(args);
        }
      } else if (msg.method === 'ui/notifications/tool-result') {
        if (msg.params?.structuredContent) {
          updateUI(msg.params.structuredContent);
        }
      } else if (msg.method === 'ui/notifications/host-context-changed') {
        // Theme changes
        if (msg.params?.theme) {
          document.documentElement.style.colorScheme = msg.params.theme;
        }
      }
    });
    
    // Initialize connection with MCP host
    async function initialize() {
      try {
        const result = await sendRequest('ui/initialize', {
          protocolVersion: '2026-01-26',
          capabilities: {},
          clientInfo: { name: 'burnout-flamegraph', version: '1.0.0' }
        });
        
        if (result?.hostContext?.theme) {
          document.documentElement.style.colorScheme = result.hostContext.theme;
        }
        
        sendNotification('ui/notifications/initialized', {});
        isStandalone = false;
        
      } catch (err) {
        // No MCP host - load demo data for standalone testing
        isStandalone = true;
        loadDemoData();
      }
    }
    
    // Start initialization
    initialize();
  </script>
</body>
</html>`;
}
