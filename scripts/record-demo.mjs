/**
 * Record a ~30-second demo video of the Burnout-as-a-Service pipeline:
 *   BEFORE check-in → BEFORE flamegraph → [sync real issues] → AFTER check-in → AFTER flamegraph → Team Dashboard
 *
 * Uses the REAL flow as documented in README:
 *   1. seed-demo.ps1 seeds 16 chaotic issues (stress → 100)
 *   2. /demo/api/sync fetches real GitHub issues (stress → 10)
 *   3. Study dashboard shows team trends
 *
 * Usage:
 *   node scripts/record-demo.mjs [base-url]
 *
 * Outputs:
 *   docs/images/demo/demo-pipeline.webm
 */

import { chromium } from 'playwright';
import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const BASE = process.argv[2] || 'https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io';
const REPO = 'roryp/burnout-app';
const USER = 'roryp';
const OUT_DIR = path.resolve('docs/images/demo');

fs.mkdirSync(OUT_DIR, { recursive: true });

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function main() {
  // ── Step 1: Ensure we have real GitHub issues cached ──
  // Sync once (or wait for rate limit), then grab the issues via the flamegraph endpoint.
  // We'll re-seed these as the AFTER state during recording, avoiding the rate limit issue.
  console.log('Step 1: Fetching real GitHub issues...');
  
  // Try to sync — if rate-limited, wait it out
  while (true) {
    const resp = await fetch(`${BASE}/demo/api/sync?repo=${REPO}`, { method: 'POST' });
    if (resp.ok) {
      const data = await resp.json();
      console.log(`  Synced ${data.issueCount} real issues from GitHub`);
      break;
    }
    const err = await resp.json().catch(() => ({}));
    const wait = (err.retryAfterSeconds || 60) + 2;
    console.log(`  Rate-limited, waiting ${wait}s...`);
    await sleep(wait * 1000);
  }
  
  // Fetch the real issues via public GitHub API so we can re-seed them later
  // Use issues-only endpoint (not PRs) — same as the backend's /demo/api/sync does
  console.log('  Caching real issues from GitHub API...');
  const ghResp = await fetch(`https://api.github.com/repos/${REPO}/issues?state=open&per_page=100`, {
    headers: { 'Accept': 'application/vnd.github.v3+json' },
  });
  const ghIssues = await ghResp.json();
  // Filter out pull requests (GitHub API returns PRs in /issues endpoint)
  const realIssues = ghIssues
    .filter(i => !i.pull_request)
    .map(i => ({
      number: i.number,
      title: i.title,
      body: i.body || '',
      labels: (i.labels || []).map(l => ({ name: typeof l === 'string' ? l : l.name })),
      assignees: (i.assignees || []).map(a => ({ login: a.login })),
      createdAt: i.created_at,
      updatedAt: i.updated_at,
      state: i.state,
    }));
  console.log(`  Cached ${realIssues.length} issues (filtered out PRs)`);

  // ── Step 2: Clear old data and seed fresh study history ──
  console.log('\nStep 2: Resetting study data and seeding fresh history...');
  await fetch(`${BASE}/demo/api/study/reset`, { method: 'DELETE' });
  const studyResp = await fetch(`${BASE}/demo/api/study/seed`, { method: 'POST' });
  if (studyResp.ok) {
    const sd = await studyResp.json();
    console.log(`  Seeded ${sd.seeded} fresh snapshots for ${sd.users.join(', ')}`);
  }

  // ── Step 3: Seed BEFORE state (16 chaotic issues → stress 100) ──
  console.log('\nStep 3: Seeding BEFORE state (16 chaotic issues)...');
  // Use the seed endpoint directly to avoid the seed script's checkin re-sync
  const now = new Date();
  const fmt = d => d.toISOString().replace(/\.\d+Z$/, 'Z');
  const ago = mins => fmt(new Date(now.getTime() - mins * 60000));
  const daysAgo = d => fmt(new Date(now.getTime() - d * 86400000));
  const hourUTC = h => { const d = new Date(now); d.setUTCHours(h, 0, 0, 0); return fmt(d); };

  const chaosIssues = [
    { number: 1, title: 'Critical auth bypass in OAuth flow', body: '', labels: [{ name: 'priority:critical' }, { name: 'security' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(10), state: 'open' },
    { number: 2, title: 'Refactor agent orchestration layer', body: '', labels: [{ name: 'architecture' }, { name: 'deep-work' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(20), state: 'open' },
    { number: 3, title: 'Implement new feature flags system', body: '', labels: [{ name: 'epic' }, { name: 'feature' }, { name: 'priority:critical' }], assignees: [{ login: USER }], createdAt: daysAgo(30), updatedAt: ago(30), state: 'open' },
    { number: 4, title: 'URGENT: Production memory leak', body: '', labels: [{ name: 'urgent' }, { name: 'bug' }], assignees: [], createdAt: daysAgo(14), updatedAt: hourUTC(3), state: 'open' },
    { number: 5, title: 'URGENT: API rate limiting broken', body: '', labels: [{ name: 'urgent' }, { name: 'bug' }], assignees: [], createdAt: daysAgo(7), updatedAt: hourUTC(4), state: 'open' },
    { number: 6, title: 'URGENT: Database connection pool exhaustion', body: '', labels: [{ name: 'urgent' }, { name: 'priority:critical' }], assignees: [], createdAt: daysAgo(30), updatedAt: hourUTC(22), state: 'open' },
    { number: 7, title: 'Fix typo in README', body: '', labels: [{ name: 'good-first-issue' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(40), state: 'open' },
    { number: 8, title: 'Update Spring Boot to 3.5.11', body: '', labels: [{ name: 'dependencies' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(50), state: 'open' },
    { number: 9, title: 'Something unclear', body: '', labels: [{ name: 'bug' }], assignees: [{ login: USER }], createdAt: daysAgo(30), updatedAt: ago(60), state: 'open' },
    { number: 10, title: 'Another vague issue', body: '', labels: [{ name: 'bug' }], assignees: [{ login: USER }], createdAt: daysAgo(30), updatedAt: ago(70), state: 'open' },
    { number: 11, title: 'CI pipeline failing intermittently', body: '', labels: [{ name: 'ci' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(80), state: 'open' },
    { number: 12, title: 'Write API documentation', body: '', labels: [{ name: 'documentation' }], assignees: [{ login: USER }], createdAt: daysAgo(14), updatedAt: ago(90), state: 'open' },
    { number: 13, title: 'Add dark mode toggle', body: '', labels: [{ name: 'enhancement' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: ago(100), state: 'open' },
    { number: 14, title: 'Fix CORS headers on demo endpoints', body: '', labels: [{ name: 'bug' }], assignees: [{ login: USER }], createdAt: daysAgo(7), updatedAt: hourUTC(3), state: 'open' },
    { number: 15, title: 'Stale tracking issue from last quarter', body: '', labels: [{ name: 'triage' }], assignees: [{ login: USER }], createdAt: daysAgo(30), updatedAt: hourUTC(4), state: 'open' },
    { number: 16, title: 'Upgrade Node.js to v22', body: '', labels: [{ name: 'dependencies' }], assignees: [{ login: USER }], createdAt: daysAgo(14), updatedAt: hourUTC(22), state: 'open' },
  ];
  const seedResp = await fetch(`${BASE}/demo/api/seed`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repo: REPO, issues: chaosIssues }),
  });
  const seedData = await seedResp.json();
  console.log(`  Seeded ${seedData.issueCount} chaotic issues (stress → 100)`);

  // Verify
  const verifyResp = await fetch(`${BASE}/demo/api/flamegraph?repo=${REPO}&userId=${USER}`);
  if (verifyResp.ok) {
    const fg = await verifyResp.json();
    console.log(`  Verified: Stress=${fg.stressScore}, Issues=${fg.totalIssues}`);
  }

  console.log('\nLaunching browser with video recording...');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    recordVideo: {
      dir: OUT_DIR,
      size: { width: 1280, height: 720 },
    },
  });
  const page = await context.newPage();

  // Helper: simulate human typing
  async function humanType(selector, text) {
    await page.click(selector);
    await page.type(selector, text, { delay: 50 });
  }

  // Helper: show a scene title card overlay
  async function showTitle(title, subtitle, holdMs = 2500) {
    await page.evaluate(([t, s]) => {
      const el = document.createElement('div');
      el.id = 'scene-title';
      el.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.88);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;transition:opacity 0.6s;';
      el.innerHTML = `
        <div style="font-size:2.2rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:12px;">${t}</div>
        <div style="color:#94a3b8;font-size:1.1rem;max-width:500px;text-align:center;line-height:1.5;">${s}</div>
      `;
      document.body.appendChild(el);
    }, [title, subtitle]);
    await sleep(holdMs);
    await page.evaluate(() => {
      const el = document.getElementById('scene-title');
      if (el) { el.style.opacity = '0'; }
    });
    await sleep(600);
    await page.evaluate(() => document.getElementById('scene-title')?.remove());
  }

  // Helper: show title card, navigate while it's visible, then fade out once loaded
  // Prevents the flash of landing pages / intermediate content before the real view renders
  async function showTitleThenNavigate(url, title, subtitle, holdMs = 2500, waitForSelector = null) {
    // Show overlay on the CURRENT page first
    await page.evaluate(([t, s]) => {
      const el = document.createElement('div');
      el.id = 'scene-title';
      el.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.88);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;transition:opacity 0.6s;';
      el.innerHTML = `
        <div style="font-size:2.2rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:12px;">${t}</div>
        <div style="color:#94a3b8;font-size:1.1rem;max-width:500px;text-align:center;line-height:1.5;">${s}</div>
      `;
      document.body.appendChild(el);
    }, [title, subtitle]);
    await sleep(holdMs);

    // Navigate to the new page
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    // Immediately inject a black cover so the landing page / loading state is hidden
    await page.evaluate(() => {
      const cover = document.createElement('div');
      cover.id = 'page-cover';
      cover.style.cssText = 'position:fixed;inset:0;background:#0a0a1a;z-index:9998;transition:opacity 0.5s;';
      document.body.appendChild(cover);
    });

    // Wait for the real content to render underneath the cover
    if (waitForSelector) {
      await page.waitForSelector(waitForSelector, { timeout: 15000 }).catch(() => {});
    } else {
      await page.waitForLoadState('networkidle');
    }
    await sleep(500); // extra buffer for animations/rendering

    // Fade out the cover to reveal the fully loaded page
    await page.evaluate(() => {
      const cover = document.getElementById('page-cover');
      if (cover) cover.style.opacity = '0';
    });
    await sleep(500);
    await page.evaluate(() => document.getElementById('page-cover')?.remove());
  }

  // ── Scene 1: BEFORE Check-in ──
  console.log('Scene 1: BEFORE check-in...');
  await page.goto(`${BASE}/checkin.html`);
  await page.waitForLoadState('networkidle');

  await showTitle('Step 1: Check Stress', 'A developer checks their burnout score on a chaotic Monday');

  await humanType('#userId', USER);
  await sleep(300);
  await humanType('#repo', REPO);
  await sleep(500);
  await page.click('#checkin-btn');

  await page.waitForSelector('#result-card', { state: 'visible', timeout: 15000 });
  await sleep(3000); // HOLD: let viewer read 100/CRITICAL and scan the bars

  // Expand workload to show which issues cause the stress
  const beforeToggles = await page.$$('.issue-toggle');
  for (const toggle of beforeToggles) {
    const text = await toggle.textContent();
    if (text && text.includes('issue')) { await toggle.click(); break; }
  }
  await sleep(4000); // HOLD: viewer reads expanded issue list

  // ── Scene 2: BEFORE Flamegraph ──
  console.log('Scene 2: BEFORE flamegraph...');
  await showTitleThenNavigate(
    `${BASE}/flamegraph.html?repo=${REPO}&userId=${USER}`,
    'Step 2: View Flamegraph',
    '16 issues — no quick wins, 9 deferred, 100% stress',
    2500,
    'text=Deep Work'
  );
  await sleep(5000); // HOLD: viewer scans the imbalanced flamegraph

  // ── Scene 3: Rebalance transition ──
  console.log('Scene 3: Rebalancing workload...');

  await page.evaluate(() => {
    const overlay = document.createElement('div');
    overlay.id = 'sync-overlay';
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.88);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;';
    overlay.innerHTML = `
      <div style="font-size:2.5rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:12px;">
        Reshaping Your Day...
      </div>
      <div style="color:#94a3b8;font-size:1.1rem;max-width:480px;text-align:center;line-height:1.6;">
        AI agents classify issues into the 3-3-3 structure:<br/>
        1 deep work · 3 quick wins · 3 maintenance
      </div>
      <div style="margin-top:24px;width:240px;height:4px;background:rgba(255,255,255,0.1);border-radius:2px;overflow:hidden;">
        <div id="sync-bar" style="width:0%;height:100%;background:linear-gradient(90deg,#00f260,#0575e6);border-radius:2px;transition:width 3.5s ease-in-out;"></div>
      </div>
    `;
    document.body.appendChild(overlay);
    requestAnimationFrame(() => document.getElementById('sync-bar').style.width = '100%');
  });

  // Re-seed with the REAL issues we cached earlier (avoids rate limit)
  const reSeedResp = await fetch(`${BASE}/demo/api/seed`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repo: REPO, issues: realIssues }),
  });
  if (reSeedResp.ok) {
    const d = await reSeedResp.json();
    console.log(`  Re-seeded ${d.issueCount} real issues`);
  }

  await sleep(4500); // HOLD: viewer reads the reshape explanation
  await page.evaluate(() => {
    const el = document.getElementById('sync-overlay');
    if (el) el.style.opacity = '0';
    el.style.transition = 'opacity 0.6s';
  });
  await sleep(600);
  await page.evaluate(() => document.getElementById('sync-overlay')?.remove());

  // ── Scene 4: AFTER Check-in ──
  console.log('Scene 4: AFTER check-in...');
  await showTitleThenNavigate(
    `${BASE}/checkin.html`,
    'Step 3: Check Again',
    'Same developer, same repo — after reshaping'
  );

  await page.fill('#userId', USER);
  await page.fill('#repo', REPO);
  await sleep(400);
  await page.click('#checkin-btn');

  await page.waitForSelector('#result-card', { state: 'visible', timeout: 15000 });
  await sleep(3000); // HOLD: viewer sees 10/LOW — the dramatic drop

  // Expand workload to show the balanced issue list
  const afterToggles = await page.$$('.issue-toggle');
  for (const toggle of afterToggles) {
    const text = await toggle.textContent();
    if (text && text.includes('issue')) { await toggle.click(); break; }
  }
  await sleep(4000); // HOLD: viewer reads the reduced issue list

  // ── Scene 5: AFTER Flamegraph ──
  console.log('Scene 5: AFTER flamegraph...');
  await showTitleThenNavigate(
    `${BASE}/flamegraph.html?repo=${REPO}&userId=${USER}`,
    'Step 4: Balanced Flamegraph',
    '3-3-3 structure — 1 deep work, 3 quick wins, 3 maintenance',
    2500,
    'text=Deep Work'
  );
  await sleep(5000); // HOLD: viewer scans the balanced 3-3-3 flamegraph

  // ── Scene 6: Team Dashboard ──
  console.log('Scene 6: Team stress dashboard...');
  await showTitleThenNavigate(
    `${BASE}/study.html`,
    'Step 5: Team Dashboard',
    'Track stress trends across participants over time',
    2500,
    '#load-btn'
  );
  await page.click('#load-btn');
  await page.waitForSelector('#summary', { state: 'visible', timeout: 15000 }).catch(() => {});
  await sleep(3000); // HOLD: viewer reads summary cards + trend chart

  // Scroll to participant tiles
  await page.evaluate(() => {
    const grid = document.getElementById('participants-section');
    if (grid) grid.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
  await sleep(4000); // HOLD: viewer reads participant cards

  // ── Done ──
  console.log('Finalizing video...');
  await page.close();
  await context.close();
  await browser.close();

  // Find and rename the video file
  const dest = path.join(OUT_DIR, 'demo-pipeline.webm');
  if (fs.existsSync(dest)) fs.unlinkSync(dest);

  const files = fs.readdirSync(OUT_DIR).filter(f => f.endsWith('.webm') && f !== 'demo-pipeline.webm');
  if (files.length > 0) {
    const latest = files.sort().pop();
    const src = path.join(OUT_DIR, latest);
    fs.renameSync(src, dest);
    console.log(`\nVideo saved: ${dest}`);

    const stats = fs.statSync(dest);
    console.log(`Size: ${(stats.size / 1024 / 1024).toFixed(1)} MB`);
  } else {
    console.error('No video file generated!');
  }
}

main().catch(err => {
  console.error('Recording failed:', err);
  process.exit(1);
});
