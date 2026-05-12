import { Page, Locator } from "@playwright/test";

export default class ProduceMessageLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get heading(): Locator { return this.page.getByText('Produce Message').nth(1); }
    get keySerdeDropdown(): Locator { return this.page.locator('#selectKeySerdeOptions').getByRole('img'); }
    get valueSerdeDropdown(): Locator { return this.page.locator('#selectValueSerdeOptions path'); }
    get keyTextbox(): Locator  { return this.page.locator('#key').getByRole('textbox'); }
    get valueTextbox(): Locator  { return this.page.locator('#content').getByRole('textbox'); }
    get headersTextbox(): Locator  { return this.page.locator('#headers').getByRole('textbox'); }
    get produceMessage():Locator { return this.page.locator('form').getByRole('button', { name: 'Produce Message' }); }

    partitionDropdown(vakue:string = 'Partition #'): Locator { return this.page.getByRole('listbox', { name: vakue }); }
    partitionDropdownElement(value: string): Locator { return this.page.getByRole('list').getByRole('option', { name: value }); }
    keySerdeDropdownElement(value: string): Locator { return this.page.getByRole('list').getByRole('option', { name: value }); }
    valueSerdeDropdownElement(value: string): Locator { return this.page.getByRole('list').getByRole('option', { name: value }); }
}