import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import expect from "../helper/util/expect";
import { PlaywrightCustomWorld } from "../support/PlaywrightCustomWorld";

setDefaultTimeout(60 * 1000 * 2);

Given('Brokers is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.brokersLink()).toBeVisible();
});

When('click on Brokers link', async function() {
  await this.locators.panel.brokersLink().click();
});

Then('Brokers heading visible', async function() {
  await this.locators.brokers.brokersHeading().waitFor({ state: 'visible' });
});

Given('Topics is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.topicsLink()).toBeVisible();
});

When('click on Topics link', async function() {
  await this.locators.panel.topicsLink().click();
});

Then('Topics heading visible', async function() {
  await this.locators.topics.topicsHeading().waitFor({ state: 'visible' });
});

Given('Consumers is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.consumersLink()).toBeVisible();
});

When('click on Consumers link', async function() {
  await this.locators.panel.consumersLink().click();
});

Then('Consumers heading visible', async function() {
  await this.locators.consumers.consumersHeading().waitFor({ state: 'visible' });
});

Given('Schema Registry is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.schemaRegistryLink()).toBeVisible();
});

When('click on Schema Registry link', async function() {
  await this.locators.panel.schemaRegistryLink().click();
});

Then('Schema Registry heading visible', async function() {
  await this.locators.schemaRegistry.schemaRegistryHeading().waitFor({ state: 'visible' });
});

Given('Kafka Connect is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.kafkaConnectLink()).toBeVisible();
});

When('click on Kafka Connect link', async function() {
  await this.locators.panel.kafkaConnectLink().click();
});

Then('Kafka Connect heading visible', async function() {
  await this.locators.connectors.connectorsHeading().waitFor({ state: 'visible' });
});

Given('KSQL DB is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.ksqlDbLink()).toBeVisible();
});

When('click on KSQL DB link', async function() {
  await this.locators.panel.ksqlDbLink().click();
});

Then('KSQL DB heading visible', async function() {
  await this.locators.ksqlDb.ksqlDbHeading().waitFor({ state: 'visible' });
});

Given('Dashboard is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.getDashboardLink()).toBeVisible();
});

When('click on Dashboard link', async function() {
  const dashboard = this.locators.panel.getDashboardLink();
  await dashboard.isVisible(); // Optional: could be removed if already handled in expect above
  await dashboard.click();
});

Then('Dashboard heading visible', async function() {
  await this.locators.dashboard.dashboardHeading().waitFor({ state: 'visible' });
});

Then('the end of current URL should be {string}', async function(this: PlaywrightCustomWorld, expected: string) {
  const actual = new URL(this.page.url()).pathname;
  expect(actual.endsWith(expected)).toBeTruthy();
});

Then('the part of current URL should be {string}', async function(this: PlaywrightCustomWorld, expected: string) {
  const actual = new URL(this.page.url()).pathname;
  expect(actual.includes(expected)).toBeTruthy();
});
