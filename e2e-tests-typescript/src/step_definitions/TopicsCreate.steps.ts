import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";
import { expectVisibility } from "../services/uiHelper";
import { CustomWorld } from "../support/customWorld";
import { generateName } from "../services/commonFunctions";

setDefaultTimeout(60 * 1000 * 2);


Given('Topics AddATopic clicked', async () => {
  const button = fixture.topics.topicAddTopicButton();
  await expect(button).toBeVisible();
  await button.click();
});

Given('TopicCreate heading visible is: {string}', async (visible: string) => {
    await expectVisibility(fixture.topicsCreate.topicsCreateHeading(), visible);
});

Given('TopicCreate TopicName input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateTopicName(), visible);
});

Given('TopicCreate NumberOfPartitions input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateNumberOfPartitions(), visible);
});

Given('TopicCreate CleanupPolicy select visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateCleanupPolicy(), visible);
});

Given('TopicCreate MinInSyncReplicas input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateMinInSyncReplicas(), visible);
});

Given('TopicCreate ReplicationFactor input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateReplicationFactor(), visible);
});

Given('TopicCreate TimeToRetainData input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateTimeToRetainData(), visible);
});

Given('TopicCreate 12Hours button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreate12Hours(), visible);
});

Given('TopicCreate 1Day button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreate1Day(), visible);
});

Given('TopicCreate 2Day button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreate2Day(), visible);
});

Given('TopicCreate 7Day button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreate7Day(), visible);
});

Given('TopicCreate 4Weeks button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreate4Weeks(), visible);
});

Given('TopicCreate MaxPartitionSize select visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateMaxPartitionSize(), visible);
});

Given('TopicCreate MaxMessageSize input visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateMaxMessageSize(), visible);
});

Given('TopicCreate AddCustomParameter button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateAddCustomParameter(), visible);
});

Given('TopicCreate Cancel button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicsCreateCancel(), visible);
});

Given('TopicCreate CreateTopic button visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topicsCreate.topicCreateCreateTopicButton(), visible);
});


When('TopicCreate Topic name starts with: {string}', async function (this: CustomWorld, prefix: string) {
  const topicName = generateName(prefix);
  this.setValue(`topicName-${prefix}`, topicName);
  await fixture.topicsCreate.topicsCreateTopicName().fill(topicName);
});

When('TopicCreate Number of partitons: {int}', async function (this: CustomWorld, count: number) {
  const input = fixture.topicsCreate.topicsCreateNumberOfPartitions();
  await input.fill(count.toString());
});

When('TopicCreate Time to retain data one day', async function (this: CustomWorld) {
  const button = fixture.topicsCreate.topicsCreate1Day();
  await button.click();
});

When('TopicCreate Create topic clicked', async function (this: CustomWorld) {
  const button = fixture.topicsCreate.topicCreateCreateTopicButton();
  await button.click();
});

Then('Header starts with: {string}', async function (this: CustomWorld, prefix: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  const header = fixture.page.getByRole('heading', { name: topicName });

  await expect(header).toBeVisible();
});

Then('Topic name started with: {string} visible is: {string}', async function (this: CustomWorld, prefix: string, visible: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  await expectVisibility(fixture.topics.topicNameLink(topicName), visible);
});

Then('TopicCreate TimeToRetainData value is: {string}', async (expectedValue: string) => {
  const input = fixture.topicsCreate.topicsCreateTimeToRetainData();
  const actualValue = await input.inputValue();
  expect(actualValue).toBe(expectedValue);
});

When('TopicCreate 12Hours button clicked', async () => {
  await fixture.topicsCreate.topicsCreate12Hours().click();
});

When('TopicCreate 1Day button clicked', async () => {
  await fixture.topicsCreate.topicsCreate1Day().click();
});

When('TopicCreate 2Day button clicked', async () => {
  await fixture.topicsCreate.topicsCreate2Day().click();
});

When('TopicCreate 7Day button clicked', async () => {
  await fixture.topicsCreate.topicsCreate7Day().click();
});

When('TopicCreate 4Weeks button clicked', async () => {
  await fixture.topicsCreate.topicsCreate4Weeks().click();
});