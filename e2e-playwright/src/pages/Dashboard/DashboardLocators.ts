import { Page, Locator } from "@playwright/test";

export default class DashboardLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    dashboardHeading = (): Locator => this.page.getByRole('heading', { name: 'Dashboard' });
}