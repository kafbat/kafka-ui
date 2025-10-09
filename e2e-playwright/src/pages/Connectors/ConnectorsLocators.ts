import { Page, Locator } from "@playwright/test";

export default class ConnectorsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByRole('heading', { name: 'Kafka Connect' })};
    get searchBox(): Locator { return this.page.getByRole('textbox', { name: 'Search by Connect Name' })};
    get createConnectorButton(): Locator { return this.page.getByRole('button', { name: 'Create Schema' })};
    get rowMenu():Locator { return this.page.getByRole('cell', { name: 'Dropdown Toggle' })};
    get internalMenuButton(): Locator { return this.page.locator('button').filter({ hasText: 'Restart' })};

    rowData(value:string): Locator { return this.page.getByRole('cell', { name: value })};
    rowMenuItem(value:string): Locator { return this.page.getByRole('menuitem', { name: value })};

    cellData(value:string): Locator { return this.page.getByText(value, { exact: true })};

    localMenuLabel(value:string): Locator { return this.page.getByText(value)};

    internalMenuCell(value:string): Locator { return this.page.getByRole('cell', { name: value })};
    internalMenuButtonItem(value:string): Locator { return this.page.getByRole('menuitem', { name: value })};
    internalMenuState(value:string): Locator { return this.page.getByRole('group').getByText(value)};
}
