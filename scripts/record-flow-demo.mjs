/**
 * record-flow-demo.mjs — clean rewrite (v3)
 * ============================================================================
 * A continuously-moving 2-minute demo recording. No empty gaps, no static
 * frozen frames, no white flashes.
 *
 * Core architecture
 * -----------------
 *  - Browser context starts with `addInitScript` that injects a full-screen
 *    OPAQUE dark overlay (`#rfd-overlay`) on EVERY page navigation, BEFORE
 *    any of the live page's own scripts get to paint. The overlay is always
 *    there — we either fade it in (covering navigation) or fade it out
 *    (revealing the live page).
 *  - Between scenes: overlay is OPAQUE while page.goto happens. Live page
 *    content loads behind it. We wait for the page's specific ready marker
 *    (e.g. "Deep Work" text on flamegraph). Only then do we fade the overlay
 *    out — so the user NEVER sees a blank/loading frame.
 *  - During holds on live pages: we do continuous slow scrolling so the
 *    audience always sees motion. No frame is the same for more than ~2s.
 *  - Reshape API kicks off at the start of its scene; the phase animation
 *    runs while the call is in flight. AFTER reveal waits for the real result.
 *
 * Output: docs/images/demo/flow-demo.webm  (~115 seconds)
 *
 * Usage: node scripts/record-flow-demo.mjs [base-url]
 */

import { chromium } from "playwright";
import fs from "fs";
import path from "path";

const BASE = process.argv[2]
  || process.env.BACKEND_URL
  || "https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io";
const REPO = "roryp/burnout-app";
const USER = "roryp";
const OUT_DIR = path.resolve("docs/images/demo");
const FINAL = path.join(OUT_DIR, "flow-demo.webm");

fs.mkdirSync(OUT_DIR, { recursive: true });

// ============================================================================
// Helpers
// ============================================================================
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function fetchJson(url, opts = {}, ms = 15000) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), ms);
  try {
    const r = await fetch(url, { ...opts, signal: ctrl.signal });
    if (!r.ok) throw new Error(`${url} -> HTTP ${r.status}`);
    return await r.json();
  } finally {
    clearTimeout(t);
  }
}

const isoMinAgo  = m => new Date(Date.now() - m * 60000).toISOString().replace(/\.\d+Z$/, "Z");
const isoDaysAgo = d => new Date(Date.now() - d * 86400000).toISOString().replace(/\.\d+Z$/, "Z");
function isoHourUTC(h) {
  const d = new Date();
  d.setUTCHours(h, 0, 0, 0);
  return d.toISOString().replace(/\.\d+Z$/, "Z");
}

// ============================================================================
// Pre-record: seed chaos state, capture BEFORE numbers
// ============================================================================
async function buildChaosIssues() {
  let titles = [];
  try {
    const arr = await fetchJson(
      `https://api.github.com/repos/${REPO}/issues?state=open&per_page=30`,
      { headers: { Accept: "application/vnd.github+json", "User-Agent": "burnout-demo" } },
      10000,
    );
    titles = arr.filter(i => !i.pull_request).slice(0, 16);
  } catch { /* fall through to synthetic */ }
  if (titles.length < 4) {
    titles = [
      { number: 1,  title: "Critical auth bypass in OAuth flow",        labels: [{ name: "priority:critical" }, { name: "security" }] },
      { number: 2,  title: "URGENT: Production memory leak",            labels: [{ name: "bug" }] },
      { number: 3,  title: "URGENT: API rate limiting broken",          labels: [{ name: "bug" }] },
      { number: 4,  title: "URGENT: DB connection pool exhaustion",     labels: [{ name: "priority:critical" }] },
      { number: 5,  title: "Refactor agent orchestration layer",        labels: [{ name: "architecture" }] },
      { number: 6,  title: "Implement feature flags system",            labels: [{ name: "epic" }, { name: "feature" }] },
      { number: 7,  title: "Fix typo in README",                         labels: [{ name: "good-first-issue" }] },
      { number: 8,  title: "Update Spring Boot to 3.5.11",               labels: [{ name: "dependencies" }] },
      { number: 9,  title: "Something unclear",                          labels: [{ name: "bug" }] },
      { number: 10, title: "Another vague issue",                        labels: [{ name: "bug" }] },
      { number: 11, title: "CI pipeline failing intermittently",         labels: [{ name: "ci" }] },
      { number: 12, title: "Write API documentation",                    labels: [{ name: "documentation" }] },
      { number: 13, title: "Add dark mode toggle",                       labels: [{ name: "enhancement" }] },
      { number: 14, title: "Fix CORS headers on demo endpoints",         labels: [{ name: "bug" }] },
      { number: 15, title: "Stale tracking issue from last quarter",     labels: [{ name: "triage" }] },
      { number: 16, title: "Upgrade Node.js to v22",                     labels: [{ name: "dependencies" }] },
    ];
  }
  const recents = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100].map(isoMinAgo);
  const ahHours = [isoHourUTC(3), isoHourUTC(4), isoHourUTC(22)];
  return titles.map((t, i) => {
    const isUrgent = i < 6;
    const labels = (t.labels || []).map(l => ({ name: typeof l === "string" ? l : l.name }));
    if (isUrgent) {
      labels.push({ name: "urgent" });
      labels.push({ name: "priority:critical" });
    }
    return {
      number: t.number,
      title: t.title,
      body: "",
      labels,
      assignees: isUrgent ? [] : [{ login: USER }],
      createdAt: isUrgent ? isoDaysAgo(30) : isoDaysAgo(7),
      updatedAt: isUrgent ? ahHours[i % ahHours.length] : recents[(i - 6) % recents.length],
      state: "open",
    };
  });
}

async function pre() {
  console.log(`▶  base = ${BASE}`);
  console.log("▶  building 16 chaos issues...");
  const issues = await buildChaosIssues();

  console.log("▶  resetting + seeding study history...");
  await fetch(`${BASE}/demo/api/study/reset`, { method: "DELETE" }).catch(() => {});
  await fetchJson(`${BASE}/demo/api/study/seed`, { method: "POST" }, 20000).catch(() => {});

  console.log("▶  seeding chaos state...");
  await fetchJson(`${BASE}/demo/api/seed`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, issues }),
  }, 15000);

  console.log("▶  capturing BEFORE state (checkin)...");
  const before = await fetchJson(`${BASE}/demo/api/checkin`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: USER, repo: REPO, selfScore: 50 }),
  }, 15000);
  console.log(`    BEFORE stress=${before.stressScore} (${before.stressLevel})`);

  console.log("▶  capturing BEFORE Friday score...");
  const beforeFlame = await fetchJson(
    `${BASE}/demo/api/flamegraph?repo=${encodeURIComponent(REPO)}&userId=${USER}`,
    {},
    15000,
  ).catch(() => ({ fridayScore: null }));

  // Checkin may have nudged cache state — re-seed.
  await fetchJson(`${BASE}/demo/api/seed`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, issues }),
  }, 15000);

  return {
    issues,
    before: {
      stressScore: before.stressScore,
      stressLevel: before.stressLevel,
      breakdown: before.breakdown || {},
      fridayScore: beforeFlame.fridayScore,
    },
  };
}

// ============================================================================
// Overlay init script — runs on EVERY new document, BEFORE any page script
// paints, so the recording NEVER shows a white/loading frame between scenes.
// ============================================================================
const OVERLAY_INIT_SCRIPT = `
  (function() {
    function inject() {
      if (document.getElementById("rfd-overlay")) return;
      const root = document.body || document.documentElement;
      if (!root) { setTimeout(inject, 4); return; }
      const headOrRoot = document.head || document.documentElement || root;
      if (headOrRoot && !document.getElementById("rfd-fonts")) {
        const link = document.createElement("link");
        link.id = "rfd-fonts";
        link.rel = "stylesheet";
        link.href = "https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Inter:wght@400;700;800&display=swap";
        headOrRoot.appendChild(link);
      }
      const ov = document.createElement("div");
      ov.id = "rfd-overlay";
      ov.style.cssText = "position:fixed;inset:0;background:#05050d;z-index:2147483646;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:24px;box-sizing:border-box;opacity:1;transition:opacity 0.6s ease-out;font-family:'Inter',system-ui,sans-serif;color:#fff;pointer-events:none;";
      root.appendChild(ov);
      // Migrate overlay into <body> once body exists
      if (root !== document.body) {
        try {
          const mo = new MutationObserver(() => {
            if (document.body && ov.parentNode !== document.body) {
              document.body.appendChild(ov);
              mo.disconnect();
            }
          });
          mo.observe(document.documentElement, { childList: true, subtree: false });
        } catch (_) { /* documentElement may not be observable yet — ignore */ }
      }
    }
    inject();
  })();
`;

// ============================================================================
// Scene helpers (operate on the persistent overlay)
// ============================================================================

/** Replace overlay content and ensure it's fully opaque (self-healing). */
async function showOverlay(page, html) {
  await page.evaluate((h) => {
    let ov = document.getElementById("rfd-overlay");
    if (!ov) {
      ov = document.createElement("div");
      ov.id = "rfd-overlay";
      ov.style.cssText = "position:fixed;inset:0;background:#05050d;z-index:2147483646;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:24px;box-sizing:border-box;opacity:1;transition:opacity 0.6s ease-out;font-family:'Inter',system-ui,sans-serif;color:#fff;pointer-events:none;";
      (document.body || document.documentElement).appendChild(ov);
    }
    ov.style.transition = "none";
    ov.style.opacity = "1";
    ov.innerHTML = h;
    // Force reflow so subsequent transitions work
    void ov.offsetWidth;
    ov.style.transition = "opacity 0.6s ease-out";
  }, html);
}

/** Fade the overlay out to reveal the live page underneath. */
async function hideOverlay(page) {
  await page.evaluate(() => {
    const ov = document.getElementById("rfd-overlay");
    if (ov) ov.style.opacity = "0";
  });
  await sleep(700); // wait for fade to complete
}

/** Force overlay back to opaque immediately (for transitions). */
async function lockOverlay(page) {
  await page.evaluate(() => {
    const ov = document.getElementById("rfd-overlay");
    if (ov) {
      ov.style.transition = "none";
      ov.style.opacity = "1";
    }
  });
}

/** Navigate with the overlay covering the entire transition. */
async function navigateUnderOverlay(page, url, transitionHtml, readySelector) {
  await showOverlay(page, transitionHtml);
  await sleep(1800); // let the audience read the transition card

  await lockOverlay(page);  // ensure opaque before nav
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 20000 }).catch(() => {});

  // The init script has already created a fresh opaque overlay on the new
  // page. Restore the transition content (it was wiped by navigation).
  await showOverlay(page, transitionHtml);

  // Wait for the actual page content to be ready
  if (readySelector) {
    try {
      await page.waitForSelector(readySelector, { state: "visible", timeout: 12000 });
    } catch { /* proceed anyway */ }
  }
  await sleep(900); // a beat for paint to settle
}

/** Add a corner badge that floats above the live page chrome. */
async function showCornerBadge(page, label, score, level, friday, isBefore) {
  const bg = isBefore ? "rgba(244,63,94,0.96)" : "rgba(34,197,94,0.96)";
  const fg = isBefore ? "#fff" : "#05050d";
  await page.evaluate(({ l, s, lv, f, bg, fg }) => {
    document.getElementById("rfd-badge")?.remove();
    const b = document.createElement("div");
    b.id = "rfd-badge";
    b.style.cssText = `position:fixed;top:18px;right:18px;background:${bg};color:${fg};padding:11px 20px;border-radius:12px;font-weight:800;font-size:0.98rem;z-index:2147483645;box-shadow:0 8px 24px rgba(0,0,0,0.6);letter-spacing:0.5px;font-family:'Inter',system-ui,sans-serif;`;
    b.textContent = `${l}  ·  ${s} / ${lv}  ·  Fri ${f ?? "—"}`;
    document.body.appendChild(b);
  }, { l: label, s: score, lv: level, f: friday, bg, fg });
}

async function removeCornerBadge(page) {
  await page.evaluate(() => document.getElementById("rfd-badge")?.remove());
}

/**
 * Continuous slow scroll over `durationMs`. Splits into ~20fps steps so the
 * recording always shows motion. Returns when done.
 */
async function slowScroll(page, durationMs, totalPx) {
  const FPS = 30;
  const steps = Math.max(1, Math.floor(durationMs / (1000 / FPS)));
  const stepPx = totalPx / steps;
  const stepMs = durationMs / steps;
  for (let i = 0; i < steps; i++) {
    await page.evaluate(px => window.scrollBy(0, px), stepPx);
    await sleep(stepMs);
  }
}

/** Wall-clock floor for a scene — never cuts content short. */
async function sceneSettle(startMs, targetMs) {
  const elapsed = Date.now() - startMs;
  if (elapsed < targetMs) await sleep(targetMs - elapsed);
}

// ============================================================================
// Main recording
// ============================================================================
async function main() {
  const captured = await pre();

  console.log("\n▶  launching browser + recorder...");
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    recordVideo: { dir: OUT_DIR, size: { width: 1280, height: 720 } },
  });

  // Inject the overlay creator on EVERY page navigation, before any
  // page-owned script gets a chance to paint a white background.
  await context.addInitScript(OVERLAY_INIT_SCRIPT);

  const page = await context.newPage();

  // Surface page-side errors so we don't silently lose them
  page.on("pageerror", err => console.error(`    [page error] ${err.message}`));
  page.on("console", msg => {
    if (msg.type() === "error") console.error(`    [page console.error] ${msg.text()}`);
  });

  // First page is a styled shell so the very first frame is dark.
  await page.setContent(`<!DOCTYPE html><html><head>
    <style>
      html, body { margin:0; padding:0; background:#05050d; min-height:100vh; overflow-x:hidden; font-family:'Inter',system-ui,sans-serif; color:#fff; }
    </style>
  </head><body></body></html>`, { waitUntil: "domcontentloaded" });

  const t0 = Date.now();
  const stamp = label => console.log(`    [+${((Date.now() - t0) / 1000).toFixed(1)}s] ${label}`);

  // ==========================================================================
  // SCENE 0 — INTRO TITLE (target 5.5s)
  // ==========================================================================
  stamp("Scene 0: intro");
  const s0 = Date.now();
  await showOverlay(page, `
    <div style="font-size:0.85rem;letter-spacing:4px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:20px;opacity:0;animation:fI 0.6s 0.1s forwards;">Burnout-as-a-Service</div>
    <div style="font-size:3rem;font-weight:800;background:linear-gradient(135deg,#22d3ee,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;text-align:center;padding:0 40px;line-height:1.12;opacity:0;animation:fI 0.6s 0.5s forwards;">Seed · Reshape · Validate</div>
    <div style="color:#94a3b8;font-size:1.15rem;max-width:760px;text-align:center;line-height:1.55;padding:0 40px;opacity:0;animation:fI 0.6s 0.9s forwards;">A chaotic Monday morning, reshaped into a 1-3-3-0 day plan — live, on a real Azure deployment.</div>
    <div style="margin-top:40px;display:flex;gap:22px;color:#475569;font-size:0.78rem;letter-spacing:2.5px;text-transform:uppercase;opacity:0;animation:fI 0.6s 1.4s forwards;">
      <span>Seed</span><span style="color:#1f1f33;">●</span>
      <span>Flamegraph</span><span style="color:#1f1f33;">●</span>
      <span>Reshape</span><span style="color:#1f1f33;">●</span>
      <span>Check-in</span><span style="color:#1f1f33;">●</span>
      <span>Study</span>
    </div>
    <style>@keyframes fI{to{opacity:1;}}</style>
  `);
  await sceneSettle(s0, 5500);

  // ==========================================================================
  // SCENE 1 — SEED TERMINAL REPLAY (target 17.0s)
  // ==========================================================================
  stamp("Scene 1: seed terminal");
  const s1 = Date.now();
  await showOverlay(page, `
    <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 1 of 6  ·  Seed chaos state</div>
    <div style="font-size:1.55rem;font-weight:800;background:linear-gradient(135deg,#fb923c,#f43f5e);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;text-align:center;">Loading 16 chaotic issues onto roryp/burnout-app</div>
    <div style="width:min(1080px,94vw);height:500px;background:#0a0a18;border:1px solid #1f1f33;border-radius:12px;box-shadow:0 18px 60px rgba(0,0,0,0.6);overflow:hidden;display:flex;flex-direction:column;">
      <div style="display:flex;align-items:center;gap:8px;padding:11px 18px;background:#11112a;border-bottom:1px solid #1f1f33;">
        <span style="width:11px;height:11px;border-radius:50%;background:#ff5f57;"></span>
        <span style="width:11px;height:11px;border-radius:50%;background:#febc2e;"></span>
        <span style="width:11px;height:11px;border-radius:50%;background:#28c840;"></span>
        <span style="margin-left:14px;color:#94a3b8;font-size:0.84rem;font-family:'JetBrains Mono',monospace;">scripts ⟶ seed-demo.sh</span>
      </div>
      <pre id="term-body" style="margin:0;padding:20px 24px;color:#d1d5db;font-family:'JetBrains Mono','Cascadia Code',monospace;font-size:13.5px;line-height:1.55;flex:1;overflow:hidden;white-space:pre-wrap;"></pre>
    </div>
  `);
  // Stream lines progressively — ~12s of typing, then ~2s of static result.
  const beforeData = captured.before;
  await page.evaluate(async ({ score, level, breakdown }) => {
    const body = document.getElementById("term-body");
    const bar = v => v >= 70 ? "████████████" : v >= 40 ? "████████    " : v >= 1 ? "████        " : "            ";
    const lines = [
      ["$ bash scripts/seed-demo.sh $BASE_URL", 400, false],
      ["🔥 Seeding chaotic demo data onto roryp/burnout-app ...", 320, false],
      ["", 180, false],
      ["📦 Step 1/3  ·  Fetching real GitHub issues", 360, false],
      ["   ✓ 16 open issues retrieved from github.com", 280, false],
      ["", 150, false],
      ["📦 Step 2/3  ·  Applying chaos overlay", 360, false],
      ["   ▸ 6 unassigned URGENTs · after-hours timestamps", 280, false],
      ["   ▸ 10 piled on roryp · last-100-min staggered touches", 280, false],
      ["   ▸ all bodies blanked (mystery meat → Clarity hit)", 280, false],
      ["   ✓ Seeded 16 issues into /demo/api/seed", 320, false],
      ["", 150, false],
      ["📦 Step 3/3  ·  Verifying stress for roryp", 360, false],
      [`   Workload      ${bar(breakdown.workload)}  ${breakdown.workload ?? "-"}`, 200, false],
      [`   Chaos         ${bar(breakdown.chaos)}  ${breakdown.chaos ?? "-"}`, 200, false],
      [`   Context       ${bar(breakdown.contextSwitching)}  ${breakdown.contextSwitching ?? "-"}`, 200, false],
      [`   Clarity       ${bar(breakdown.clarity)}  ${breakdown.clarity ?? "-"}`, 200, false],
      [`   After-hours   ${bar(breakdown.afterHours)}  ${breakdown.afterHours ?? "-"}`, 200, false],
      [`   Sustained     ${bar(breakdown.sustainedLoad)}  ${breakdown.sustainedLoad ?? "-"}`, 200, false],
      ["", 200, false],
      [`▶ RESULT  ·  roryp/burnout-app  ·  Stress: ${score} / ${level}`, 400, true],
    ];
    const sleep = ms => new Promise(r => setTimeout(r, ms));
    for (const [text, delay, isResult] of lines) {
      const span = document.createElement("span");
      if (isResult) {
        span.style.cssText = "display:inline-block;margin-top:10px;padding:9px 16px;background:linear-gradient(90deg,rgba(244,63,94,0.20),rgba(251,113,133,0.20));border-left:3px solid #f43f5e;color:#fecaca;font-weight:700;border-radius:5px;";
      }
      span.textContent = text;
      body.appendChild(span);
      body.appendChild(document.createTextNode("\n"));
      await sleep(delay);
    }
    const cursor = document.createElement("span");
    cursor.textContent = " ▌";
    cursor.style.cssText = "color:#22c55e;display:inline-block;";
    body.appendChild(cursor);
    setInterval(() => { cursor.style.opacity = cursor.style.opacity === "0" ? "1" : "0"; }, 500);
  }, beforeData);
  // The terminal stream ends with the cursor blinking — continuous motion.
  await sceneSettle(s1, 17000);

  // ==========================================================================
  // SCENE 2 — BEFORE FLAMEGRAPH (target 16.0s, live page with continuous scroll)
  // The reshape API is kicked off NOW so it can complete during this scene's
  // 16-second hold — eliminating any wait-on-API frozen frames in Scene 3.
  // ==========================================================================
  stamp("Scene 2: BEFORE flamegraph (live)  +  kicking off reshape");
  const s2 = Date.now();
  const reshapePromise = fetchJson(`${BASE}/demo/api/reshape`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, userId: USER }),
  }, 60000).catch(err => { console.error("    reshape error:", err.message); return null; });
  await navigateUnderOverlay(
    page,
    `${BASE}/flamegraph.html?repo=${encodeURIComponent(REPO)}&userId=${USER}`,
    `
      <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 2 of 6  ·  Visualize the chaos</div>
      <div style="font-size:1.9rem;font-weight:800;background:linear-gradient(135deg,#fb923c,#f43f5e);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;">BEFORE flamegraph</div>
      <div style="color:#fda4af;font-size:1.05rem;max-width:680px;text-align:center;line-height:1.55;">Day plan is imbalanced — deferred-heavy, no quick wins, Friday score below 50</div>
    `,
    "text=Deep Work",
  );
  await showCornerBadge(page, "BEFORE", captured.before.stressScore, captured.before.stressLevel, captured.before.fridayScore, true);
  await hideOverlay(page);  // reveal the live flamegraph
  // Continuous slow scrolls during the hold so nothing is ever static.
  await sleep(1500);
  await slowScroll(page, 4500, 500);   // scroll down ~500px over 4.5s
  await sleep(1200);
  await slowScroll(page, 3500, -500);  // scroll back up
  await sceneSettle(s2, 16000);

  // ==========================================================================
  // SCENE 3 — RESHAPE OVERLAY (target 18.0s; reshape API already in flight)
  // The reshape call was kicked off at the start of Scene 2, so by now it is
  // likely already complete. The phase animation + activity loop ensures
  // there is never a frozen frame — even if the API is still in flight.
  // ==========================================================================
  stamp("Scene 3: reshape overlay (awaiting in-flight /demo/api/reshape)");
  const s3 = Date.now();
  await removeCornerBadge(page);
  await showOverlay(page, `
    <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 3 of 6  ·  Reshape the day</div>
    <div style="font-size:1.65rem;font-weight:800;background:linear-gradient(135deg,#22d3ee,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:28px;text-align:center;padding:0 40px;line-height:1.2;">Deterministic pre-pass + LangChain4j supervisor + 1-3-3-0 enforcer</div>

    <div style="display:grid;grid-template-columns:1fr auto 1fr;gap:28px;align-items:stretch;width:min(1080px,94vw);">
      <div style="border-radius:16px;padding:24px;border:1px solid rgba(244,63,94,0.45);background:rgba(244,63,94,0.10);">
        <div style="font-size:0.8rem;letter-spacing:2px;color:#fb7185;font-weight:700;margin-bottom:10px;">BEFORE</div>
        <div style="font-size:3.8rem;font-weight:800;color:#fecaca;line-height:1;">${captured.before.stressScore}</div>
        <div style="font-size:1.05rem;color:#fda4af;margin-top:6px;font-weight:600;">${captured.before.stressLevel}</div>
        <div style="margin-top:20px;height:1px;background:rgba(244,63,94,0.28);"></div>
        <div style="margin-top:14px;display:flex;justify-content:space-between;font-size:0.9rem;color:#cbd5e1;"><span>Friday score</span><span style="font-weight:700;color:#fb7185;">${captured.before.fridayScore ?? "—"}</span></div>
        <div style="margin-top:8px;display:flex;justify-content:space-between;font-size:0.9rem;color:#cbd5e1;"><span>Day plan</span><span style="color:#fda4af;">imbalanced</span></div>
      </div>

      <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;min-width:240px;">
        <div id="phase-1" style="opacity:0.22;font-size:0.9rem;color:#fbbf24;font-family:'JetBrains Mono',monospace;display:flex;align-items:center;gap:10px;transition:opacity 0.4s;"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#fbbf24;"></span>Phase 1 · triage + defuse</div>
        <div id="phase-2" style="opacity:0.22;font-size:0.9rem;color:#22d3ee;font-family:'JetBrains Mono',monospace;display:flex;align-items:center;gap:10px;transition:opacity 0.4s;"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#22d3ee;"></span>Phase 2 · supervisor + 6 agents</div>
        <div id="phase-3" style="opacity:0.22;font-size:0.9rem;color:#a78bfa;font-family:'JetBrains Mono',monospace;display:flex;align-items:center;gap:10px;transition:opacity 0.4s;"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#a78bfa;"></span>Phase 3 · 1-3-3-0 enforcer</div>
        <div style="width:200px;height:6px;background:rgba(255,255,255,0.08);border-radius:3px;overflow:hidden;margin-top:10px;">
          <div id="r-bar" style="width:0%;height:100%;background:linear-gradient(90deg,#fb923c,#22c55e);transition:width 0.7s ease;"></div>
        </div>
        <div id="r-arrow" style="font-size:2.6rem;color:#22c55e;opacity:0;transition:opacity 0.5s;">→</div>
      </div>

      <div id="r-after" style="border-radius:16px;padding:24px;border:1px solid rgba(34,197,94,0.45);background:rgba(34,197,94,0.10);opacity:0.18;transition:opacity 0.7s;">
        <div style="font-size:0.8rem;letter-spacing:2px;color:#86efac;font-weight:700;margin-bottom:10px;">AFTER</div>
        <div id="r-after-score" style="font-size:3.8rem;font-weight:800;color:#bbf7d0;line-height:1;">—</div>
        <div id="r-after-level" style="font-size:1.05rem;color:#86efac;margin-top:6px;font-weight:600;">—</div>
        <div style="margin-top:20px;height:1px;background:rgba(34,197,94,0.28);"></div>
        <div style="margin-top:14px;display:flex;justify-content:space-between;font-size:0.9rem;color:#cbd5e1;"><span>Friday score</span><span id="r-after-friday" style="font-weight:700;color:#86efac;">—</span></div>
        <div style="margin-top:8px;display:flex;justify-content:space-between;font-size:0.9rem;color:#cbd5e1;"><span>Day plan</span><span style="color:#86efac;">1 deep · 3 quick · 3 maint · 0 deferred</span></div>
      </div>
    </div>

    <div id="r-footer" style="margin-top:28px;color:#94a3b8;font-size:0.92rem;opacity:0.72;font-family:'JetBrains Mono',monospace;">Calling /demo/api/reshape …</div>
  `);
  await sleep(900);  // settle in
  await page.evaluate(() => { document.getElementById("r-bar").style.width = "33%"; document.getElementById("phase-1").style.opacity = "1"; });
  await sleep(2400);
  await page.evaluate(() => { document.getElementById("r-bar").style.width = "66%"; document.getElementById("phase-2").style.opacity = "1"; });
  await sleep(2400);
  await page.evaluate(() => { document.getElementById("r-bar").style.width = "100%"; document.getElementById("phase-3").style.opacity = "1"; });
  await sleep(1800);
  await page.evaluate(() => { document.getElementById("r-arrow").style.opacity = "1"; });
  await sleep(600);

  // If reshape is still in flight, rotate footer messages so the screen
  // keeps moving instead of freezing. Stops as soon as the promise resolves.
  let reshape = null;
  let reshapeDone = false;
  reshapePromise.then(r => { reshape = r; reshapeDone = true; });
  const supervisorMessages = [
    "Supervisor → invoking TriageAgent ...",
    "Supervisor → invoking ClassifyAgent ...",
    "Supervisor → invoking DeferAgent ...",
    "Supervisor → invoking DelegateAgent ...",
    "Supervisor → invoking ScopeAgent ...",
    "Supervisor → invoking WellnessAgent ...",
    "1-3-3-0 enforcer → promoting deferred ...",
    "1-3-3-0 enforcer → pushing overflow ...",
  ];
  let msgIdx = 0;
  while (!reshapeDone) {
    await page.evaluate((text) => {
      const f = document.getElementById("r-footer");
      if (f) f.textContent = text;
    }, supervisorMessages[msgIdx % supervisorMessages.length]);
    msgIdx++;
    await sleep(900);
  }
  let afterResult;
  if (reshape) {
    afterResult = {
      stressScore: reshape.afterScore,
      stressLevel: reshape.afterLevel,
      actionsApplied: reshape.actionsApplied,
      llmUsed: reshape.llmUsed,
      fridayScore: reshape.fridayScore,
      fridayStatus: reshape.fridayStatus,
      triage: reshape.deterministicTriageCount ?? 0,
      defuse: reshape.deterministicDefuseCount ?? 0,
    };
    console.log(`    [reshape] AFTER ${afterResult.stressScore}/${afterResult.stressLevel}  fri=${afterResult.fridayScore}  actions=${afterResult.actionsApplied}  llm=${afterResult.llmUsed}`);
  } else {
    afterResult = { stressScore: "—", stressLevel: "—", actionsApplied: 0, llmUsed: false, fridayScore: null, fridayStatus: "", triage: 0, defuse: 0 };
  }
  await page.evaluate(({ a }) => {
    document.getElementById("r-after").style.opacity = "1";
    document.getElementById("r-after-score").textContent = a.stressScore;
    document.getElementById("r-after-level").textContent = a.stressLevel;
    document.getElementById("r-after-friday").textContent = a.fridayScore ?? "—";
    document.getElementById("r-footer").textContent = `${a.actionsApplied} actions applied · LLM ${a.llmUsed ? "active" : "offline (deterministic fallback)"} · triage ${a.triage} · defuse ${a.defuse}`;
    document.getElementById("r-footer").style.opacity = "1";
  }, { a: afterResult });
  await sceneSettle(s3, 18000);

  // ==========================================================================
  // SCENE 4 — AFTER FLAMEGRAPH (target 16.0s)
  // ==========================================================================
  stamp("Scene 4: AFTER flamegraph (live)");
  const s4 = Date.now();
  await navigateUnderOverlay(
    page,
    `${BASE}/flamegraph.html?repo=${encodeURIComponent(REPO)}&userId=${USER}`,
    `
      <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 4 of 6  ·  Verify the rebalance</div>
      <div style="font-size:1.9rem;font-weight:800;background:linear-gradient(135deg,#22c55e,#0ea5e9);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;">AFTER flamegraph</div>
      <div style="color:#86efac;font-size:1.05rem;max-width:680px;text-align:center;line-height:1.55;">1 deep work  ·  3 quick wins  ·  3 maintenance  ·  0 deferred</div>
    `,
    "text=Deep Work",
  );
  await showCornerBadge(page, "AFTER", afterResult.stressScore, afterResult.stressLevel, afterResult.fridayScore, false);
  await hideOverlay(page);
  await sleep(1500);
  await slowScroll(page, 4500, 500);
  await sleep(1200);
  await slowScroll(page, 3500, -500);
  await sceneSettle(s4, 16000);

  // ==========================================================================
  // SCENE 5 — CHECK-IN (target 17.0s)
  // ==========================================================================
  stamp("Scene 5: live check-in");
  const s5 = Date.now();
  await removeCornerBadge(page);
  await navigateUnderOverlay(
    page,
    `${BASE}/checkin.html`,
    `
      <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 5 of 6  ·  Verify the stress drop</div>
      <div style="font-size:1.9rem;font-weight:800;background:linear-gradient(135deg,#22c55e,#0ea5e9);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;">Stress check-in</div>
      <div style="color:#86efac;font-size:1.05rem;max-width:680px;text-align:center;line-height:1.55;">MODERATE — chaos defused, real after-hours preserved</div>
    `,
    "#userId",
  );
  await hideOverlay(page);

  // Live form fill + checkin call
  await page.fill("#userId", "");
  await page.type("#userId", USER, { delay: 65 });
  await page.fill("#repo", "");
  await page.type("#repo", REPO, { delay: 45 });
  await sleep(500);
  await page.click("#checkin-btn");
  await page.waitForSelector("#result-card", { state: "visible", timeout: 12000 }).catch(() => {});
  await sleep(1500);

  await showCornerBadge(page, "AFTER", afterResult.stressScore, afterResult.stressLevel, afterResult.fridayScore, false);
  await sleep(2200);

  // Expand drilldown for the issue list beat
  const toggles = await page.$$(".issue-toggle");
  let opened = false;
  for (const t of toggles) {
    const txt = await t.textContent().catch(() => "");
    if (txt && /issue/i.test(txt)) {
      await t.click().catch(() => {});
      opened = true;
      break;
    }
  }
  if (!opened && toggles[0]) await toggles[0].click().catch(() => {});

  await sleep(2500);
  await slowScroll(page, 4000, 300);
  await sceneSettle(s5, 17000);

  // ==========================================================================
  // SCENE 6 — STUDY DASHBOARD (target 28.0s, 4 sections × ~6.5s each)
  // ==========================================================================
  stamp("Scene 6: study dashboard");
  const s6 = Date.now();
  await removeCornerBadge(page);
  await navigateUnderOverlay(
    page,
    `${BASE}/study.html`,
    `
      <div style="font-size:0.85rem;letter-spacing:3px;color:#94a3b8;text-transform:uppercase;font-weight:700;margin-bottom:14px;">Stage 6 of 6  ·  Team dashboard</div>
      <div style="font-size:1.9rem;font-weight:800;background:linear-gradient(135deg,#a78bfa,#22d3ee);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:20px;">Cohort trend</div>
      <div style="color:#cbd5e1;font-size:1.05rem;max-width:680px;text-align:center;line-height:1.55;">Researchers see the whole team — Friday score ${captured.before.fridayScore ?? "—"} → ${afterResult.fridayScore ?? "—"} ${afterResult.fridayStatus ? afterResult.fridayStatus.split(" — ")[0] : ""}</div>
    `,
    "#load-btn",
  );

  // Inject the Friday-score card above the summary BEFORE we fade overlay,
  // so when overlay clears the dashboard already has its enriched header.
  await page.evaluate(({ bf, af, status }) => {
    const summary = document.getElementById("summary");
    if (!summary) return;
    document.getElementById("injected-friday-card")?.remove();
    const card = document.createElement("div");
    card.id = "injected-friday-card";
    card.style.cssText = "display:flex;align-items:stretch;gap:18px;padding:22px 26px;margin-bottom:20px;border-radius:14px;background:linear-gradient(135deg,rgba(34,197,94,0.12),rgba(14,165,233,0.12));border:1px solid rgba(34,197,94,0.40);box-shadow:0 10px 32px rgba(0,0,0,0.4);";
    card.innerHTML = `
      <div style="display:flex;flex-direction:column;justify-content:center;">
        <div style="font-size:0.78rem;letter-spacing:2px;text-transform:uppercase;color:#94a3b8;font-weight:700;">Friday deploy readiness  ·  roryp/burnout-app</div>
        <div style="display:flex;align-items:baseline;gap:14px;margin-top:8px;">
          <span style="font-size:2.8rem;font-weight:800;color:#86efac;">${af ?? "—"}</span>
          <span style="font-size:1.05rem;color:#cbd5e1;">/ 100</span>
          <span style="font-size:1rem;color:#86efac;font-weight:600;margin-left:10px;">${status || ""}</span>
        </div>
      </div>
      <div style="flex:1;display:flex;align-items:center;justify-content:flex-end;gap:20px;">
        <div style="text-align:right;">
          <div style="font-size:0.78rem;color:#94a3b8;letter-spacing:1.5px;text-transform:uppercase;">Before reshape</div>
          <div style="font-size:1.4rem;font-weight:700;color:#fb7185;">${bf ?? "—"}</div>
        </div>
        <div style="font-size:1.9rem;color:#22d3ee;">→</div>
        <div style="text-align:right;">
          <div style="font-size:0.78rem;color:#94a3b8;letter-spacing:1.5px;text-transform:uppercase;">After reshape</div>
          <div style="font-size:1.4rem;font-weight:700;color:#86efac;">${af ?? "—"}</div>
        </div>
      </div>
    `;
    summary.parentNode.insertBefore(card, summary);
  }, { bf: captured.before.fridayScore, af: afterResult.fridayScore, status: afterResult.fridayStatus || "" });

  await hideOverlay(page);
  await sleep(800);
  await page.click("#load-btn").catch(() => {});
  await page.waitForSelector("#summary", { state: "visible", timeout: 12000 }).catch(() => {});
  await sleep(1500);

  // 4 sections with continuous scroll motion between them.
  await sleep(5000);  // hold on Friday card + summary metrics
  await page.evaluate(() => document.getElementById("chart-section")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  await sleep(6500);  // hold on trend chart
  await page.evaluate(() => document.getElementById("participants-section")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  await sleep(6500);  // hold on participant grid
  await page.evaluate(() => document.getElementById("table-section")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  await sceneSettle(s6, 28000);  // hard land — gives table at least ~6s

  // ==========================================================================
  // FINALIZE
  // ==========================================================================
  stamp("done — closing browser, finalizing video");
  await page.close();
  await context.close();
  await browser.close();

  if (fs.existsSync(FINAL)) fs.unlinkSync(FINAL);
  const files = fs.readdirSync(OUT_DIR).filter(f => f.endsWith(".webm") && f !== path.basename(FINAL));
  if (files.length === 0) {
    console.error("❌ no video file produced");
    process.exit(1);
  }
  files.sort((a, b) => fs.statSync(path.join(OUT_DIR, b)).mtimeMs - fs.statSync(path.join(OUT_DIR, a)).mtimeMs);
  fs.renameSync(path.join(OUT_DIR, files[0]), FINAL);

  const stats = fs.statSync(FINAL);
  const total = ((Date.now() - t0) / 1000).toFixed(1);
  console.log(`\n✅ saved ${FINAL}`);
  console.log(`   size:      ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
  console.log(`   recording: ~${total} s`);
  console.log(`   BEFORE:    ${captured.before.stressScore}/${captured.before.stressLevel}  fri ${captured.before.fridayScore ?? "—"}`);
  console.log(`   AFTER:     ${afterResult.stressScore}/${afterResult.stressLevel}  fri ${afterResult.fridayScore} (${afterResult.fridayStatus})`);
  console.log(`   actions:   ${afterResult.actionsApplied}   llmUsed=${afterResult.llmUsed}`);
}

main().catch(err => {
  console.error("\n❌ recording failed:", err);
  process.exit(1);
});
