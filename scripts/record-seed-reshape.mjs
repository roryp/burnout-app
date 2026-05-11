/**
 * Recording: stress checkin BEFORE -> flamegraph BEFORE -> reshape ->
 *            stress checkin AFTER -> flamegraph AFTER -> study dashboard.
 *
 * Self-seeds the chaotic BEFORE state (real GH titles + chaos overlay)
 * and the study history before recording. No external seed script needed.
 * Every scene holds for ~8 seconds max — no lingering.
 *
 * Usage:
 *   node scripts/record-seed-reshape.mjs [base-url]
 *
 * Output:
 *   docs/images/demo/seed-reshape.webm
 */

import { chromium } from "playwright";
import fs from "fs";
import path from "path";

const BASE = process.argv[2] || "https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io";
const REPO = "roryp/burnout-app";
const USER = "roryp";
const OUT_DIR = path.resolve("docs/images/demo");
const CHECKIN_URL = BASE + "/checkin.html";
const FLAMEGRAPH_URL = BASE + "/flamegraph.html?repo=" + REPO + "&userId=" + USER;
const STUDY_URL = BASE + "/study.html";

const SCENE_HOLD = 7000;       // 7s of clean page content per scene
const INTRO_HOLD = 3500;       // 3.5s for opening/closing title cards
const CAPTION_HOLD = 1500;     // 1.5s for caption banners on top of page

fs.mkdirSync(OUT_DIR, { recursive: true });

const sleep = ms => new Promise(r => setTimeout(r, ms));

const TITLE_OVERLAY_CSS = "position:fixed;inset:0;background:rgba(0,0,0,0.92);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;transition:opacity 0.5s;";
const TITLE_HEAD_STYLE = "font-size:2.4rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:16px;text-align:center;padding:0 40px;";
const TITLE_SUB_STYLE = "color:#94a3b8;font-size:1.15rem;max-width:700px;text-align:center;line-height:1.5;padding:0 40px;";
const TITLE_KICKER_STYLE = "color:#fbbf24;font-size:0.95rem;margin-top:14px;text-align:center;padding:0 40px;letter-spacing:0.5px;text-transform:uppercase;";

async function showTitle(page, title, subtitle, kicker, holdMs) {
  await page.evaluate(([t, s, k, ovCss, hStyle, sStyle, kStyle]) => {
    document.getElementById("scene-title")?.remove();
    const el = document.createElement("div");
    el.id = "scene-title";
    el.style.cssText = ovCss;
    el.innerHTML = '<div style="' + hStyle + '">' + t + '</div><div style="' + sStyle + '">' + s + '</div>' + (k ? '<div style="' + kStyle + '">' + k + '</div>' : "");
    document.body.appendChild(el);
  }, [title, subtitle, kicker || "", TITLE_OVERLAY_CSS, TITLE_HEAD_STYLE, TITLE_SUB_STYLE, TITLE_KICKER_STYLE]);
  await sleep(holdMs);
}

async function fadeOutTitle(page) {
  await page.evaluate(() => {
    const el = document.getElementById("scene-title");
    if (el) { el.style.opacity = "0"; }
  });
  await sleep(500);
  await page.evaluate(() => document.getElementById("scene-title")?.remove());
}

/** Brief caption banner that appears on top of the page (not blocking it). */
async function showCaption(page, text, isBefore) {
  const accent = isBefore ? "#ef4444" : "#22c55e";
  await page.evaluate(([t, acc]) => {
    document.getElementById("scene-caption")?.remove();
    const el = document.createElement("div");
    el.id = "scene-caption";
    el.style.cssText = "position:fixed;top:18px;left:18px;background:rgba(10,10,26,0.92);color:#fff;padding:10px 18px;border-radius:10px;font-weight:700;font-size:1.05rem;z-index:9998;box-shadow:0 4px 14px rgba(0,0,0,0.5);border-left:4px solid " + acc + ";max-width:540px;line-height:1.4;transition:opacity 0.5s;";
    el.textContent = t;
    document.body.appendChild(el);
  }, [text, accent]);
}

async function fadeCaption(page) {
  await page.evaluate(() => {
    const el = document.getElementById("scene-caption");
    if (el) { el.style.opacity = "0"; }
  });
  await sleep(500);
  await page.evaluate(() => document.getElementById("scene-caption")?.remove());
}

async function runCheckin(page) {
  // Robustly fill the form even if it was pre-filled by previous navigation
  await page.waitForSelector("#userId", { timeout: 15000 });
  await page.fill("#userId", "");
  await page.type("#userId", USER, { delay: 45 });
  await page.fill("#repo", "");
  await page.type("#repo", REPO, { delay: 35 });
  await sleep(400);
  await page.click("#checkin-btn");
  await page.waitForSelector("#result-card", { state: "visible", timeout: 20000 }).catch(() => {});
  await sleep(1200); // let metrics animate in
}

async function addCornerBadge(page, label, score, level, isBefore) {
  const bg = isBefore ? "rgba(239,68,68,0.95)" : "rgba(34,197,94,0.95)";
  const fg = isBefore ? "#fff" : "#0a0a1a";
  await page.evaluate(([l, s, lv, bgCol, fgCol]) => {
    document.getElementById("state-badge")?.remove();
    const badge = document.createElement("div");
    badge.id = "state-badge";
    badge.style.cssText = "position:fixed;top:18px;right:18px;background:" + bgCol + ";color:" + fgCol + ";padding:10px 18px;border-radius:10px;font-weight:800;font-size:1.05rem;z-index:9998;box-shadow:0 4px 14px rgba(0,0,0,0.5);letter-spacing:0.5px;";
    badge.textContent = l + "  \u00b7  " + s + " / " + lv;
    document.body.appendChild(badge);
  }, [label, score, level, bg, fg]);
}

async function removeCornerBadge(page) {
  await page.evaluate(() => document.getElementById("state-badge")?.remove());
}

async function main() {
  // --- Pre-record: seed chaotic BEFORE state directly (real GH titles + chaos overlay) ---
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
  const studyResp = await fetch(BASE + "/demo/api/study/seed", { method: "POST" }).catch(() => null);
  if (studyResp && studyResp.ok) {
    const sd = await studyResp.json();
    console.log("  Seeded " + sd.seeded + " fresh study snapshots for " + sd.users.join(", "));
  }

  console.log("Pre-record: seeding chaotic BEFORE state...");
  const seedResp = await fetch(BASE + "/demo/api/seed", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repo: REPO, issues: chaosIssues }),
  });
  if (!seedResp.ok) {
    console.error("Seed failed: " + seedResp.status);
    process.exit(1);
  }
  const seedData = await seedResp.json();
  console.log("  Seeded " + (seedData.issueCount ?? "?") + " issues");

  // --- Verify BEFORE state via API ---
  console.log("Verifying seeded BEFORE state...");
  const verifyResp = await fetch(BASE + "/demo/api/checkin", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: USER, repo: REPO, selfScore: 50 }),
  });
  if (!verifyResp.ok) {
    console.error("BEFORE checkin failed: " + verifyResp.status);
    process.exit(1);
  }
  const before = await verifyResp.json();
  console.log("  BEFORE stress=" + before.stressScore + " (" + before.stressLevel + ")");

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

  // --- Scene 1: Intro card ---
  console.log("\nScene 1: Intro card (3.5s)...");
  await page.goto("about:blank");
  await page.evaluate(() => { document.body.style.background = "#0a0a1a"; });
  await showTitle(
    page,
    "Seed \u2192 Reshape",
    "16 chaotic issues seeded on roryp/burnout-app \u2014 score 58 / HIGH",
    "Burnout-as-a-Service demo",
    INTRO_HOLD,
  );

  // --- Scene 2: BEFORE Stress Check-in (~9s total: form fill + 7s view) ---
  console.log("Scene 2: BEFORE stress check-in (~" + before.stressScore + " / " + before.stressLevel + ") ...");
  await page.goto(CHECKIN_URL, { waitUntil: "domcontentloaded" });
  await page.waitForLoadState("networkidle").catch(() => {});
  await fadeOutTitle(page);
  await runCheckin(page);
  await addCornerBadge(page, "BEFORE", before.stressScore, before.stressLevel, true);
  await showCaption(page, "Step 1: Stress check-in \u2014 BEFORE reshape", true);
  await sleep(CAPTION_HOLD);
  await fadeCaption(page);
  await sleep(SCENE_HOLD - CAPTION_HOLD);

  // --- Scene 3: BEFORE Flamegraph (~8.5s total: nav + caption + view) ---
  console.log("Scene 3: BEFORE flamegraph...");
  await removeCornerBadge(page);
  await page.goto(FLAMEGRAPH_URL, { waitUntil: "domcontentloaded" });
  await page.waitForLoadState("networkidle").catch(() => {});
  await page.waitForSelector("text=Deep Work", { timeout: 15000 }).catch(() => {});
  await addCornerBadge(page, "BEFORE", before.stressScore, before.stressLevel, true);
  await showCaption(page, "Step 2: Flamegraph \u2014 imbalanced day plan, heavy deferred bucket", true);
  await sleep(CAPTION_HOLD);
  await fadeCaption(page);
  await sleep(SCENE_HOLD - CAPTION_HOLD);

  // --- Scene 4: Reshape transition (real API call, ~7s) ---
  console.log("Scene 4: Calling /demo/api/reshape ...");
  await removeCornerBadge(page);
  await page.evaluate(() => {
    const overlay = document.createElement("div");
    overlay.id = "reshape-overlay";
    overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,0.94);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:9999;";
    overlay.innerHTML = [
      '<div style="font-size:2.4rem;font-weight:800;background:linear-gradient(135deg,#00f260,#0575e6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:22px;">Reshaping Your Day...</div>',
      '<div style="color:#fbbf24;font-size:1rem;max-width:660px;text-align:center;line-height:1.5;margin-bottom:8px;"><strong>Phase 1:</strong> Deterministic pre-pass triages URGENTs and defuses chaos inputs</div>',
      '<div style="color:#22d3ee;font-size:1rem;max-width:660px;text-align:center;line-height:1.5;margin-bottom:8px;"><strong>Phase 2:</strong> LangChain4j supervisor + 6 sub-agents build the 1-3-3 plan</div>',
      '<div style="color:#a78bfa;font-size:1rem;max-width:660px;text-align:center;line-height:1.5;"><strong>Phase 3:</strong> 1-3-3-0 enforcer rebalances overflow off the plate</div>',
      '<div style="margin-top:28px;width:340px;height:5px;background:rgba(255,255,255,0.1);border-radius:3px;overflow:hidden;"><div id="reshape-bar" style="width:0%;height:100%;background:linear-gradient(90deg,#00f260,#0575e6);transition:width 6.5s ease-in-out;"></div></div>',
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
  let reshape = null;
  if (reshapeResp.ok) {
    reshape = await reshapeResp.json();
    console.log("  Reshape: before=" + reshape.beforeScore + " -> after=" + reshape.afterScore + " (" + reshape.afterLevel + ")");
    console.log("  Actions=" + reshape.actionsApplied + ", LLM=" + reshape.llmUsed + ", triage=" + (reshape.deterministicTriageCount ?? 0) + ", defuse=" + (reshape.deterministicDefuseCount ?? 0));
  } else {
    console.error("Reshape failed: " + reshapeResp.status);
  }
  const RESHAPE_HOLD = 7000;
  const elapsed = Date.now() - reshapeStart;
  if (elapsed < RESHAPE_HOLD) await sleep(RESHAPE_HOLD - elapsed);

  await page.evaluate(() => {
    const el = document.getElementById("reshape-overlay");
    if (el) { el.style.transition = "opacity 0.5s"; el.style.opacity = "0"; }
  });
  await sleep(500);
  await page.evaluate(() => document.getElementById("reshape-overlay")?.remove());

  const afterScore = reshape?.afterScore ?? "-";
  const afterLevel = reshape?.afterLevel ?? "-";

  // --- Scene 5: AFTER Stress Check-in ---
  console.log("Scene 5: AFTER stress check-in (~" + afterScore + " / " + afterLevel + ") ...");
  await page.goto(CHECKIN_URL, { waitUntil: "domcontentloaded" });
  await page.waitForLoadState("networkidle").catch(() => {});
  await runCheckin(page);
  await addCornerBadge(page, "AFTER", afterScore, afterLevel, false);
  await showCaption(page, "Step 3: Stress check-in \u2014 AFTER reshape", false);
  await sleep(CAPTION_HOLD);
  await fadeCaption(page);
  await sleep(SCENE_HOLD - CAPTION_HOLD);

  // --- Scene 6: AFTER Flamegraph ---
  console.log("Scene 6: AFTER flamegraph...");
  await removeCornerBadge(page);
  await page.goto(FLAMEGRAPH_URL, { waitUntil: "domcontentloaded" });
  await page.waitForLoadState("networkidle").catch(() => {});
  await page.waitForSelector("text=Deep Work", { timeout: 15000 }).catch(() => {});
  await addCornerBadge(page, "AFTER", afterScore, afterLevel, false);
  await showCaption(page, "Step 4: Rebalanced flamegraph \u2014 1 deep \u00b7 up to 3 quick \u00b7 up to 3 maintenance", false);
  await sleep(CAPTION_HOLD);
  await fadeCaption(page);
  await sleep(SCENE_HOLD - CAPTION_HOLD);

  // --- Scene 7: Study Dashboard ---
  console.log("Scene 7: Study dashboard...");
  await removeCornerBadge(page);
  await page.goto(STUDY_URL, { waitUntil: "domcontentloaded" });
  await page.waitForLoadState("networkidle").catch(() => {});
  await showCaption(page, "Step 5: Team dashboard \u2014 cohort trend across all participants", false);
  await page.click("#load-btn").catch(() => {});
  await page.waitForSelector("#summary", { state: "visible", timeout: 15000 }).catch(() => {});
  await sleep(CAPTION_HOLD);
  await fadeCaption(page);
  await sleep(SCENE_HOLD - CAPTION_HOLD);

  // --- Scene 8: Outro summary ---
  console.log("Scene 8: Summary card...");
  await showTitle(
    page,
    (reshape ? (reshape.beforeScore + " \u2192 " + reshape.afterScore) : "Done"),
    reshape ? ("LLM used: " + reshape.llmUsed + "  \u00b7  Actions applied: " + reshape.actionsApplied + "  \u00b7  Pre-pass triage: " + (reshape.deterministicTriageCount ?? 0) + ", defuse: " + (reshape.deterministicDefuseCount ?? 0)) : "Recording complete",
    "All numbers are measured \u2014 no swapping data",
    INTRO_HOLD,
  );

  // --- Done ---
  console.log("\nFinalizing video...");
  await page.close();
  await context.close();
  await browser.close();

  const dest = path.join(OUT_DIR, "seed-reshape.webm");
  if (fs.existsSync(dest)) fs.unlinkSync(dest);
  const files = fs.readdirSync(OUT_DIR).filter(f => f.endsWith(".webm") && f !== "seed-reshape.webm" && f !== "demo-pipeline.webm");
  if (files.length > 0) {
    const latest = files.sort().pop();
    fs.renameSync(path.join(OUT_DIR, latest), dest);
    const stats = fs.statSync(dest);
    console.log("Video saved: " + dest);
    console.log("Size: " + (stats.size / 1024 / 1024).toFixed(2) + " MB");
    if (reshape) {
      console.log("\nMeasured numbers in the video:");
      console.log("  before=" + reshape.beforeScore + " -> after=" + reshape.afterScore + " (" + reshape.afterLevel + ")");
      console.log("  actionsApplied=" + reshape.actionsApplied + ", llmUsed=" + reshape.llmUsed);
    }
  } else {
    console.error("No video file generated!");
    process.exit(1);
  }
}

main().catch(err => {
  console.error("Recording failed:", err);
  process.exit(1);
});

