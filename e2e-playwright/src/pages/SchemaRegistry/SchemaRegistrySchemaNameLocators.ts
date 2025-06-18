import { Page, Locator } from "@playwright/test";

export default class SchemaRegistrySchemaNameLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get editSchema(): Locator { return this.page.getByRole('button', { name: 'Edit Schema' })};
    get newSchemaTextbox(): Locator { return this.page.locator('#newSchema div').filter({ hasText: '{ "type": "record", "name": "' }).nth(1)};
    get submit(): Locator { return this.page.getByRole('button', { name: 'Submit' })};
    get incompatibeError(): Locator { return this.page.getByText('Schema being registered is')};
    get menu(): Locator { return this.page.getByRole('button', { name: 'Dropdown Toggle' })};
    get removeSchema(): Locator { return this.page.getByText('Remove schema')};
    get confirm(): Locator { return this.page.getByRole('button', { name: 'Confirm' })};

    heading(value: string): Locator { return this.page.getByText(`Schema Registry${value}`)};

    version(value: string): Locator { return this.page.getByText(`Latest version${value}`)};
    type(value: string): Locator { return this.page.getByRole('paragraph').filter({ hasText: value })};
    compatibility(value: string): Locator { return this.page.getByText(`Compatibility${value}`)};

    editDropdown(value: string): Locator { return this.page.getByRole('listbox').filter({ hasText: value }).getByRole('img')};
    editDropdownElement(value: string): Locator { return this.page.getByRole('list').getByRole('option', { name: value, exact: true })};
}