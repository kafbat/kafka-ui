import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { stepsContext } from "../hooks/pageFixture";
import expect from "../helper/util/expect";
import { scenarioContext } from "../hooks/scenarioContext";

setDefaultTimeout(60 * 1000 * 2);

Given('Brokers is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.brokersLink()).toBeVisible();
});

When('click on Brokers link', async () => {
  await scenarioContext.panel!.brokersLink().click();
});

Then('Brokers heading visible', async () => {
  await scenarioContext.brokers!.brokersHeading().waitFor({ state: 'visible' });
});

Given('Topics is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.topicsLink()).toBeVisible();
});

When('click on Topics link', async () => {
  await scenarioContext.panel!.topicsLink().click();
});

Then('Topics heading visible', async () => {
  await scenarioContext.topics!.topicsHeading().waitFor({ state: 'visible' });
});

Given('Consumers is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.consumersLink()).toBeVisible();
});

When('click on Consumers link', async () => {
  await scenarioContext.panel!.consumersLink().click();
});

Then('Consumers heading visible', async () => {
  await scenarioContext.consumers!.consumersHeading().waitFor({ state: 'visible' });
});

Given('Schema Registry is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.schemaRegistryLink()).toBeVisible();
});

When('click on Schema Registry link', async () => {
  await scenarioContext.panel!.schemaRegistryLink().click();
});

Then('Schema Registry heading visible', async () => {
  await scenarioContext.schemaRegistry!.schemaRegistryHeading().waitFor({ state: 'visible' });
});

Given('Kafka Connect is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.kafkaConnectLink()).toBeVisible();
});

When('click on Kafka Connect link', async () => {
  await scenarioContext.panel!.kafkaConnectLink().click();
});

Then('Kafka Connect heading visible', async () => {
  await scenarioContext.connectors!.connectorsHeading().waitFor({ state: 'visible' });
});

Given('KSQL DB is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.ksqlDbLink()).toBeVisible();
});

When('click on KSQL DB link', async () => {
  await scenarioContext.panel!.ksqlDbLink().click();
});

Then('KSQL DB heading visible', async () => {
  await scenarioContext.ksqlDb!.ksqlDbHeading().waitFor({ state: 'visible' });
});

Given('Dashboard is visible', async () => {
  await stepsContext.page.goto(process.env.BASEURL!);
  await expect(scenarioContext.panel!.getDashboardLink()).toBeVisible();
});

When('click on Dashboard link', async () => {
  const dashboard = scenarioContext.panel!.getDashboardLink();
  await dashboard.isVisible();
  await dashboard.click();
});

Then('Dashboard heading visible', async () => {
  await scenarioContext.dashboard!.dashboardHeading().waitFor({ state: 'visible' });
});

Then('the end of current URL should be {string}', async (expected: string) => {
  const actual = new URL(stepsContext.page.url()).pathname;
  expect(actual.endsWith(expected)).toBeTruthy();
});

Then('the part of current URL should be {string}', async (expected: string) => {
  const actual = new URL(stepsContext.page.url()).pathname;
  expect(actual.includes(expected)).toBeTruthy();
});