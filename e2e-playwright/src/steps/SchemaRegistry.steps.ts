/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { expectVisibility } from "../services/uiHelper";
import { generateName } from "../services/commonFunctions";
import { getAvroSchema, getAvroUpdatedSchema, getJsonSchema, getProtobufSchema } from "../services/schemas"
import { clearWithSelectAll } from '../services/commonFunctions'

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

When('SchemaRegistryCreate Schema input from json', async function(this: PlaywrightWorld) {
  const schema = getJsonSchema();
  await this.locators.schemaRegistry.schemaType.click();
  await this.locators.schemaRegistry.schemaTypeElement("JSON").click();
  await this.locators.schemaRegistry.schemaTextBox.fill(schema);
});

When('SchemaRegistryCreate Schema input from protobuf', async function(this: PlaywrightWorld) {
  const schema = getProtobufSchema();
  await this.locators.schemaRegistry.schemaType.click();
  await this.locators.schemaRegistry.schemaTypeElement("PROTOBUF").click();
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

Given('SchemaRegistry click on schema starts with: {string}', async function(this: PlaywrightWorld, prefix: string) {
  const schemaName = this.getValue<string>(`subjectName-${prefix}`);
  const locator = await this.locators.schemaRegistry.toSchema(schemaName).click();
});

Given('SchemaRegistrySchemaName version is: {string}', async function(this: PlaywrightWorld, version: string) {
  await expectVisibility( this.locators.schemaName.version(version), "true");
});

Given('SchemaRegistrySchemaName type is: {string}', async function(this: PlaywrightWorld, expectedType: string) {
  await expectVisibility( this.locators.schemaName.type(expectedType), "true");
});

Given('SchemaRegistrySchemaName Compatibility is: {string}', async function(this: PlaywrightWorld, expectedCompatibility: string) {
    await expectVisibility( this.locators.schemaName.compatibility(expectedCompatibility), "true");
});

Given('SchemaRegistrySchemaName EditSchema clicked', async function(this: PlaywrightWorld) {
  await this.locators.schemaName.editSchema.click();
});

Given('SchemaRegistrySchemaNameEdit New schema is update avro not valid', async function(this: PlaywrightWorld) {
  const schema = getAvroUpdatedSchema();
  const textBox = this.locators.schemaName.newSchemaTextbox;
  await textBox.click();
  await clearWithSelectAll(this.page);
  await textBox.focus();
  await textBox.type(schema)
});

Given(
  'SchemaRegistrySchemaNameEdit old Compatibility is: {string}, new Compatibility is: {string}',
  async function(this: PlaywrightWorld, oldLevel: string, newLevel: string) {
    await this.locators.schemaName.editDropdown(oldLevel).click();
    await this.locators.schemaName.editDropdownElement(newLevel).click();
  }
);

When('SchemaRegistrySchemaNameEdit submit clicked', async function(this: PlaywrightWorld) {
  await this.locators.schemaName.submit.click();
});

Then('Error incompatible visible', async function(this: PlaywrightWorld) {
  await expectVisibility(this.locators.schemaName.incompatibeError, "true");
});

When('SchemaRegistrySchemaName remove schema clicked', async function(this: PlaywrightWorld) {
  await this.locators.schemaName.menu.click();
  await this.locators.schemaName.removeSchema.click()
  await this.locators.schemaName.confirm.click();
});

Then('Schema starts with: {string} deleted', async function(this: PlaywrightWorld, prefix: string) {
  const schemaName = this.getValue<string>(`subjectName-${prefix}`);

});