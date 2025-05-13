import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics serchfield visible', async () => {
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

When('Topic SelectAllTopic visible is: {string}', async (state: string) => {
  const checkbox = fixture.topics.topicSelectAllCheckBox();
  const shouldBeVisible = state === "true";

  if (shouldBeVisible) {
    await expect(checkbox).toBeVisible();
  } else {
    await expect(checkbox).toHaveCount(0);
  }
});


Then('Topic SelectAllTopic checked is: {string}', async (state: string) => {
  const checkbox = fixture.topics.topicSelectAllCheckBox();
  const desiredState = state === "true";
  const currentState = await checkbox.isChecked();

  if (currentState !== desiredState) {
    if (desiredState) {
      await checkbox.check();
    } else {
      await checkbox.uncheck();
    }
  }

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
    const topic = fixture.topics.topicNameLink(topicName);
    if (visible === "true") {
    await expect(topic).toBeVisible();
  } else if (visible === "false") {
    await expect(topic).toHaveCount(0);
  } else {
    throw new Error(`Invalid visibility value: ${visible}`);
  }
});

When('Topic serchfield input cleared', async () => {
    const textBox = fixture.topics.topicSearchField();

    await textBox.fill('');

  const text = await textBox.inputValue();
  expect(text).toBe('');
});

When('Topics ShowInternalTopics switched is: {string}', async (state: string) => {
    const checkBox = fixture.topics.topicShowInternalTopics();
    const desiredState = state === "true";
    const isChecked = await checkBox.isChecked();

  if (isChecked !== desiredState) {
    if (desiredState) {
      await checkBox.check();    // turn ON
    } else {
      await checkBox.uncheck();  // turn OFF
    }
  }
});

When('Topic row named: {string} checked is: {string}', async (topicName: string, state: string) => {
  const checkbox = fixture.topics.topicRowCheckBox(topicName);
  const desiredState = state === 'true';
  const isChecked = await checkbox.isChecked();

  if (isChecked !== desiredState) {
    if (desiredState) {
      await checkbox.check();
    } else {
      await checkbox.uncheck();
    }
  }

  const finalState = await checkbox.isChecked();
  expect(finalState.toString()).toBe(state);
});
