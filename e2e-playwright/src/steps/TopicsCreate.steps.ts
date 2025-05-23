/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { expectVisibility } from "../services/uiHelper";
import { generateName } from "../services/commonFunctions";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics AddATopic clicked', async function(this: PlaywrightWorld) {
  const button = this.locators.topics.addTopicButton;
  await expect(button).toBeVisible();
  await button.click();
});

Given('TopicCreate heading visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.heading, visible);
});

Given('TopicCreate TopicName input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicName, visible);
});

Given('TopicCreate NumberOfPartitions input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.numberOfPartitions, visible);
});

Given('TopicCreate CleanupPolicy select visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.cleanupPolicy, visible);
});

Given('TopicCreate MinInSyncReplicas input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.minInSyncReplicas, visible);
});

Given('TopicCreate ReplicationFactor input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.replicationFactor, visible);
});

Given('TopicCreate TimeToRetainData input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.timeToRetainData, visible);
});

Given('TopicCreate 12Hours button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.button12Hours, visible);
});

Given('TopicCreate 1Day button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.button1Day, visible);
});

Given('TopicCreate 2Day button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.button2Day, visible);
});

Given('TopicCreate 7Day button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.button7Day, visible);
});

Given('TopicCreate 4Weeks button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.button4Weeks, visible);
});

Given('TopicCreate MaxPartitionSize select visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.maxPartitionSize, visible);
});

Given('TopicCreate MaxMessageSize input visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.maxMessageSize, visible);
});

Given('TopicCreate AddCustomParameter button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.addCustomParameter, visible);
});

Given('TopicCreate Cancel button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.cancel, visible);
});

Given('TopicCreate CreateTopic button visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.createTopicButton, visible);
});

When('TopicCreate Topic name starts with: {string}', async function(this: PlaywrightWorld, prefix: string) {
  const topicName = generateName(prefix);
  this.setValue(`topicName-${prefix}`, topicName);
  await this.locators.topicsCreate.topicName.fill(topicName);
});

When('TopicCreate Number of partitons: {int}', async function(this: PlaywrightWorld, count: number) {
  await this.locators.topicsCreate.numberOfPartitions.fill(count.toString());
});

When('TopicCreate Time to retain data one day', async function() {
  await this.locators.topicsCreate.button1Day.click();
});

When('TopicCreate Create topic clicked', async function() {
  await this.locators.topicsCreate.createTopicButton.click();
});

Then('Header starts with: {string}', async function(this: PlaywrightWorld, prefix: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  const header = this.page.getByRole('heading', { name: topicName });
  await expect(header).toBeVisible();
});

Then('Topic name started with: {string} visible is: {string}', async function(this: PlaywrightWorld, prefix: string, visible: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  await expectVisibility(this.locators.topics.nameLink(topicName), visible);
});

Then('TopicCreate TimeToRetainData value is: {string}', async function(this: PlaywrightWorld, expectedValue: string) {
  const input = this.locators.topicsCreate.timeToRetainData;
  const actualValue = await input.inputValue();
  expect(actualValue).toBe(expectedValue);
});

When('TopicCreate 12Hours button clicked', async function() {
  await this.locators.topicsCreate.button12Hours.click();
});

When('TopicCreate 1Day button clicked', async function() {
  await this.locators.topicsCreate.button1Day.click();
});

When('TopicCreate 2Day button clicked', async function() {
  await this.locators.topicsCreate.button2Day.click();
});

When('TopicCreate 7Day button clicked', async function() {
  await this.locators.topicsCreate.button7Day.click();
});

When('TopicCreate 4Weeks button clicked', async function() {
  await this.locators.topicsCreate.button4Weeks.click();
});
