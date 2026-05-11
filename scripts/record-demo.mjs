/**
 * Record a ~75-second demo video of the REAL Burnout-as-a-Service pipeline:
 *   Landing -> BEFORE check-in (HIGH) -> BEFORE flamegraph
 *           -> RESHAPE (deterministic pre-pass + LangChain4j supervisor)
 *           -> AFTER check-in (MODERATE) -> AFTER flamegraph (1-3-3-0 compliant)
 *           -> Study Dashboard
 *
 * NOT the old "swap-the-data" sync trick. The score drop is driven by the
 * actual /demo/api/reshape endpoint running against the seeded chaos. The
 * pre-pass intentionally leaves real after-hours / recent-touch timestamps
 * intact (acknowledge-don't-erase) so the AFTER score still reflects genuine
 * human activity — the WellnessAgent uses that signal to recommend a break.
 *
 * Holds are long (6-8s per scene) so you can narrate each step live.
 *
 * Usage:
 *   node scripts/record-demo.mjs [base-url]
 *
 * Outputs:
 *   docs/images/demo/demo-pipeline.webm
 */

import { chromium } from "playwright";
import fs from "fs";
import path from "path";

const BASE = process.argv[2] || "https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io";
const REPO = "roryp/burnout-app";
const USER = "roryp";
const OUT_DIR = path.resolve("docs/images/demo");

fs.mkdirSync(OUT_DIR, { recursive: true });

const sleep = ms => new Promise(r => setTimeout(r, ms));

const TITLE_OVERLAY_CSS = "position:fixed;inset:0;background:rgba(0,0,0,0.92);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;transition:opacity 0.6s;";
const TITLE_HEAD_STYLE = "font-size:2.4rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:16px;text-align:center;padding:0 40px;";
const TITLE_SUB_STYLE = "color:#94a3b8;font-size:1.15rem;max-width:600px;text-align:center;line-height:1.5;padding:0 40px;";

async function main() {
  // --- Pre-record: build chaos issues (real GitHub titles + chaos overlay) ---
  console.log("Pre-record: building chaos issues...");
  const now = new Date();
  const fmt = d => d.toISOString().replace(/\.\d+Z$/, "Z");
  const ago = mins => fmt(new Date(now.getTime() - mins * 60000));
  const daysAgo = d => fmt(new Date(now.getTime() - d * 86400000));
  const hourUTC = h => { const d = new Date(now); d.setUTCHours(h, 0, 0, 0); return fmt(d); };
  const recent = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100].map(ago);
  const afterHours = [hourUTC(3), hourUTC(4), hourUTC(22)];

  let realTitles = [];
  try {
    const ghResp = await fetch("https://api.github.com/repos/" + REPO + "/issues?state=open&per_page=30", {
      headers: { "Accept": "application/vnd.github+json", "User-Agent": "burnout-app-demo" },
    });
    if (ghResp.ok) {
      const all = await ghResp.json();
      realTitles = all.filter(i => !i.pull_request).slice(0, 16);
    }
  } catch {}
  if (realTitles.length < 4) {
    console.log("  GitHub unavailable, using synthetic titles");
    realTitles = [
      { number: 1, title: "Critical auth bypass in OAuth flow", labels: [{ name: "priority:critical" }, { name: "security" }] },
      { number: 2, title: "URGENT: Production memory leak", labels: [{ name: "bug" }] },
      { number: 3, title: "URGENT: API rate limiting broken", labels: [{ name: "bug" }] },
      { number: 4, title: "URGENT: Database connection pool exhaustion", labels: [{ name: "priority:critical" }] },
      { number: 5, title: "Refactor agent orchestration layer", labels: [{ name: "architecture" }] },
      { number: 6, title: "Implement new feature flags system", labels: [{ name: "epic" }, { name: "feature" }] },
      { number: 7, title: "Fix typo in README", labels: [{ name: "good-first-issue" }] },
      { number: 8, title: "Update Spring Boot to 3.5.11", labels: [{ name: "dependencies" }] },
      { number: 9, title: "Something unclear", labels: [{ name: "bug" }] },
      { number: 10, title: "Another vague issue", labels: [{ name: "bug" }] },
      { number: 11, title: "CI pipeline failing intermittently", labels: [{ name: "ci" }] },
      { number: 12, title: "Write API documentation", labels: [{ name: "documentation" }] },
      { number: 13, title: "Add dark mode toggle", labels: [{ name: "enhancement" }] },
      { number: 14, title: "Fix CORS headers on demo endpoints", labels: [{ name: "bug" }] },
      { number: 15, title: "Stale tracking issue from last quarter", labels: [{ name: "triage" }] },
      { number: 16, title: "Upgrade Node.js to v22", labels: [{ name: "dependencies" }] },
    ];
  }

  const chaosIssues = realTitles.map((src, i) => {
    const isUrgent = i < 6;
    const labels = (src.labels || []).map(l => ({ name: typeof l === "string" ? l : l.name }));
    if (isUrgent) {
      labels.push({ name: "urgent" });
      labels.push({ name: "priority:critical" });
    }
    return {
      number: src.number,
      title: src.title,
      body: "",
      labels,
      assignees: isUrgent ? [] : [{ login: USER }],
      createdAt: isUrgent ? daysAgo(30) : daysAgo(7),
      updatedAt: isUrgent ? afterHours[i % afterHours.length] : recent[(i - 6) % recent.length],
      state: "open",
    };
  });

  console.log("Pre-record: resetting and seeding study history...");
  await fetch(BASE + "/demo/api/study/reset", { method: "DELETE" }).catch(() => {});
  const studyResp = await fetch(BASE + "/demo/api/study/seed", { method: "POST" });
  if (studyResp.ok) {
    const sd = await studyResp.json();
    console.log("  Seeded " + sd.seeded + " fresh snapshots for " + sd.users.join(", "));
  }

  console.log("Pre-record: seeding chaotic BEFORE state...");
  const seedResp = await fetch(BASE + "/demo/api/seed", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, issues: chaosIssues }),
  });
  if (seedResp.ok) {
    const seedData = await seedResp.json();
    console.log("  Seeded " + seedData.issueCount + " issues");
  }

  const verifyResp = await fetch(BASE + "/demo/api/checkin", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: USER, repo: REPO, selfScore: 50 }),
  });
  if (verifyResp.ok) {
    const v = await verifyResp.json();
    console.log("  Verified BEFORE: stress=" + v.stressScore + " (" + v.stressLevel + ")");
  }

  // Re-seed because the verification checkin may have refreshed cache state
  await fetch(BASE + "/demo/api/seed", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, issues: chaosIssues }),
  });

  // --- Launch browser with video recording ---
  console.log("\nLaunching browser with video recording...");
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    recordVideo: { dir: OUT_DIR, size: { width: 1280, height: 720 } },
  });
  const page = await context.newPage();

  async function humanType(selector, text) {
    await page.click(selector);
    await page.type(selector, text, { delay: 50 });
  }

  async function showTitleThenNavigate(url, title, subtitle, holdMs = 3000, waitForSelector = null) {
    await page.evaluate(([t, s, ovCss, hStyle, sStyle]) => {
      const el = document.createElement("div");
      el.id = "scene-title";
      el.style.cssText = ovCss;
      el.innerHTML = '<div style="' + hStyle + '">' + t + '</div><div style="' + sStyle + '">' + s + '</div>';
      document.body.appendChild(el);
    }, [title, subtitle, TITLE_OVERLAY_CSS, TITLE_HEAD_STYLE, TITLE_SUB_STYLE]);
    await sleep(holdMs);

    await page.goto(url, { waitUntil: "domcontentloaded" });
    await page.evaluate(() => {
      const cover = document.createElement("div");
      cover.id = "page-cover";
      cover.style.cssText = "position:fixed;inset:0;background:#0a0a1a;z-index:9998;transition:opacity 0.6s;";
      document.body.appendChild(cover);
    });

    if (waitForSelector) {
      await page.waitForSelector(waitForSelector, { timeout: 15000 }).catch(() => {});
    } else {
      await page.waitForLoadState("networkidle");
    }
    await sleep(700);

    await page.evaluate(() => {
      const cover = document.getElementById("page-cover");
      if (cover) cover.style.opacity = "0";
    });
    await sleep(600);
    await page.evaluate(() => document.getElementById("page-cover")?.remove());
  }

  // --- Scene 0: Landing Page ---
  console.log("\nScene 0: Landing page (6s hold for narration)...");
  await page.goto(BASE + "/");
  await page.waitForLoadState("networkidle");
  await sleep(6000);

  // --- Scene 1: BEFORE Check-in ---
  console.log("Scene 1: BEFORE check-in (~58 / HIGH)...");
  await showTitleThenNavigate(
    BASE + "/checkin.html",
    "Step 1: Stress Check-In",
    "Monday morning. Roryp checks his burnout score on his repo.",
    3500,
    "#userId",
  );
  await humanType("#userId", USER);
  await sleep(400);
  await humanType("#repo", REPO);
  await sleep(600);
  await page.click("#checkin-btn");

  await page.waitForSelector("#result-card", { state: "visible", timeout: 15000 });
  await sleep(8000); // narration: "HIGH. Workload, Chaos, Context Switching, After-Hours all firing."

  const beforeToggles = await page.$$(".issue-toggle");
  for (const toggle of beforeToggles) {
    const txt = await toggle.textContent();
    if (txt && txt.toLowerCase().includes("issue")) { await toggle.click(); break; }
  }
  await sleep(8000); // narration: walk through the issue list

  // --- Scene 2: BEFORE Flamegraph ---
  console.log("Scene 2: BEFORE flamegraph (imbalanced)...");
  await showTitleThenNavigate(
    BASE + "/flamegraph.html?repo=" + REPO + "&userId=" + USER,
    "Step 2: View the Flamegraph",
    "Imbalanced workload — no quick wins, mostly deferred",
    3500,
    "text=Deep Work",
  );
  await sleep(8000); // narration: "1 deep, 0 quick wins, 0 maintenance, 9 deferred. Friday score below 50 — NOT_READY."

  // --- Scene 3: Reshape transition ---
  console.log("Scene 3: Calling reshape (deterministic pre-pass + supervisor)...");
  await page.evaluate(() => {
    const overlay = document.createElement("div");
    overlay.id = "reshape-overlay";
    overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,0.92);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;";
    overlay.innerHTML = [
      '<div style="font-size:2.6rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:18px;">Reshaping Your Day...</div>',
      '<div style="color:#fbbf24;font-size:1.05rem;max-width:600px;text-align:center;line-height:1.6;margin-bottom:8px;"><strong>Phase 1:</strong> Deterministic pre-pass triages unassigned URGENTs and defuses chaos inputs</div>',
      '<div style="color:#22d3ee;font-size:1.05rem;max-width:600px;text-align:center;line-height:1.6;"><strong>Phase 2:</strong> LangChain4j supervisor coordinates 6 sub-agents to build a 3-3-3 plan</div>',
      '<div style="margin-top:30px;width:300px;height:5px;background:rgba(255,255,255,0.1);border-radius:3px;overflow:hidden;"><div id="reshape-bar" style="width:0%;height:100%;background:linear-gradient(90deg,#00f260,#0575e6);transition:width 7s ease-in-out;"></div></div>',
    ].join("");
    document.body.appendChild(overlay);
    requestAnimationFrame(() => { document.getElementById("reshape-bar").style.width = "100%"; });
  });

  const reshapeStart = Date.now();
  const reshapeResp = await fetch(BASE + "/demo/api/reshape", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, userId: USER }),
  });
  let reshapeReport = null;
  if (reshapeResp.ok) {
    reshapeReport = await reshapeResp.json();
    console.log("  Reshape: before=" + reshapeReport.beforeScore + " -> after=" + reshapeReport.afterScore + " (" + reshapeReport.afterLevel + ")");
    console.log("  Actions: " + reshapeReport.actionsApplied + ", LLM: " + reshapeReport.llmUsed);
  } else {
    console.log("  Reshape failed: " + reshapeResp.status);
  }
  const elapsed = Date.now() - reshapeStart;
  if (elapsed < 7500) await sleep(7500 - elapsed);

  await page.evaluate(() => {
    const el = document.getElementById("reshape-overlay");
    if (el) { el.style.transition = "opacity 0.6s"; el.style.opacity = "0"; }
  });
  await sleep(700);
  await page.evaluate(() => document.getElementById("reshape-overlay")?.remove());

  // --- Scene 4: AFTER Check-in ---
  console.log("Scene 4: AFTER check-in (MODERATE — after-hours preserved)...");
  await showTitleThenNavigate(
    BASE + "/checkin.html",
    "Step 3: Check Stress Again",
    "Same developer, same repo — same day, but reshaped",
    3500,
    "#userId",
  );
  await page.fill("#userId", USER);
  await page.fill("#repo", REPO);
  await sleep(500);
  await page.click("#checkin-btn");

  await page.waitForSelector("#result-card", { state: "visible", timeout: 15000 });
  await sleep(8000); // narration: "MODERATE. Compliant. Chaos defused. After-hours kept — that's real activity."

  const afterToggles = await page.$$(".issue-toggle");
  for (const toggle of afterToggles) {
    const txt = await toggle.textContent();
    if (txt && txt.toLowerCase().includes("issue")) { await toggle.click(); break; }
  }
  await sleep(8000); // narration: same issues, now triaged + scoped + classified

  // --- Scene 5: AFTER Flamegraph ---
  console.log("Scene 5: AFTER flamegraph (3-3-3 compliant)...");
  await showTitleThenNavigate(
    BASE + "/flamegraph.html?repo=" + REPO + "&userId=" + USER,
    "Step 4: Balanced Flamegraph",
    "3-3-3 structure: 1 deep work, up to 3 quick wins, up to 3 maintenance",
    3500,
    "text=Deep Work",
  );
  await sleep(8000); // narration: "1 deep, 3 quick wins, 3 maintenance, 0 deferred. Friday score ≥ 80 — READY."

  // --- Scene 6: Study Dashboard ---
  console.log("Scene 6: Team study dashboard...");
  await showTitleThenNavigate(
    BASE + "/study.html",
    "Step 5: Team Dashboard",
    "Researchers see the cohort trend across all participants",
    3500,
    "#load-btn",
  );
  await page.click("#load-btn");
  await page.waitForSelector("#summary", { state: "visible", timeout: 15000 }).catch(() => {});
  await sleep(6000); // narration: "5 participants, ~118 snapshots, see the trend chart"

  await page.evaluate(() => {
    const grid = document.getElementById("participants-section");
    if (grid) grid.scrollIntoView({ behavior: "smooth", block: "start" });
  });
  await sleep(6000); // narration: "click any participant to drill in"

  // --- Done ---
  console.log("\nFinalizing video...");
  await page.close();
  await context.close();
  await browser.close();

  const dest = path.join(OUT_DIR, "demo-pipeline.webm");
  if (fs.existsSync(dest)) fs.unlinkSync(dest);
  const files = fs.readdirSync(OUT_DIR).filter(f => f.endsWith(".webm") && f !== "demo-pipeline.webm");
  if (files.length > 0) {
    const latest = files.sort().pop();
    fs.renameSync(path.join(OUT_DIR, latest), dest);
    const stats = fs.statSync(dest);
    console.log("Video saved: " + dest);
    console.log("Size: " + (stats.size / 1024 / 1024).toFixed(1) + " MB");
    if (reshapeReport) {
      console.log("\nReshape captured in video:");
      console.log("  before=" + reshapeReport.beforeScore + " -> after=" + reshapeReport.afterScore + " (" + reshapeReport.afterLevel + ")");
      console.log("  actionsApplied=" + reshapeReport.actionsApplied + ", llmUsed=" + reshapeReport.llmUsed);
    }
  } else {
    console.error("No video file generated!");
  }
}

main().catch(err => {
  console.error("Recording failed:", err);
  process.exit(1);
});
