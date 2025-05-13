import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";
import { expectVisibility } from "../services/visibilityHelper";

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