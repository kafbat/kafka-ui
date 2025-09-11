import { Page, Locator } from "@playwright/test";

export default class ConnectorsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get clustersTab(): Locator { return this.page.getByRole('link', { name: 'Clusters' })};
}
