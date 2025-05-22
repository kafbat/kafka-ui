import { Page, Locator } from "@playwright/test";

export default class ConsumersLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    consumersHeading = (): Locator => this.page.getByRole('heading', { name: 'Consumers' });
    consumersSearchBox = (): Locator => this.page.getByRole('textbox', { name: 'Search by Consumer Group ID' });
    consumersSearchByGroupId = (): Locator => this.page.getByText('Group ID');
}