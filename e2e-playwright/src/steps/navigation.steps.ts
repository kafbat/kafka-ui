/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import expect from "../helper/util/expect";
import { PlaywrightWorld } from "../support/PlaywrightWorld";

setDefaultTimeout(60 * 1000 * 2);

Given('Brokers is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.brokersLink).toBeVisible();
});

When('click on Brokers link', async function() {
  await this.locators.panel.brokersLink.click();
});

Then('Brokers heading visible', async function() {
  await this.locators.brokers.heading.waitFor({ state: 'visible' });
});

Given('Topics is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.topicsLink).toBeVisible();
});

When('click on Topics link', async function() {
  await this.locators.panel.topicsLink.click();
});

Then('Topics heading visible', async function() {
  await this.locators.topics.heading.waitFor({ state: 'visible' });
});

Given('Consumers is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.consumersLink).toBeVisible();
});

When('click on Consumers link', async function() {
  await this.locators.panel.consumersLink.click();
});

Then('Consumers heading visible', async function() {
  await this.locators.consumers.heading.waitFor({ state: 'visible' });
});

Given('Schema Registry is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.schemaRegistryLink).toBeVisible();
});

When('click on Schema Registry link', async function() {
  await this.locators.panel.schemaRegistryLink.click();
});

Then('Schema Registry heading visible', async function() {
  await this.locators.schemaRegistry.heading.waitFor({ state: 'visible' });
});

Given('Kafka Connect is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.kafkaConnectLink).toBeVisible();
});

When('click on Kafka Connect link', async function(this: PlaywrightWorld) {
  await this.locators.panel.kafkaConnectLink.click();
});

Then('Kafka Connect heading visible', async function(this: PlaywrightWorld) {
  await this.locators.connectors.heading.waitFor({ state: 'visible' });
});

Given('KSQL DB is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  await expect(this.locators.panel.ksqlDbLink).toBeVisible();
});

When('click on KSQL DB link', async function(this: PlaywrightWorld) {
  await this.locators.panel.ksqlDbLink.click();
});

Then('KSQL DB heading visible', async function(this: PlaywrightWorld) {
  await this.locators.ksqlDb.heading.waitFor({ state: 'visible' });
});

Given('Dashboard is visible', async function() {
  await this.page.goto(process.env.BASEURL!);
  expect(this.locators.panel.getDashboardLink.isVisible());
});

When('click on Dashboard link', async function(this: PlaywrightWorld) {
  const dashboard = this.locators.panel.getDashboardLink;
  await dashboard.isVisible();
  await dashboard.click();
});

Then('Dashboard heading visible', async function() {
  await this.locators.dashboard.heading.waitFor({ state: 'visible' });
});

Then('the end of current URL should be {string}', async function(this: PlaywrightWorld, expected: string) {
  const actual = new URL(this.page.url()).pathname;
  expect(actual.endsWith(expected)).toBeTruthy();
});

Then('the part of current URL should be {string}', async function(this: PlaywrightWorld, expected: string) {
  const actual = new URL(this.page.url()).pathname;
  expect(actual.includes(expected)).toBeTruthy();
});
