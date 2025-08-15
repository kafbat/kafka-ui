/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout, DataTable  } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import { expectVisibility, refreshPageAfterDelay } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { createStreamQuery, createTableQuery } from "../services/KsqlScripts";
import { generateName, Delete   } from "../services/commonFunctions";

setDefaultTimeout(60 * 1000 * 4);

Given('KSQL DB Tables header visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.tablesHeader, visible);
});

Given('KSQL DB Streams header visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.streamsHeader, visible);
});

Given('KSQL DB Tables link visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.tablesLink, visible);
});

Given('KSQL DB Streams link visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.streamsLink, visible);
});

When('KSQL DB ExecuteKSQLRequest click', async function(this: PlaywrightWorld) {
  await this.locators.ksqlDb.executeKSQLREquestButton.click();
});

Given('KSQL DB Clear visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.clear, visible);
});

Given('KSQL DB Execute visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.execute, visible);
});

Given('KSQL DB textbox visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.ksqlDb.textField, visible);
});

Given('KSQL DB KSQL for stream starts with: {string}, kafka_topic starts with: {string}, value_format: {string}',
  async function(this: PlaywrightWorld, stream: string, topic: string, format: string) {
    const topicName = generateName(topic);
    const streamName = generateName(stream).toUpperCase();
    const query = createStreamQuery(streamName, topicName, format);

    await this.locators.ksqlDb.clear.click();
    const textbox = this.locators.ksqlDb.textField;
    await textbox.click();
    await textbox.type(query);
    await Delete(this.page);
    await this.locators.ksqlDb.execute.click();

    this.setValue(`topicName-${topic}`, topicName);
    this.setValue(`streamName-${stream}`, streamName);
  }
);

Then('KSQL DB stream created', async function(this: PlaywrightWorld) {
    await expectVisibility(this.locators.ksqlDb.success, "true");
    await expectVisibility(this.locators.ksqlDb.streamCSreated, "true");
});

Then('KSQL DB clear result visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
    await expectVisibility(this.locators.ksqlDb.clearResults, visible);
});

Then(
  'KSQL DB KSQL for table starts with: {string}, stream starts with: {string}',
  async function(this: PlaywrightWorld, table: string, stream: string) {

    const tableName = generateName(table);
    const streamName = this.getValue<string>(`streamName-${stream}`);
    const query = createTableQuery(tableName, streamName);

    await this.locators.ksqlDb.clear.click();
    const textbox = this.locators.ksqlDb.textField;
    await textbox.click();
    await textbox.type(query);
    await Delete(this.page);
    await this.locators.ksqlDb.execute.click();

    this.setValue(`tableName-${table}`, tableName);
  }
);

Then('KSQL DB table created', async function(this: PlaywrightWorld) {
    await expectVisibility(this.locators.ksqlDb.success, "true");
});

When('KSQL DB Stream clicked', async function(this: PlaywrightWorld) {
  await this.locators.ksqlDb.streamsLink.click();
});

When('KSQL DB Table clicked', async function(this: PlaywrightWorld) {
  await this.locators.ksqlDb.tablesLink.click();
});

Then('KSQL DB stream starts with: {string} visible is: {string}', async function(this: PlaywrightWorld, name: string, visible: string) {
    const streamName = this.getValue<string>(`streamName-${name}`);
    await refreshPageAfterDelay(this.page);
    await expectVisibility(this.page.getByRole('cell', { name: streamName }), visible);
});

Then('KSQL DB table starts with: {string} visible is: {string}', async function(this: PlaywrightWorld, name: string, visible: string) {
    const tableName = this.getValue<string>(`tableName-${name}`);
    await refreshPageAfterDelay(this.page);
    await expectVisibility(this.page.getByRole('cell', { name: tableName }).first(), visible);
});

Then('KSQL DB KSQL cleared', async function(this: PlaywrightWorld) {
  await this.locators.ksqlDb.clear.click();
  await this.locators.ksqlDb.clearResults.click();
});

Given(
  'KSQL DB KSQL data inserted to stream starts with: {string}, from table:',
  async function(this: PlaywrightWorld, streamPrefix: string, table: DataTable) {
    const streamName = this.getValue<string>(`streamName-${streamPrefix}`);
    const stream = streamName?.trim();
    const rows = table.hashes();

    for (const row of rows) {
      const id = row.Id;
      const lat = row.la;
      const lon = row.lo;

      const query = `INSERT INTO ${stream} (profileId, latitude, longitude) VALUES ('${id}', ${lat}, ${lon});`;

      await this.locators.ksqlDb.clear.click();
      const textbox = this.locators.ksqlDb.textField;
      await textbox.click();
      await textbox.type(query);
      await Delete(this.page);
      await this.locators.ksqlDb.execute.click();

      await expectVisibility(this.locators.ksqlDb.querySucceed, 'true');
      await this.locators.ksqlDb.clear.click();
    }
});

Given(
  'KSQL DB long query stream starts with: {string} stared',
  async function(this: PlaywrightWorld, streamPrefix: string) {
  await this.locators.ksqlDb.clear.click();
  const streamName = this.getValue<string>(`streamName-${streamPrefix}`);
  const query = `SELECT * FROM ${streamName} EMIT CHANGES;`.trim();

  await this.locators.ksqlDb.clear.click();
  const textbox = this.locators.ksqlDb.textField;
  await textbox.click();
  await textbox.type(query);
  await Delete(this.page);
  await this.locators.ksqlDb.execute.click();

  await expectVisibility(this.locators.ksqlDb.consumingQueryExecution, 'true');
});

Given('KSQL DB long query stoped', async function(this: PlaywrightWorld) {
  await expectVisibility(this.locators.ksqlDb.consumingQueryExecution, 'true');
  const abortButton = this.locators.ksqlDb.abort;
  await abortButton.scrollIntoViewIfNeeded();
  await this.locators.ksqlDb.abort.click({ force: true });
});