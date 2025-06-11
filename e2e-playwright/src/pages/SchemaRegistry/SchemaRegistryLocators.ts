import { Page, Locator } from "@playwright/test";

export default class SchemaRegistryLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByRole('heading', { name: 'Schema Registry' })};
    get searchBox(): Locator { return this.page.getByRole('textbox', { name: 'Search by Schema Name' })};
    get createSchemaButton(): Locator { return this.page.getByRole('button', { name: 'Create Schema' })};

    get createHeading(): Locator { return this.page.getByText('Schema RegistryCreate')};
    get subjectTextBox(): Locator { return this.page.getByRole('textbox', { name: 'Schema Name' })};
    get schemaTextBox(): Locator { return this.page.locator('textarea[name="schema"]')};
    get schemaTypeDropDown(): Locator { return this.page.locator('form path')};
    get submit(): Locator { return this.page.getByRole('button', { name: 'Submit' })};

    schemaTypeDropDownElement(value:string): Locator { return this.page.getByRole('option', { name: value })};
}