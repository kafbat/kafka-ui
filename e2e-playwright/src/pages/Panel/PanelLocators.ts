import { Page, Locator } from "@playwright/test";

export default class PanelLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    linkByName(name: string): Locator {
      return this.page.getByRole('link', { name });
    }

    get brokersLink(): Locator { return this.linkByName('Brokers');}
    get topicsLink(): Locator { return this.page.getByTitle('Topics');}
    get consumersLink(): Locator { return this.linkByName('Consumers');}
    get schemaRegistryLink(): Locator { return this.linkByName('Schema Registry');}
    get ksqlDbLink(): Locator { return this.linkByName('KSQL DB');}
    get getDashboardLink(): Locator { return this.linkByName('Dashboard');}
    get kafkaConnectLink(): Locator { return this.linkByName('Kafka Connect');}
}