import { Page, Locator } from "@playwright/test";

export default class BrokerDetailsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get logDirectors(): Locator { return  this.page.getByRole('link', { name: 'Log directories' })};
    get firstDefaultCell(): Locator { return  this.page.getByRole('cell', { name: '/tmp/kraft-combined-logs' })};
    get configs(): Locator { return  this.page.getByRole('link', { name: 'Configs' })};
    get configsTextbox(): Locator { return  this.page.getByRole('textbox', { name: 'Search by Key or Value' })};
    get metrics(): Locator { return  this.page.getByRole('link', { name: 'Metrics' })};
    get configsKey(): Locator { return  this.page.getByRole('cell', { name: 'Key' })};
    get configsValue(): Locator { return  this.page.getByRole('cell', { name: 'Value' })};
    get configsSource(): Locator { return  this.page.getByRole('cell', { name: 'Source' })};

    configCell(value: string): Locator { return  this.page.getByRole('cell', { name: value })};
    header(value: string): Locator { return  this.page.getByText(value)};
}