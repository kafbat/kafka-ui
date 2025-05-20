import { Page, Locator } from "@playwright/test";

export default class PanelLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    private linkByName = (name: string): Locator =>
      this.page.getByRole('link', { name });
  
    brokersLink = (): Locator => this.linkByName('Brokers');
    topicsLink = (): Locator => this.page.getByTitle('Topics');
    consumersLink = (): Locator => this.linkByName('Consumers');
    schemaRegistryLink = (): Locator => this.linkByName('Schema Registry');
    ksqlDbLink = (): Locator => this.linkByName('KSQL DB');
    getDashboardLink = (): Locator => this.linkByName('Dashboard');
    kafkaConnectLink = (): Locator => this.linkByName('Kafka Connect');
}