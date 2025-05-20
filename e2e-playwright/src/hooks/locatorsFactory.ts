import { Page } from "@playwright/test";

import PanelLocators from "../pages/Panel/PanelLocators";
import BrokersLocators from "../pages/Brokers/BrokersLocators";
import TopicsLocators from "../pages/Topics/TopicsLocators";
import TopicCreateLocators from "../pages/Topics/TopicsCreateLocators";
import ConsumersLocators from "../pages/Consumers/ConsumersLocators";
import SchemaRegistryLocators from "../pages/SchemaRegistry/SchemaRegistryLocators";
import ConnectorsLocators from "../pages/Connectors/ConnectorsLocators";
import ksqlDbLocators from "../pages/KSQLDB/ksqldbLocators";
import DashboardLocators from "../pages/Dashboard/DashboardLocators";

export class locatorsFactory {
  static panel(page: Page) {
    return new PanelLocators(page);
  }

  static brokers(page: Page) {
    return new BrokersLocators(page);
  }

  static topics(page: Page) {
    return new TopicsLocators(page);
  }

  static topicsCreate(page: Page) {
    return new TopicCreateLocators(page);
  }

  static consumers(page: Page) {
    return new ConsumersLocators(page);
  }

  static schemaRegistry(page: Page) {
    return new SchemaRegistryLocators(page);
  }

  static connectors(page: Page) {
    return new ConnectorsLocators(page);
  }

  static ksqlDb(page: Page) {
    return new ksqlDbLocators(page);
  }

  static dashboard(page: Page) {
    return new DashboardLocators(page);
  }
  
  static initAll(page: Page) {
    return {
      panel: this.panel(page),
      brokers: this.brokers(page),
      topics: this.topics(page),
      topicsCreate: this.topicsCreate(page),
      consumers: this.consumers(page),
      schemaRegistry: this.schemaRegistry(page),
      connectors: this.connectors(page),
      ksqlDb: this.ksqlDb(page),
      dashboard: this.dashboard(page)
    };
  }
}
