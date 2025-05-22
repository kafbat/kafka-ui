import { Page, Locator } from "@playwright/test";

export default class ksqlDbLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    ksqlDbHeading = (): Locator => this.page.getByRole('heading', { name: 'KSQL DB' });
    ksqlDbExecuteKSQLREquestButton = (): Locator => this.page.getByRole('button', { name: 'Execute KSQL Request' });
    ksqlDbTablesLink = (): Locator => this.page.getByRole('link', { name: 'Tables' });
    ksqlDbStreamsLink = (): Locator => this.page.getByRole('link', { name: 'Streams' });

}