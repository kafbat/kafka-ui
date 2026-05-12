/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { expectVisibility } from "../services/uiHelper";

setDefaultTimeout(60 * 1000 * 2);

Given('Brokers Uptime visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokers.uptime, visible);
});

Given('Brokers Partitions visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokers.partitions, visible);
});

When('Brokers cell element {string} clicked', async function(this: PlaywrightWorld, index: string) {
  await this.locators.brokers.toBroker(index).click();
});

Given('BrokerDetails name is: {string} header visible is: {string}', async function(this: PlaywrightWorld, expectedName: string, visible: string) {
  await expectVisibility(this.locators.brokerDetails.header(expectedName), visible);
});

Given('BrokerDetails Log directories visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.brokerDetails.logDirectors, visible);
});

Given('BrokerDetails Configs visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.configs, visible);
});

Given('BrokerDetails Metrics visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.metrics, visible);
});

Given('BrokerDetails Configs Key visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.configsKey, visible);
});

Given('BrokerDetails Configs Value visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.configsValue, visible);
});

Given('BrokerDetails Configs Source visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.configsSource, visible);
});

When('BrokerDetails Configs clicked', async function(this: PlaywrightWorld) {
  await this.locators.brokerDetails.configs.click();
});
Then('BrokerDetails searchfield visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.brokerDetails.configsTextbox, visible);
});

When('BrokerDetails searchfield input is: {string} cell value is: {string}', async function(this: PlaywrightWorld, input: string, expected: string) {
    const searchField = this.locators.brokerDetails.configsTextbox;
    await searchField.fill(input);
    await expectVisibility( this.locators.brokerDetails.configCell(expected), "true");
    await searchField.fill('');
});
