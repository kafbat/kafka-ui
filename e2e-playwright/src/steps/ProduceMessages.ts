/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { expectVisibility, expectVisuallyActive } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";

setDefaultTimeout(60 * 1000 * 2);

Given('Topics TopicName partitions is: {int}', async function(this: PlaywrightWorld, count: number) {
    await expectVisibility(this.locators.topicTopicName.partitions(count.toString()), 'true');
});

Given('Topics TopicName Overview visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.overview, visible);
});

Given('Topics TopicName Messages visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.messages, visible);
});

Given('Topics TopicName Consumers visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.consumers, visible);
});

Given('Topics TopicName Settings visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.settings, visible);
});

Given('Topics TopicName Statistics visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.statistics, visible);
});

Given('Produce message clicked', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.produceMessage.click();
});

Then('ProduceMessage header visible', async function(this: PlaywrightWorld) {
    await expect(this.locators.produceMessage.heading).toBeVisible();
});

Given('ProduceMessage Key input is: {string}', async function(this: PlaywrightWorld, key: string) {
    const textbox = this.locators.produceMessage.keyTextbox;
    await textbox.fill(key);

    const actualValue = await textbox.inputValue();
    expect(actualValue).toContain(key);
});

Given('ProduceMessage Value input is: {string}', async function(this: PlaywrightWorld, value: string) {
    const textbox = this.locators.produceMessage.valueTextbox;
    await textbox.fill(value);

    const actualValue = await textbox.inputValue();
    expect(actualValue).toContain(value);
});

Given('ProduceMessage Headers input key is: {string}, value is: {string}', async function(this: PlaywrightWorld, key: string, value: string) {
    const header = `{"${key}":"${value}"}`;
    const textbox = this.locators.produceMessage.headersTextbox;
    await textbox.clear();
    await textbox.fill(header);

    const actualValue = await textbox.inputValue();
    expect(actualValue).toContain(header);
  }
);

Given('ProduceMessage Produce Message button clicked', async function(this: PlaywrightWorld) {
    await this.locators.produceMessage.produceMessage.click();
});

When('Topics TopicName Messages clicked', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.messages.click();
});

Then('TopicName messages contains key: {string}', async function(this: PlaywrightWorld, expectedKey: string) {
    await this.locators.topicTopicName.messageValue(expectedKey).click()
    await this.locators.topicTopicName.keyButton.click();
    await expectVisibility(this.locators.topicTopicName.messageKeyTextbox(expectedKey), "true");
});

Then('TopicName messages contains value: {string}', async function(this: PlaywrightWorld, expectedValue: string) {
    await this.locators.topicTopicName.valueButton.click()
    await expectVisibility(this.locators.topicTopicName.messageValue(expectedValue), "true");
});

Then('TopicName messages contains headers key is: {string}, value is: {string}', async function(this: PlaywrightWorld, headerKey: string, headerValue: string) {
    const expectedHeader = `"${headerKey}":"${headerValue}"`;
    await this.locators.topicTopicName.headersButton.click()
    await expectVisibility(this.locators.topicTopicName.messageHeadersTextbox(expectedHeader), "true");
});

Given('TopicName menu button clicked', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.dotsMenu.click();
});

Then('TopicNameMenu clear messages active is: {string}', async function(this: PlaywrightWorld, state: string) {
    await expectVisuallyActive(this.locators.topicTopicName.menuItemClearMessages, state);
});

When('TopicNameMenu edit settings clicked', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.menuItemEditSettings.click();
});

When('TopicName cleanup policy set to: {string}', async function(this: PlaywrightWorld, policy: string) {
    await this.locators.topicTopicName.cleanupPolicyDropdown.click();
    await this.locators.topicTopicName.cleanupPolicyDropdownItem(policy).click();
});

When('TopicName UpdateTopic button clicked', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.updateTopicButton.click();
});