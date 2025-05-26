/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { expectVisibility, ensureCheckboxState } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics Serchfield visible', async function() {
  await expect(this.locators.topics.searchField).toBeVisible();
});

Given('Topics ShowInternalTopics visible', async function() {
  await expect(this.locators.topics.showInternalTopics).toBeVisible();
});

Given('Topics AddATopic visible', async function() {
  await expect(this.locators.topics.addTopicButton).toBeVisible();
});

Given('Topics DeleteSelectedTopics active is: {string}', async function(this: PlaywrightWorld, state: string) {
  const isEnabled = await this.locators.topics.deleteSelectedTopicsButton.isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

Given('Topics CopySelectedTopic active is: {string}', async function(this: PlaywrightWorld, state: string) {
  const isEnabled = await this.locators.topics.copySelectedTopicButton.isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

Given('Topics PurgeMessagesOfSelectedTopics active is: {string}', async function(this: PlaywrightWorld, state: string) {
  const isEnabled = await this.locators.topics.purgeMessagesOfSelectedTopicsButton.isEnabled();
  expect(isEnabled.toString()).toBe(state);
});

When('Topic SelectAllTopic visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.topics.selectAllCheckBox, visible);
});

Then('Topic SelectAllTopic checked is: {string}', async function(this: PlaywrightWorld, state: string) {
  const checkbox = this.locators.topics.selectAllCheckBox;
  await ensureCheckboxState(checkbox, state);
  const actual = await checkbox.isChecked();
  expect(actual.toString()).toBe(state);
});

When('Topics serchfield input {string}', async function(this: PlaywrightWorld, topicName: string) {
  const textBox = this.locators.topics.searchField;
  await textBox.fill(topicName);
  const actual = await textBox.inputValue();
  expect(actual).toBe(topicName);
});

Then('Topic named: {string} visible is: {string}', async function(this: PlaywrightWorld, topicName: string, visible: string) {
  await expectVisibility(this.locators.topics.nameLink(topicName), visible);
});

When('Topic serchfield input cleared', async function(this: PlaywrightWorld) {
  const textBox = this.locators.topics.searchField;
  await textBox.fill('');
  const text = await textBox.inputValue();
  expect(text).toBe('');
});

When('Topics ShowInternalTopics switched is: {string}', async function(this: PlaywrightWorld, state: string) {
  const checkBox = this.locators.topics.showInternalTopics;
  await ensureCheckboxState(checkBox, state);
});

When('Topic row named: {string} checked is: {string}', async function(this: PlaywrightWorld, topicName: string, state: string) {
  const checkbox = this.locators.topics.rowCheckBox(topicName);
  await ensureCheckboxState(checkbox, state);
});
