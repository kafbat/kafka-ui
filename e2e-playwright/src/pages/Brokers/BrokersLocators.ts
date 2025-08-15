import { Page, Locator } from "@playwright/test";

export default class BrokersLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return  this.page.getByRole('heading', { name: 'Brokers' })};
    get uptime(): Locator { return  this.page.getByRole('heading', { name: 'Uptime' })};
    get partitions(): Locator { return  this.page.getByRole('heading', { name: 'Partitions' })};

    toBroker(value: string): Locator { return this.page.getByRole('cell', { name: value, exact: true }); }
}