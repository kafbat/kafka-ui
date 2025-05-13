import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";

setDefaultTimeout(60 * 1000 * 2);


Given('Topics AddATopic clicked', async () => {
  const button = fixture.topics.topicAddTopicButton();
  await expect(button).toBeVisible();
  await button.click();
});