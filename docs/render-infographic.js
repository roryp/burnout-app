// One-shot infographic renderer.
// Renders docs/images/infographic.html (1920×1080 fixed) to docs/images/algorithm-pipeline.png.
// Run with: node docs/render-infographic.js
const path = require('path');
const { chromium } = require('playwright');

(async () => {
    const htmlPath = path.resolve(__dirname, 'images', 'infographic.html');
    const outPath  = path.resolve(__dirname, 'images', 'algorithm-pipeline.png');

    const browser = await chromium.launch();
    const ctx = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        deviceScaleFactor: 1
    });
    const page = await ctx.newPage();
    await page.goto('file://' + htmlPath.replace(/\\/g, '/'), { waitUntil: 'networkidle' });
    await page.waitForTimeout(300); // let fonts settle
    await page.screenshot({ path: outPath, clip: { x: 0, y: 0, width: 1920, height: 1080 } });
    await browser.close();

    console.log('Wrote ' + outPath);
})().catch(e => { console.error(e); process.exit(1); });
