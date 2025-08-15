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
    get tablesHeader(): Locator { return this.page.getByTitle('Tables').locator('div')};
    get streamsHeader(): Locator { return this.page.getByTitle('Streams').locator('div')};
    get textField(): Locator { return this.page.locator('.ace_content')};
    get clear(): Locator { return this.page.getByRole('button', { name: 'Clear', exact: true })};
    get execute(): Locator { return this.page.getByRole('button', { name: 'Execute', exact: true })};
    get success(): Locator { return this.page.getByRole('cell', { name: 'SUCCESS' })};
    get streamCSreated(): Locator { return this.page.getByRole('cell', { name: 'Stream created' })};
    get clearResults(): Locator { return this.page.getByRole('button', { name: 'Clear results' })};
    get querySucceed(): Locator { return this.page.getByRole('heading', { name: 'Query succeed' })};

    get consumingQueryExecution(): Locator { return this.page.getByText('Consuming query execution')};
    get abort(): Locator { return this.page.getByText('Abort')};
    get cancelled(): Locator { return this.page.getByText('Cancelled')};
}