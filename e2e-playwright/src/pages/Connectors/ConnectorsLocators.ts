import { Page, Locator } from "@playwright/test";

export default class ConnectorsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByRole('heading', { name: 'Connectors' })};
    get searchBox(): Locator { return this.page.getByRole('textbox', { name: 'SSearch by Connect Name' })};
    get createConnectorButton(): Locator { return this.page.getByRole('button', { name: 'Create Schema' })};
}