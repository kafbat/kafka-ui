import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";

setDefaultTimeout(60 * 1000 * 2);

Given('Brokers is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.brokersLink()).toBeVisible();
  });

When('click on Brokers link', async () => {
    await fixture.navigationPanel.brokersLink().click();
});

Then('Brokers heading visible', async () => {
    await fixture.brokers.brokersHeading().waitFor({ state: 'visible' });
});

  
Given('Topics is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.topicsLink()).toBeVisible();
});
  
When('click on Topics link', async () => {
    await fixture.navigationPanel.topicsLink().click();
});

Then('Topics heading visible', async () => {
    await fixture.topics.topicsHeading().waitFor({ state: 'visible' });
});


Given('Consumers is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.consumersLink()).toBeVisible();
});

When('click on Consumers link', async () => {
    await fixture.navigationPanel.consumersLink().click();
});

Then('Consumers heading visible', async () => {
    await fixture.consumers.consumersHeading().waitFor({ state: 'visible' });
});


Given('Schema Registry is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.schemaRegistryLink()).toBeVisible();
});

When('click on Schema Registry link', async () => {
    await fixture.navigationPanel.schemaRegistryLink().click();
});

Then('Schema Registry heading visible', async () => {
    await fixture.schemaRegistry.schemaRegistryHeading().waitFor({ state: 'visible' });
});


Given('Kafka Connect is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.kafkaConnectLink()).toBeVisible();
});

When('click on Kafka Connect link', async () => {
    await fixture.navigationPanel.kafkaConnectLink().click();
});

Then('Kafka Connect heading visible', async () => {
    await fixture.connectors.connectorsHeading().waitFor({ state: 'visible' });
});
  
Given('KSQL DB is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    await expect(fixture.navigationPanel.ksqlDbLink()).toBeVisible();
});

When('click on KSQL DB link', async () => {
    await fixture.navigationPanel.ksqlDbLink().click();
});
  
Then('KSQL DB heading visible', async () => {
    await fixture.ksqlDb.ksqlDbHeading().waitFor({ state: 'visible' });
});


Given('Dashboard is visible', async () => {
    await fixture.page.goto(process.env.BASEURL!);
    var tmp = fixture.navigationPanel.getDashboardLink();
    await expect(fixture.navigationPanel.getDashboardLink()).toBeVisible();
});
  
When('click on Dashboard link', async () => {
    const dashboard = fixture.navigationPanel.getDashboardLink()
    await dashboard.isVisible();
    await dashboard.click();
});

Then('Dashboard heading visible', async () => {
    await fixture.dashboard.dashboardHeading().waitFor({ state: 'visible' });
});


Then('the end of current URL should be {string}', async (expected: string) => {
  const actual = new URL(fixture.page.url()).pathname;
  expect(actual.endsWith(expected)).toBeTruthy();
});

Then('the part of current URL should be {string}', async (expected: string) => {
    const actual = new URL(fixture.page.url()).pathname;
    expect(actual.includes(expected)).toBeTruthy();
  });
