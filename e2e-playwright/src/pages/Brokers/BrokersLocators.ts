import { Page, Locator } from "@playwright/test";

export default class BrokersLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    brokersHeading = (): Locator => this.page.getByRole('heading', { name: 'Brokers' });
}