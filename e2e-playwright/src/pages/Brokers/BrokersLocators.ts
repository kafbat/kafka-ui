import { Page, Locator } from "@playwright/test";

export default class BrokersLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return  this.page.getByRole('heading', { name: 'Brokers' })};
}