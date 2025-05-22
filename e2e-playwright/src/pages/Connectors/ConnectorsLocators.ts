import { Page, Locator } from "@playwright/test";

export default class ConnectorsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    connectorsHeading = (): Locator => this.page.getByRole('heading', { name: 'Connectors' });
    connectorsSearchBox = (): Locator => this.page.getByRole('textbox', { name: 'SSearch by Connect Name' });
    connectorsCreateConnectorButton = (): Locator => this.page.getByRole('button', { name: 'Create Schema' });
}