import { Page, Locator } from "@playwright/test";

export default class DashboardLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator  { return this.page.getByRole('heading', { name: 'Dashboard' })};
}