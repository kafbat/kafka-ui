import { Page, Locator } from "@playwright/test";

export default class TopicsTopickNameLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    get overview():Locator { return this.page.getByRole('link', { name: 'Overview' }); }
    get messages():Locator { return this.page.getByRole('link', { name: 'Messages' }); }
    get consumers():Locator { return this.page.getByRole('main').getByRole('navigation').getByRole('link', { name: 'Consumers' }); }
    get settings():Locator { return this.page.getByRole('link', { name: 'Settings' }); }
    get statistics():Locator { return this.page.getByRole('link', { name: 'Statistics' }); }
    get produceMessage():Locator { return this.page.getByRole('button', { name: 'Produce Message' }).first(); }
    get keyButton():Locator { return this.page.getByRole('button', { name: 'Key' }); }
    get valueButton():Locator { return this.page.getByRole('button', { name: 'Value' }); }
    get headersButton():Locator { return this.page.getByRole('button', { name: 'Headers' }); }

    heading(topicName: string): Locator { return this.page.getByText(`Topics${topicName}`); }
    partitions(value: string):Locator { return this.page.getByRole('group').getByText(value).first(); }
    messageKey(value: string):Locator  { return this.page.getByText(value, { exact: true }); }
    messageValue(value: string):Locator  { return this.page.getByText(value).first(); }

    messageKeyTextbox(value: string):Locator  { return this.page.locator('#schema div').filter({ hasText: value }).nth(1); }
    messageValueTextbox(value: string):Locator  { return this.page.locator('div').filter({ hasText:value }).nth(1); }
    messageHeadersTextbox(value: string):Locator  { return this.page.locator('div').filter({ hasText: value }).nth(1); }
}