/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { expectVisibility, expectVisuallyActive, refreshPageAfterDelay } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { getBlankJson } from "../services/templateJsons"
import { jsonFilter } from "../services/filters"

setDefaultTimeout(60 * 1000 * 4);

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

Given('ProduceMessage Value input template Json', async function(this: PlaywrightWorld) {
    const value = getBlankJson
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

Then('Topics TopicName Overview click', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.overview.click();
});

Then('TopicName messages count is {string}', async function(this: PlaywrightWorld, expectedCount: string) {
  await expectVisibility(this.locators.topicTopicName.messagesCount(expectedCount), "true");
});

When('TopicName clear messages clicked', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.messagesDropdown.click();
  await this.locators.topicTopicName.clearMessages.click();
});

When('TopicName menu clear messages clicked', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.menuItemClearMessages.click();
  await this.locators.topicTopicName.confirm.click();
  }
);

Given('TopicName menu button clicked for topic starts with: {string}', async function(this: PlaywrightWorld, prefix: string) {
  const topicName = this.getValue<string>(`topicName-${prefix}`);
  await this.locators.topicTopicName.dotsMenuForTopic(topicName).click();
});


When('TopicName menu RecreateTopic clicked', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.menuItemRecreateTopic.click();
    await this.locators.topicTopicName.confirm.click();
});

Given('Topics TopicName AddFilters button click', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.addFilters.click();
});

Given('Topics TopicName AddFilter visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.topicTopicName.addFilterHead, "true");
});

Given('Topics TopicName AddFilter filterCode Json value is: {string}', async function(this: PlaywrightWorld, value: string) {
    const filter = jsonFilter(value);
    await this.locators.topicTopicName.addFilterFilterCode.click();
    const input = this.locators.topicTopicName.addFilterFilterCodeInput;
    await input.fill(filter);
});

Given('Topics TopicName AddFilter filterCode change value is: {string}', async function(this: PlaywrightWorld, newValue: string) {
    await this.locators.topicTopicName.addFilterFilterCode.click();
    await this.locators.topicTopicName.addFilterFilterCodeInput.press('Backspace');
    await this.locators.topicTopicName.addFilterFilterCodeInput.type(newValue);
});

Given('Topics TopicName AddFilter display name starts with: {string}', async function(this: PlaywrightWorld, namePrefix: string) {
    await this.locators.topicTopicName.addFilterDisplayName.fill(namePrefix);
});

When('Topics TopicName AddFilter button click', async function(this: PlaywrightWorld) {
    await this.locators.topicTopicName.addFilterButton.click();
});

Then('Topics TopicName Messages filter name starts with: {string} visible is: {string}', async function(this: PlaywrightWorld, namePrefix: string, visible: string) {
   await expectVisibility( this.locators.topicTopicName.filterName(namePrefix), visible);
});

Then('Topics TopicName Messages exist is: {string}', async function(this: PlaywrightWorld, expected: string) {
    await expectVisibility( this.locators.topicTopicName.cellExists, expected);
});

Given('Topics TopicName Messages edit filter button click', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.editFilter.click();
});

Then('Topics TopicName AddFilter EditFilter button click', async function(this: PlaywrightWorld) {
  await this.locators.topicTopicName.editFilterButton.click();
});