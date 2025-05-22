import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { PlaywrightCustomWorld } from "../support/PlaywrightCustomWorld";
import { expectVisibility } from "../services/uiHelper";
import { generateName } from "../services/commonFunctions";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics AddATopic clicked', async function() {
  const button = this.locators.topics.topicAddTopicButton();
  await expect(button).toBeVisible();
  await button.click();
});

Given('TopicCreate heading visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateHeading(), visible);
});

Given('TopicCreate TopicName input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateTopicName(), visible);
});

Given('TopicCreate NumberOfPartitions input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateNumberOfPartitions(), visible);
});

Given('TopicCreate CleanupPolicy select visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateCleanupPolicy(), visible);
});

Given('TopicCreate MinInSyncReplicas input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateMinInSyncReplicas(), visible);
});

Given('TopicCreate ReplicationFactor input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateReplicationFactor(), visible);
});

Given('TopicCreate TimeToRetainData input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateTimeToRetainData(), visible);
});

Given('TopicCreate 12Hours button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreate12Hours(), visible);
});

Given('TopicCreate 1Day button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreate1Day(), visible);
});

Given('TopicCreate 2Day button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreate2Day(), visible);
});

Given('TopicCreate 7Day button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreate7Day(), visible);
});

Given('TopicCreate 4Weeks button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreate4Weeks(), visible);
});

Given('TopicCreate MaxPartitionSize select visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateMaxPartitionSize(), visible);
});

Given('TopicCreate MaxMessageSize input visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateMaxMessageSize(), visible);
});

Given('TopicCreate AddCustomParameter button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateAddCustomParameter(), visible);
});

Given('TopicCreate Cancel button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicsCreateCancel(), visible);
});

Given('TopicCreate CreateTopic button visible is: {string}', async function(this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topicsCreate.topicCreateCreateTopicButton(), visible);
});

When('TopicCreate Topic name starts with: {string}', async function(this: PlaywrightCustomWorld, prefix: string) {
  const topicName = generateName(prefix);
  this.setValue(`topicName-${prefix}`, topicName);
  await this.locators.topicsCreate.topicsCreateTopicName().fill(topicName);
});

When('TopicCreate Number of partitons: {int}', async function(this: PlaywrightCustomWorld, count: number) {
  await this.locators.topicsCreate.topicsCreateNumberOfPartitions().fill(count.toString());
});

When('TopicCreate Time to retain data one day', async function() {
  await this.locators.topicsCreate.topicsCreate1Day().click();
});

When('TopicCreate Create topic clicked', async function() {
  await this.locators.topicsCreate.topicCreateCreateTopicButton().click();
});

Then('Header starts with: {string}', async function(this: PlaywrightCustomWorld, prefix: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  const header = this.page.getByRole('heading', { name: topicName });
  await expect(header).toBeVisible();
});

Then('Topic name started with: {string} visible is: {string}', async function(this: PlaywrightCustomWorld, prefix: string, visible: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  await expectVisibility(this.locators.topics.topicNameLink(topicName), visible);
});

Then('TopicCreate TimeToRetainData value is: {string}', async function(this: PlaywrightCustomWorld, expectedValue: string) {
  const input = this.locators.topicsCreate.topicsCreateTimeToRetainData();
  const actualValue = await input.inputValue();
  expect(actualValue).toBe(expectedValue);
});

When('TopicCreate 12Hours button clicked', async function() {
  await this.locators.topicsCreate.topicsCreate12Hours().click();
});

When('TopicCreate 1Day button clicked', async function() {
  await this.locators.topicsCreate.topicsCreate1Day().click();
});

When('TopicCreate 2Day button clicked', async function() {
  await this.locators.topicsCreate.topicsCreate2Day().click();
});

When('TopicCreate 7Day button clicked', async function() {
  await this.locators.topicsCreate.topicsCreate7Day().click();
});

When('TopicCreate 4Weeks button clicked', async function() {
  await this.locators.topicsCreate.topicsCreate4Weeks().click();
});
