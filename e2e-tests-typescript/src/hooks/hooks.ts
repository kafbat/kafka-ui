import '../helper/env/env';
import { BeforeAll, AfterAll, Before, After, Status } from "@cucumber/cucumber";
import { Browser, BrowserContext } from "@playwright/test";
import { fixture } from "./pageFixture";
import { invokeBrowser } from "../helper/browsers/browserManager";
import { createLogger } from "winston";
import { options } from "../helper/util/logger";
import PanelLocators from "../pages/Panel/PanelLocators";
import BrokersLocators from "../pages/Brokers/BrokersLocators";
import TopicsLocators from "../pages/Topics/TopicsLocators";
import ConsumersLocators from "../pages/Consumers/ConsumersLocators";
import SchemaRegistryLocators from "../pages/SchemaRegistry/SchemaRegistryLocators";
import ConnectorsLocators from "../pages/Connectors/ConnectorsLocators";
import ksqlDbLocators from "../pages/KSQLDB/ksqldbLocators";
import DashboardLocators from '../pages/Dashboard/DashboardLocators';
import TopicCreateLocators from "../pages/Topics/TopicsCreateLocators";

let browser: Browser;
let context: BrowserContext;

BeforeAll(async function () {
    browser = await invokeBrowser();

});

Before(async function ({ pickle }) {
    const scenarioName = pickle.name + pickle.id
    context = await browser.newContext();
    await context.tracing.start({
        name: scenarioName,
        title: pickle.name,
        sources: true,
        screenshots: true, snapshots: true
    });
    const page = await context.newPage();

    fixture.page = page;

    fixture.logger = createLogger(options(scenarioName));

    fixture.navigationPanel = new PanelLocators(page);

    fixture.brokers = new BrokersLocators(page);

    fixture.topics = new TopicsLocators(page);
    fixture.topicsCreate = new TopicCreateLocators(page);

    fixture.consumers = new ConsumersLocators(page);

    fixture.schemaRegistry = new SchemaRegistryLocators(page);

    fixture.connectors = new ConnectorsLocators(page);

    fixture.ksqlDb = new ksqlDbLocators(page);

    fixture.dashboard = new DashboardLocators(page);

});

// After({ timeout: 30000 }, async function ({ pickle, result }) {
//     let img: Buffer | undefined;
//     const path = `./test-results/trace/${pickle.id}.zip`;
//     if (result?.status == Status.FAILED) {
//         img = await fixture.page.screenshot(
//             { path: `./test-results/screenshots/${pickle.name}.png`, type: "png" })
//     }
//     await context.tracing.stop({ path: path });
//     await fixture.page.close();
//     await context.close();
//     if (result?.status == Status.FAILED && img) {
//         await this.attach(
//             img, "image/png"
//         );
//         const traceFileLink = `<a href="https://trace.playwright.dev/">Open ${path}</a>`
//         await this.attach(`Trace file: ${traceFileLink}`, 'text/html');

//     }
// });

After({ timeout: 30000 }, async function ({ pickle, result }) {
    let img: Buffer | undefined;
    const path = `./test-results/trace/${pickle.id}.zip`;

    try {
        if (result?.status === Status.FAILED) {
            img = await fixture.page.screenshot({
                path: `./test-results/screenshots/${pickle.name}.png`,
                type: "png"
            });
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
        await fixture.page.close();
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
            await this.attach(img, "image/png");

            const traceFileLink = `<a href="https://trace.playwright.dev/">Open ${path}</a>`;
            await this.attach(`Trace file: ${traceFileLink}`, "text/html");
        }
    } catch (e) {
        console.error("Error attaching screenshot or trace:", e);
    }
});

AfterAll(async function () {
    await browser.close();
})
