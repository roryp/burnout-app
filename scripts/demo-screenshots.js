/**
 * Playwright screenshot script for Burnout-as-a-Service demo.
 * Called by demo-screenshots.ps1 — do not run directly.
 *
 * Usage: node scripts/demo-screenshots.js <baseUrl> <outDir> <mode> [width] [height] [waitMs]
 *   mode: before | after | study
 */
const { chromium } = require('playwright');

(async () => {
    const baseUrl = process.argv[2];
    const outDir = process.argv[3];
    const mode = process.argv[4] || 'before';
    const width = parseInt(process.argv[5]) || 1280;
    const height = parseInt(process.argv[6]) || 900;
    const waitMs = parseInt(process.argv[7]) || 3000;

    if (!baseUrl || !outDir) {
        console.error('Usage: node demo-screenshots.js <baseUrl> <outDir> <mode> [width] [height] [waitMs]');
        process.exit(1);
    }

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width, height } });

    async function screenshot(url, filename, actions) {
        const page = await context.newPage();
        await page.goto(url, { waitUntil: 'networkidle' });
        if (actions) await actions(page);
        await page.waitForTimeout(waitMs);
        const path = outDir + '/' + filename;
        await page.screenshot({ path, fullPage: true, type: 'png' });
        console.log('  Saved: ' + filename);
        await page.close();
    }

    if (mode === 'before') {
        // --- Landing page ---
        console.log('Taking landing page screenshot...');
        await screenshot(baseUrl + '/', 'landing.png');

        // --- BEFORE: Checkin ---
        console.log('Taking BEFORE checkin screenshot...');
        await screenshot(baseUrl + '/checkin.html', 'checkin-before.png', async (page) => {
            await page.fill('input[placeholder*="octocat"]', 'roryp');
            await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
            await page.click('button:has-text("Check My Stress")');
            await page.waitForSelector('text=CRITICAL', { timeout: 15000 }).catch(() => {
                return page.waitForSelector('[class*="score"]', { timeout: 5000 });
            });
        });

        // --- BEFORE: Checkin with drilldown expanded ---
        console.log('Taking BEFORE checkin drilldown screenshot...');
        await screenshot(baseUrl + '/checkin.html', 'stress-before.png', async (page) => {
            await page.fill('input[placeholder*="octocat"]', 'roryp');
            await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
            await page.click('button:has-text("Check My Stress")');
            await page.waitForSelector('text=CRITICAL', { timeout: 15000 }).catch(() => {
                return page.waitForSelector('[class*="score"]', { timeout: 5000 });
            });
            await page.waitForTimeout(500);
            const toggles = await page.$$('.issue-toggle');
            for (const toggle of toggles) {
                const text = await toggle.textContent();
                if (text && text.includes('issue')) { await toggle.click(); break; }
            }
            await page.waitForTimeout(500);
        });

        // --- BEFORE: Flamegraph ---
        console.log('Taking BEFORE flamegraph screenshot...');
        await screenshot(baseUrl + '/flamegraph.html?repo=roryp/burnout-app&userId=roryp', 'flamegraph-before.png', async (page) => {
            await page.waitForSelector('text=Deep Work', { timeout: 15000 });
        });

    } else if (mode === 'after') {
        // --- AFTER: Checkin ---
        console.log('Taking AFTER checkin screenshot...');
        await screenshot(baseUrl + '/checkin.html', 'checkin-after.png', async (page) => {
            await page.fill('input[placeholder*="octocat"]', 'roryp');
            await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
            await page.click('button:has-text("Check My Stress")');
            // After reshape, stress is typically MODERATE (chaos defused, real after-hours preserved).
            // Fall back to LOW if the LLM did extra work, then to any score element if neither resolves.
            await page.waitForSelector('text=MODERATE', { timeout: 15000 })
                .catch(() => page.waitForSelector('text=LOW', { timeout: 5000 }))
                .catch(() => page.waitForSelector('[class*="score"]', { timeout: 5000 }));
        });

        // --- AFTER: Checkin with drilldown expanded ---
        console.log('Taking AFTER checkin drilldown screenshot...');
        await screenshot(baseUrl + '/checkin.html', 'stress-after.png', async (page) => {
            await page.fill('input[placeholder*="octocat"]', 'roryp');
            await page.fill('input[placeholder*="owner/repo"]', 'roryp/burnout-app');
            await page.click('button:has-text("Check My Stress")');
            await page.waitForSelector('text=MODERATE', { timeout: 15000 })
                .catch(() => page.waitForSelector('text=LOW', { timeout: 5000 }))
                .catch(() => page.waitForSelector('[class*="score"]', { timeout: 5000 }));
            await page.waitForTimeout(500);
            const toggles = await page.$$('.issue-toggle');
            for (const toggle of toggles) {
                const text = await toggle.textContent();
                if (text && text.includes('issue')) { await toggle.click(); break; }
            }
            await page.waitForTimeout(500);
        });

        // --- AFTER: Flamegraph ---
        console.log('Taking AFTER flamegraph screenshot...');
        await screenshot(baseUrl + '/flamegraph.html?repo=roryp/burnout-app&userId=roryp', 'flamegraph-after.png', async (page) => {
            await page.waitForSelector('text=Deep Work', { timeout: 15000 });
        });

    } else if (mode === 'study') {
        // --- Study Dashboard ---
        console.log('Taking study dashboard screenshot...');
        await screenshot(baseUrl + '/study.html', 'study-dashboard.png', async (page) => {
            await page.click('button:has-text("Load Data")');
            await page.waitForSelector('text=Snapshots', { timeout: 15000 });
        });
    }

    await browser.close();
    console.log('Done (' + mode + ')');
})();
