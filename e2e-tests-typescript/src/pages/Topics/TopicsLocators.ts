import { Page, Locator } from "@playwright/test";

export default class TopicsLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    topicsHeading = (): Locator => this.page.getByRole('heading', { name: 'Topics' });
    topicsAddButton = (): Locator => this.page.getByRole('button', { name: 'Add a Topic' });
}