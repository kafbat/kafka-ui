import { Page, Locator } from "@playwright/test";

export default class ksqlDbLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByRole('heading', { name: 'KSQL DB' })};
    get executeKSQLREquestButton(): Locator { return this.page.getByRole('button', { name: 'Execute KSQL Request' })};
    get tablesLink(): Locator { return this.page.getByRole('link', { name: 'Tables' })};
    get streamsLink(): Locator { return this.page.getByRole('link', { name: 'Streams' })};

}