import { Page, Locator } from "@playwright/test";

export default class SchemaRegistryLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    schemaRegistryHeading = (): Locator => this.page.getByRole('heading', { name: 'Schema Registry' });
    schemaRegistrySearchBox = (): Locator => this.page.getByRole('textbox', { name: 'Search by Schema Name' });
    schemaRegistryCreateSchemaButton = (): Locator => this.page.getByRole('button', { name: 'Create Schema' });
}