/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { expectVisibility } from "../services/uiHelper";
import { generateName   } from "../services/commonFunctions";
import { getAvroSchema } from "../services/schemas"

setDefaultTimeout(60 * 1000 * 2);

Given('SchemaRegistry CheateSchema clicked', async function(this: PlaywrightWorld) {
  await this.locators.schemaRegistry.createSchemaButton.click();
});

Given('SchemaRegistryCreate is visible', async function(this: PlaywrightWorld) {
  await expectVisibility(this.locators.schemaRegistry.createHeading, "true");
});

Given('SchemaRegistryCreate Subject visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.schemaRegistry.subjectTextBox, visible);
});

Given('SchemaRegistryCreate Schema visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.schemaRegistry.schemaTextBox, visible);
});

Given('SchemaRegistryCreate SchemaType visible is: {string}', async function(this: PlaywrightWorld, visible: string) {
  await expectVisibility(this.locators.schemaRegistry.schemaTypeDropDown, visible);
});

When('SchemaRegistryCreate Subject input starts with: {string}', async function(this: PlaywrightWorld, prefix: string) {
  const subjectName = generateName(prefix);
  await this.locators.schemaRegistry.subjectTextBox.fill(subjectName);
  this.setValue(`subjectName-${prefix}`, subjectName);
});

When('SchemaRegistryCreate Schema input from avro', async function(this: PlaywrightWorld) {
  const schema = getAvroSchema();
  await this.locators.schemaRegistry.schemaTextBox.fill(schema);
});

When('SchemaRegistryCreate Submit clicked', async function(this: PlaywrightWorld) {
  await this.locators.schemaRegistry.submit.click();
});

Then('SchemaRegistrySchemaName starts with: {string}, visible is: {string}',
  async function(this: PlaywrightWorld, prefix: string, visible: string) {
    const streamName = this.getValue<string>(`subjectName-${prefix}`);
    const locator = this.locators.schemaName.heading(streamName);
    await expectVisibility(locator, visible);
  }
);