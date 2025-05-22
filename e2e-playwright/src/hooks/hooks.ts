import '../helper/env/env';
import { BeforeAll, AfterAll, Before, After, Status, setDefaultTimeout, setWorldConstructor } from "@cucumber/cucumber";
import { Browser } from "@playwright/test";
import { invokeBrowser } from "../helper/browsers/browserManager";
import fs from 'fs';
import { PlaywrightCustomWorld } from '../support/PlaywrightCustomWorld';

let browser: Browser;

BeforeAll(async function() {
    browser = await invokeBrowser();
});

setDefaultTimeout(60 * 1000);

setWorldConstructor(PlaywrightCustomWorld);

Before(async function(this: PlaywrightCustomWorld, { pickle }) {
    const scenarioName = pickle.name + pickle.id
    const context = await browser.newContext({
        recordVideo: { dir: 'test-results/videos/' },
        locale: 'en-US'
    });
    await context.tracing.start({
        name: scenarioName,
        title: pickle.name,
        sources: true,
        screenshots: true, snapshots: true
    });
   await this.init(context, scenarioName);
});

After({ timeout: 30000 }, async function(this: PlaywrightCustomWorld, { pickle, result }) {
       let img: Buffer | undefined;
    const path = `./test-results/trace/${pickle.id}.zip`;
    try {
        if (result?.status === Status.FAILED) {
            img = await this.page?.screenshot({
                path: `./test-results/screenshots/${pickle.name}.png`,
                type: "png"
            });
            const video = this.page?.video();
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
        await this.browserContext?.tracing.stop({ path });
    } catch (e) {
        console.error("Error stopping tracing:", e);
    }

    try {
        await this.page?.close();
    } catch (e) {
        console.error("Error closing page:", e);
    }

    try {
        await this.browserContext?.close();
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

AfterAll(async function() {
    await browser.close();
})
