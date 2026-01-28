// Burnout Wheel UI Template - Vanilla SVG (NO D3.js)
// This generates the HTML/JS for the MCP App UI

export function generateWheelUI(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="light dark">
  <title>Burnout 3-3-3 Wheel</title>
  <style>
    :root {
      --bg: #ffffff;
      --text: #1a1a1a;
      --text-muted: #666666;
      --border: #e0e0e0;
      --deep-work: #4CAF50;
      --quick-wins: #2196F3;
      --maintenance: #FF9800;
      --deferred: #9E9E9E;
      --card-bg: #f8f9fa;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #1a1a1a;
        --text: #e0e0e0;
        --text-muted: #999999;
        --border: #333333;
        --card-bg: #252525;
      }
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
      padding: 16px;
      line-height: 1.5;
    }
    .container {
      max-width: 800px;
      margin: 0 auto;
    }
    .header {
      text-align: center;
      margin-bottom: 20px;
    }
    .header h1 {
      font-size: 1.5rem;
      font-weight: 600;
      margin-bottom: 4px;
    }
    .header .repo {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .status {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 0.75rem;
      background: var(--card-bg);
      margin-top: 8px;
    }
    .status.connected { background: #4CAF5033; color: #4CAF50; }
    .status.demo { background: #FF980033; color: #FF9800; }
    .status.error { background: #f4433633; color: #f44336; }
    
    .wheel-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 24px;
    }
    .wheel-container {
      position: relative;
      width: 280px;
      height: 280px;
    }
    #wheel-svg {
      width: 100%;
      height: 100%;
    }
    .arc {
      cursor: pointer;
      transition: opacity 0.2s, transform 0.2s;
      transform-origin: center;
    }
    .arc:hover {
      opacity: 0.85;
    }
    .arc.selected {
      stroke: var(--text);
      stroke-width: 3;
    }
    .center-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      pointer-events: none;
    }
    .center-text .score {
      font-size: 2rem;
      font-weight: 700;
    }
    .center-text .label {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    
    .metrics {
      display: flex;
      gap: 16px;
      justify-content: center;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }
    .metric {
      background: var(--card-bg);
      padding: 12px 16px;
      border-radius: 8px;
      text-align: center;
      min-width: 100px;
    }
    .metric .value {
      font-size: 1.5rem;
      font-weight: 600;
    }
    .metric .label {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .metric.stress .value { color: var(--quick-wins); }
    .metric.friday .value { color: var(--deep-work); }
    
    .legend {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
      margin-bottom: 24px;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: var(--card-bg);
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;
    }
    .legend-item:hover {
      background: var(--border);
    }
    .legend-item.selected {
      outline: 2px solid var(--text);
    }
    .legend-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .legend-text {
      flex: 1;
    }
    .legend-count {
      font-weight: 600;
      font-size: 0.9rem;
    }
    
    .bucket-details {
      background: var(--card-bg);
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 16px;
      display: none;
    }
    .bucket-details.visible {
      display: block;
    }
    .bucket-details h3 {
      font-size: 1rem;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .issue-list {
      list-style: none;
    }
    .issue-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 0;
      border-bottom: 1px solid var(--border);
      cursor: pointer;
    }
    .issue-item:last-child {
      border-bottom: none;
    }
    .issue-item:hover {
      color: var(--quick-wins);
    }
    .issue-number {
      font-family: monospace;
      color: var(--text-muted);
      font-size: 0.85rem;
    }
    .issue-title {
      flex: 1;
    }
    .complexity {
      background: var(--border);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    
    .tooltip {
      position: fixed;
      background: var(--text);
      color: var(--bg);
      padding: 6px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      pointer-events: none;
      z-index: 1000;
      display: none;
      max-width: 250px;
    }
    .tooltip.visible {
      display: block;
    }
    
    .explanation {
      background: var(--card-bg);
      border-radius: 8px;
      padding: 16px;
      margin-top: 16px;
      font-size: 0.9rem;
      line-height: 1.6;
    }
    .explanation strong { color: var(--deep-work); }
    
    .loading {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
    }
    .loading .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid var(--border);
      border-top-color: var(--quick-wins);
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 16px;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  </style>
</head>
<body>
  <div class="container">
    <div id="loading" class="loading">
      <div class="spinner"></div>
      <div>Loading your workload...</div>
    </div>
    
    <div id="content" style="display: none;">
      <div class="header">
        <h1>🎯 3-3-3 Day Structure</h1>
        <div class="repo" id="repo-name">-</div>
        <div class="status" id="status">Connecting...</div>
      </div>
      
      <div class="wheel-section">
        <div class="wheel-container">
          <svg id="wheel-svg" viewBox="0 0 280 280"></svg>
          <div class="center-text">
            <div class="score" id="stress-score">-</div>
            <div class="label">Stress Score</div>
          </div>
        </div>
      </div>
      
      <div class="metrics">
        <div class="metric stress">
          <div class="value" id="metric-stress">-</div>
          <div class="label">Stress Level</div>
        </div>
        <div class="metric friday">
          <div class="value" id="metric-friday">-</div>
          <div class="label">Friday Score</div>
        </div>
      </div>
      
      <div class="legend" id="legend"></div>
      
      <div class="bucket-details" id="bucket-details">
        <h3 id="bucket-title">Issues</h3>
        <ul class="issue-list" id="issue-list"></ul>
      </div>
      
      <div class="explanation" id="explanation" style="display: none;"></div>
    </div>
  </div>
  
  <div class="tooltip" id="tooltip"></div>
  
  <script>
    // State
    let wheelData = null;
    let selectedBucket = null;
    let isStandalone = false;
    let pendingRequests = new Map();
    let nextRequestId = 1;
    
    // Bucket configuration
    const BUCKETS = [
      { key: 'deepWork', name: 'Deep Work', icon: '🎯', color: '#4CAF50' },
      { key: 'quickWins', name: 'Quick Wins', icon: '⚡', color: '#2196F3' },
      { key: 'maintenance', name: 'Maintenance', icon: '🔧', color: '#FF9800' },
      { key: 'deferred', name: 'Deferred', icon: '📦', color: '#9E9E9E' },
    ];
    
    // MCP Protocol Communication
    function sendRequest(method, params) {
      const id = nextRequestId++;
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          pendingRequests.delete(id);
          reject(new Error('Request timeout'));
        }, 5000);
        
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
      
      // Handle responses
      if (msg.id !== undefined && pendingRequests.has(msg.id)) {
        const { resolve, reject, timeout } = pendingRequests.get(msg.id);
        clearTimeout(timeout);
        pendingRequests.delete(msg.id);
        msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result);
        return;
      }
      
      // Handle notifications from host
      if (msg.method === 'ui/notifications/tool-input') {
        handleToolInput(msg.params?.arguments || {});
      } else if (msg.method === 'ui/notifications/tool-result') {
        handleToolResult(msg.params);
      } else if (msg.method === 'ui/notifications/host-context-changed') {
        applyHostContext(msg.params);
      }
    });
    
    function handleToolInput(args) {
      // Live preview as model generates
      if (args.dayPlan || args.deepWork || args.quickWins) {
        updateWheel(args);
      }
    }
    
    function handleToolResult(result) {
      if (result?.structuredContent) {
        updateWheel(result.structuredContent);
      }
    }
    
    function applyHostContext(ctx) {
      if (ctx?.theme) {
        document.documentElement.style.colorScheme = ctx.theme;
      }
      if (ctx?.styles?.css?.properties) {
        const root = document.documentElement;
        for (const [key, value] of Object.entries(ctx.styles.css.properties)) {
          root.style.setProperty(key, value);
        }
      }
    }
    
    // Initialize
    async function initialize() {
      try {
        const result = await sendRequest('ui/initialize', {
          protocolVersion: '2026-01-26',
          capabilities: {},
          clientInfo: { name: 'burnout-wheel', version: '1.0.0' }
        });
        
        if (result?.hostContext) {
          applyHostContext(result.hostContext);
        }
        
        sendNotification('ui/notifications/initialized', {});
        updateStatus('connected', 'Connected');
        
      } catch (err) {
        console.log('No MCP host, running standalone:', err.message);
        isStandalone = true;
        updateStatus('demo', 'Demo Mode');
        loadDemoData();
      }
    }
    
    function loadDemoData() {
      updateWheel({
        repo: 'demo/burnout-app',
        dayPlan: {
          deepWork: { number: 42, title: 'Implement caching layer', complexity: 8 },
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
        agentExplanation: '**Demo Mode** - This is sample data. Connect to an MCP host to see your real workload!',
      });
    }
    
    function updateStatus(type, text) {
      const el = document.getElementById('status');
      el.className = 'status ' + type;
      el.textContent = text;
    }
    
    // Update wheel with data
    function updateWheel(data) {
      wheelData = normalizeData(data);
      
      document.getElementById('loading').style.display = 'none';
      document.getElementById('content').style.display = 'block';
      
      document.getElementById('repo-name').textContent = wheelData.repo || 'Unknown';
      document.getElementById('stress-score').textContent = wheelData.stressScore ?? '-';
      document.getElementById('metric-stress').textContent = getStressEmoji(wheelData.stressScore);
      document.getElementById('metric-friday').textContent = (wheelData.fridayScore ?? '-') + '%';
      
      renderDonut();
      renderLegend();
      
      if (wheelData.agentExplanation) {
        const expEl = document.getElementById('explanation');
        expEl.innerHTML = formatMarkdown(wheelData.agentExplanation);
        expEl.style.display = 'block';
      }
    }
    
    function normalizeData(data) {
      // Handle both flat and nested structures
      const plan = data.dayPlan || data;
      return {
        repo: data.repo || plan.repo,
        deepWork: plan.deepWork,
        quickWins: plan.quickWins || [],
        maintenance: plan.maintenance || [],
        deferred: plan.deferred || [],
        stressScore: data.stressScore ?? plan.stressScore,
        fridayScore: data.fridayScore ?? plan.fridayScore,
        agentExplanation: data.agentExplanation || plan.agentExplanation,
      };
    }
    
    function getStressEmoji(score) {
      if (score === undefined || score === null) return '-';
      if (score < 30) return '🟢 Low';
      if (score < 60) return '🟡 Moderate';
      return '🔴 High';
    }
    
    function formatMarkdown(text) {
      return text
        .replace(/\\*\\*(.+?)\\*\\*/g, '<strong>$1</strong>')
        .replace(/\\n/g, '<br>');
    }
    
    // SVG Donut Chart (NO D3.js!)
    function renderDonut() {
      const svg = document.getElementById('wheel-svg');
      svg.innerHTML = '';
      
      const cx = 140, cy = 140;
      const outerR = 120, innerR = 65;
      
      const counts = BUCKETS.map(b => {
        if (b.key === 'deepWork') return wheelData.deepWork ? 1 : 0;
        return (wheelData[b.key] || []).length;
      });
      const total = counts.reduce((a, b) => a + b, 0) || 1;
      
      let startAngle = -Math.PI / 2;
      
      BUCKETS.forEach((bucket, i) => {
        const count = counts[i];
        if (count === 0) return;
        
        const sliceAngle = (count / total) * 2 * Math.PI;
        const endAngle = startAngle + sliceAngle;
        
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', describeArc(cx, cy, innerR, outerR, startAngle, endAngle));
        path.setAttribute('fill', bucket.color);
        path.setAttribute('class', 'arc');
        path.setAttribute('data-bucket', bucket.key);
        
        path.addEventListener('click', () => selectBucket(bucket.key));
        path.addEventListener('mouseenter', (e) => showTooltip(e, bucket, count));
        path.addEventListener('mouseleave', hideTooltip);
        
        svg.appendChild(path);
        startAngle = endAngle;
      });
    }
    
    function describeArc(cx, cy, innerR, outerR, startAngle, endAngle) {
      const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
      
      const x1 = cx + outerR * Math.cos(startAngle);
      const y1 = cy + outerR * Math.sin(startAngle);
      const x2 = cx + outerR * Math.cos(endAngle);
      const y2 = cy + outerR * Math.sin(endAngle);
      const x3 = cx + innerR * Math.cos(endAngle);
      const y3 = cy + innerR * Math.sin(endAngle);
      const x4 = cx + innerR * Math.cos(startAngle);
      const y4 = cy + innerR * Math.sin(startAngle);
      
      return [
        'M', x1, y1,
        'A', outerR, outerR, 0, largeArc, 1, x2, y2,
        'L', x3, y3,
        'A', innerR, innerR, 0, largeArc, 0, x4, y4,
        'Z'
      ].join(' ');
    }
    
    function renderLegend() {
      const container = document.getElementById('legend');
      container.innerHTML = '';
      
      BUCKETS.forEach(bucket => {
        let count;
        if (bucket.key === 'deepWork') {
          count = wheelData.deepWork ? 1 : 0;
        } else {
          count = (wheelData[bucket.key] || []).length;
        }
        
        const item = document.createElement('div');
        item.className = 'legend-item' + (selectedBucket === bucket.key ? ' selected' : '');
        item.innerHTML = \`
          <div class="legend-dot" style="background: \${bucket.color}"></div>
          <div class="legend-text">\${bucket.icon} \${bucket.name}</div>
          <div class="legend-count">\${count}</div>
        \`;
        item.addEventListener('click', () => selectBucket(bucket.key));
        container.appendChild(item);
      });
    }
    
    function selectBucket(key) {
      selectedBucket = selectedBucket === key ? null : key;
      
      // Update arc selection
      document.querySelectorAll('.arc').forEach(arc => {
        arc.classList.toggle('selected', arc.dataset.bucket === selectedBucket);
      });
      
      // Update legend selection
      renderLegend();
      
      // Show bucket details
      const details = document.getElementById('bucket-details');
      if (!selectedBucket) {
        details.classList.remove('visible');
        return;
      }
      
      const bucket = BUCKETS.find(b => b.key === selectedBucket);
      document.getElementById('bucket-title').innerHTML = \`\${bucket.icon} \${bucket.name}\`;
      
      let issues = [];
      if (selectedBucket === 'deepWork' && wheelData.deepWork) {
        issues = [wheelData.deepWork];
      } else {
        issues = wheelData[selectedBucket] || [];
      }
      
      const list = document.getElementById('issue-list');
      list.innerHTML = issues.map(issue => \`
        <li class="issue-item" data-number="\${issue.number}">
          <span class="issue-number">#\${issue.number}</span>
          <span class="issue-title">\${escapeHtml(issue.title)}</span>
          \${issue.complexity ? \`<span class="complexity">\${issue.complexity}pts</span>\` : ''}
        </li>
      \`).join('');
      
      // Add click handlers for issues
      list.querySelectorAll('.issue-item').forEach(item => {
        item.addEventListener('click', () => onIssueClick(parseInt(item.dataset.number)));
      });
      
      details.classList.add('visible');
    }
    
    async function onIssueClick(number) {
      const issue = findIssue(number);
      if (!issue) return;
      
      if (isStandalone) {
        const url = \`https://github.com/\${wheelData.repo}/issues/\${number}\`;
        window.open(url, '_blank');
        return;
      }
      
      // Send message to chat
      try {
        await sendRequest('ui/message', {
          content: [{
            type: 'text',
            text: \`Tell me more about issue #\${number}: "\${issue.title}"\`
          }]
        });
      } catch (err) {
        console.error('Failed to send message:', err);
      }
    }
    
    function findIssue(number) {
      if (wheelData.deepWork?.number === number) return wheelData.deepWork;
      for (const key of ['quickWins', 'maintenance', 'deferred']) {
        const found = (wheelData[key] || []).find(i => i.number === number);
        if (found) return found;
      }
      return null;
    }
    
    function showTooltip(event, bucket, count) {
      const tooltip = document.getElementById('tooltip');
      tooltip.textContent = \`\${bucket.name}: \${count} item\${count !== 1 ? 's' : ''}\`;
      tooltip.style.left = event.clientX + 10 + 'px';
      tooltip.style.top = event.clientY + 10 + 'px';
      tooltip.classList.add('visible');
    }
    
    function hideTooltip() {
      document.getElementById('tooltip').classList.remove('visible');
    }
    
    function escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    }
    
    // Start
    initialize();
  </script>
</body>
</html>`;
}
