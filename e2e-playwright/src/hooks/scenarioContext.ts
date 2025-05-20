import PanelLocators from "../pages/Panel/PanelLocators";
import BrokersLocators from "../pages/Brokers/BrokersLocators";
import TopicsLocators from "../pages/Topics/TopicsLocators";
import TopicCreateLocators from "../pages/Topics/TopicsCreateLocators";
import ConsumersLocators from "../pages/Consumers/ConsumersLocators";
import SchemaRegistryLocators from "../pages/SchemaRegistry/SchemaRegistryLocators";
import ConnectorsLocators from "../pages/Connectors/ConnectorsLocators";
import ksqlDbLocators from "../pages/KSQLDB/ksqldbLocators";
import DashboardLocators from "../pages/Dashboard/DashboardLocators";

export const scenarioContext: Partial<{
  panel: PanelLocators;
  brokers: BrokersLocators;

  topics: TopicsLocators;
  topicsCreate: TopicCreateLocators;
  
  consumers: ConsumersLocators;
  schemaRegistry: SchemaRegistryLocators;
  connectors: ConnectorsLocators;
  ksqlDb: ksqlDbLocators;
  dashboard: DashboardLocators;
}> = {};