import '../helper/env/env';
import { BeforeAll, AfterAll, Before, After, Status, setDefaultTimeout } from "@cucumber/cucumber";
import { Browser, BrowserContext } from "@playwright/test";
import { stepsContext } from "./pageFixture";
import { invokeBrowser } from "../helper/browsers/browserManager";
import { createLogger } from "winston";
import { options } from "../helper/util/logger";
import fs from 'fs';
import { scenarioContext } from "./scenarioContext";
import { locatorsFactory } from './locatorsFactory';

let browser: Browser;
let context: BrowserContext;

BeforeAll(async function () {
    browser = await invokeBrowser();
});

setDefaultTimeout(60 * 1000);

Before(async function ({ pickle }) {
    const scenarioName = pickle.name + pickle.id
    context = await browser.newContext({
        recordVideo: { dir: 'test-results/videos/' },
        locale: 'en-US'
    });
    await context.tracing.start({
        name: scenarioName,
        title: pickle.name,
        sources: true,
        screenshots: true, snapshots: true
    });
    const page = await context.newPage();
    
    stepsContext.page = page;
    stepsContext.logger = createLogger(options(scenarioName));
    
    Object.assign(scenarioContext, locatorsFactory.initAll(page));
});

After({ timeout: 30000 }, async function ({ pickle, result }) {
    let img: Buffer | undefined;
    const path = `./test-results/trace/${pickle.id}.zip`;

    try {
        if (result?.status === Status.FAILED) {
            img = await stepsContext.page.screenshot({
                path: `./test-results/screenshots/${pickle.name}.png`,
                type: "png"
            });
            const video = stepsContext.page.video();
            if (video) {
                const videoPath = await video.path();
                const videoFile = fs.readFileSync(videoPath);
                this.attach(videoFile, 'video/webm');
            }
        }
    } catch (e) {
        console.error("Error taking screenshot:", e);
    }

    try {
        await context.tracing.stop({ path });
    } catch (e) {
        console.error("Error stopping tracing:", e);
    }

    try {
        await stepsContext.page.close();
    } catch (e) {
        console.error("Error closing page:", e);
    }

    try {
        await context.close();
    } catch (e) {
        console.error("Error closing context:", e);
    }

    try {
        if (result?.status === Status.FAILED && img) {
            this.attach(img, "image/png");

            const traceFileLink = `<a href="https://trace.playwright.dev/">Open ${path}</a>`;
            this.attach(`Trace file: ${traceFileLink}`, "text/html");
        }
    } catch (e) {
        console.error("Error attaching screenshot or trace:", e);
    }
});

AfterAll(async function () {
    await browser.close();
})
