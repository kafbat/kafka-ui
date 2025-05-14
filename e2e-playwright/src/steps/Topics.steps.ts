import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";
import { expectVisibility, ensureCheckboxState } from "../services/uiHelper";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics Serchfield visible', async () => {
    await expect(fixture.topics.topicSearchField()).toBeVisible();
});

Given('Topics ShowInternalTopics visible', async () => {
  await expect(fixture.topics.topicShowInternalTopics()).toBeVisible();
});


Given('Topics AddATopic visible', async () => {
  await expect(fixture.topics.topicAddTopicButton()).toBeVisible();
});


Given('Topics DeleteSelectedTopics active is: {string}', async (state: string) => {
  const isEnabled = await fixture.topics.topicDeleteSelectedTopicsButton().isEnabled();

  expect(isEnabled.toString()).toBe(state);
});


Given('Topics CopySelectedTopic active is: {string}', async (state: string) => {
  const isEnabled = await fixture.topics.topicCopySelectedTopicButton().isEnabled();

  expect(isEnabled.toString()).toBe(state);
});


Given('Topics PurgeMessagesOfSelectedTopics active is: {string}', async (state: string) => {
  const isEnabled = await fixture.topics.topicPurgeMessagesOfSelectedTopicsButton().isEnabled();

  expect(isEnabled.toString()).toBe(state);
});

When('Topic SelectAllTopic visible is: {string}', async (visible: string) => {
  await expectVisibility(fixture.topics.topicSelectAllCheckBox(), visible)
});


Then('Topic SelectAllTopic checked is: {string}', async (state: string) => {
  const checkbox = fixture.topics.topicSelectAllCheckBox();
  await ensureCheckboxState(checkbox, state);
  const actual = await checkbox.isChecked();
  expect(actual.toString()).toBe(state);
});

When('Topics serchfield input {string}', async (topicName: string) => {
    const textBox = fixture.topics.topicSearchField();

    await textBox.fill(topicName);

    const actual = await textBox.inputValue();
    expect(actual, topicName)
});

Then('Topic named: {string} visible is: {string}', async (topicName: string, visible: string) => {
  await expectVisibility(fixture.topics.topicNameLink(topicName), visible);
});

When('Topic serchfield input cleared', async () => {
  const textBox = fixture.topics.topicSearchField();

  await textBox.fill('');

  const text = await textBox.inputValue();
  expect(text).toBe('');
});

When('Topics ShowInternalTopics switched is: {string}', async (state: string) => {
  const checkBox = fixture.topics.topicShowInternalTopics();

  await ensureCheckboxState(checkBox, state);
});

When('Topic row named: {string} checked is: {string}', async (topicName: string, state: string) => {
  const checkbox = fixture.topics.topicRowCheckBox(topicName);
  
  await ensureCheckboxState(checkbox, state);
});
