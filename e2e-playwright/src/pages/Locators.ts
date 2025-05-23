import { Page } from "@playwright/test";
import PanelLocators from "./Panel/PanelLocators";
import BrokersLocators from "./Brokers/BrokersLocators";
import TopicsLocators from "./Topics/TopicsLocators";
import TopicCreateLocators from "./Topics/TopicsCreateLocators";
import ConsumersLocators from "./Consumers/ConsumersLocators";
import SchemaRegistryLocators from "./SchemaRegistry/SchemaRegistryLocators";
import ConnectorsLocators from "./Connectors/ConnectorsLocators";
import ksqlDbLocators from "./KSQLDB/ksqldbLocators";
import DashboardLocators from "./Dashboard/DashboardLocators";

export class Locators {
  private readonly page: Page;

  private _panel?: PanelLocators;
  private _brokers?: BrokersLocators;
  private _topics?: TopicsLocators;
  private _topicsCreate?: TopicCreateLocators;
  private _consumers?: ConsumersLocators;
  private _schemaRegistry?: SchemaRegistryLocators;
  private _connectors?: ConnectorsLocators;
  private _ksqlDb?: ksqlDbLocators;
  private _dashboard?: DashboardLocators;

  constructor(page: Page) {
    this.page = page;
  }

  get panel() {
    return (this._panel ??= new PanelLocators(this.page));
  }

  get brokers() {
    return (this._brokers ??= new BrokersLocators(this.page));
  }

  get topics() {
    return (this._topics ??= new TopicsLocators(this.page));
  }

  get topicsCreate() {
    return (this._topicsCreate ??= new TopicCreateLocators(this.page));
  }

  get consumers() {
    return (this._consumers ??= new ConsumersLocators(this.page));
  }

  get schemaRegistry() {
    return (this._schemaRegistry ??= new SchemaRegistryLocators(this.page));
  }

  get connectors() {
    return (this._connectors ??= new ConnectorsLocators(this.page));
  }

  get ksqlDb() {
    return (this._ksqlDb ??= new ksqlDbLocators(this.page));
  }

  get dashboard() {
    return (this._dashboard ??= new DashboardLocators(this.page));
  }
}
