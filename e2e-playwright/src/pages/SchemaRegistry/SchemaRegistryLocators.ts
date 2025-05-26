import { Page, Locator } from "@playwright/test";

export default class SchemaRegistryLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByRole('heading', { name: 'Schema Registry' })};
    get searchBox(): Locator { return this.page.getByRole('textbox', { name: 'Search by Schema Name' })};
    get createSchemaButton(): Locator { return this.page.getByRole('button', { name: 'Create Schema' })};
}