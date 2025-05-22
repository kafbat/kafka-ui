import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { expectVisibility, ensureCheckboxState } from "../services/uiHelper";
import { PlaywrightCustomWorld } from "../support/PlaywrightCustomWorld"; 

setDefaultTimeout(60 * 1000 * 2);

Given('Topics Serchfield visible', async function (this: PlaywrightCustomWorld) {
  await expect(this.locators.topics.topicSearchField()).toBeVisible();
});

Given('Topics ShowInternalTopics visible', async function (this: PlaywrightCustomWorld) {
  await expect(this.locators.topics.topicShowInternalTopics()).toBeVisible();
});

Given('Topics AddATopic visible', async function (this: PlaywrightCustomWorld) {
  await expect(this.locators.topics.topicAddTopicButton()).toBeVisible();
});

Given('Topics DeleteSelectedTopics active is: {string}', async function (this: PlaywrightCustomWorld, state: string) {
  const isEnabled = await this.locators.topics.topicDeleteSelectedTopicsButton().isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

Given('Topics CopySelectedTopic active is: {string}', async function (this: PlaywrightCustomWorld, state: string) {
  const isEnabled = await this.locators.topics.topicCopySelectedTopicButton().isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

Given('Topics PurgeMessagesOfSelectedTopics active is: {string}', async function (this: PlaywrightCustomWorld, state: string) {
  const isEnabled = await this.locators.topics.topicPurgeMessagesOfSelectedTopicsButton().isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

When('Topic SelectAllTopic visible is: {string}', async function (this: PlaywrightCustomWorld, visible: string) {
  await expectVisibility(this.locators.topics.topicSelectAllCheckBox(), visible);
});

Then('Topic SelectAllTopic checked is: {string}', async function (this: PlaywrightCustomWorld, state: string) {
  const checkbox = this.locators.topics.topicSelectAllCheckBox();
  await ensureCheckboxState(checkbox, state);
  const actual = await checkbox.isChecked();
  expect(actual.toString()).toBe(state);
});

When('Topics serchfield input {string}', async function (this: PlaywrightCustomWorld, topicName: string) {
  const textBox = this.locators.topics.topicSearchField();
  await textBox.fill(topicName);
  const actual = await textBox.inputValue();
  expect(actual).toBe(topicName);
});

Then('Topic named: {string} visible is: {string}', async function (this: PlaywrightCustomWorld, topicName: string, visible: string) {
  await expectVisibility(this.locators.topics.topicNameLink(topicName), visible);
});

When('Topic serchfield input cleared', async function (this: PlaywrightCustomWorld) {
  const textBox = this.locators.topics.topicSearchField();
  await textBox.fill('');
  const text = await textBox.inputValue();
  expect(text).toBe('');
});

When('Topics ShowInternalTopics switched is: {string}', async function (this: PlaywrightCustomWorld, state: string) {
  const checkBox = this.locators.topics.topicShowInternalTopics();
  await ensureCheckboxState(checkBox, state);
});

When('Topic row named: {string} checked is: {string}', async function (this: PlaywrightCustomWorld, topicName: string, state: string) {
  const checkbox = this.locators.topics.topicRowCheckBox(topicName);
  await ensureCheckboxState(checkbox, state);
});
