import { Page, Locator } from "@playwright/test";

export default class PanelLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    linkByName(name: string): Locator{
      return this.page.getByRole('link', { name });
    }
  
    brokersLink(): Locator { return this.linkByName('Brokers');}
    topicsLink(): Locator { return this.page.getByTitle('Topics');}
    consumersLink(): Locator { return this.linkByName('Consumers');}
    schemaRegistryLink(): Locator { return this.linkByName('Schema Registry');}
    ksqlDbLink(): Locator { return this.linkByName('KSQL DB');}
    getDashboardLink(): Locator { return this.linkByName('Dashboard');}
    kafkaConnectLink(): Locator { return this.linkByName('Kafka Connect');}
}