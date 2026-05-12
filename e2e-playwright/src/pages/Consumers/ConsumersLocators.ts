import { Page, Locator } from "@playwright/test";

export default class ConsumersLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator  { return this.page.getByRole('heading', { name: 'Consumers' })};
    get searchBox(): Locator { return this.page.getByRole('textbox', { name: 'Search by Consumer Group ID' })};
    get searchByGroupId(): Locator  { return this.page.getByText('Group ID')};
}