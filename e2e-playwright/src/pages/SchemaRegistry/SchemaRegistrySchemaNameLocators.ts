import { Page, Locator } from "@playwright/test";

export default class SchemaRegistrySchemaNameLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    heading(value: string): Locator { return this.page.getByText(`Schema Registry${value}`)};
}