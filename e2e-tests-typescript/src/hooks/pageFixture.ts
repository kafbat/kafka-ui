import { Page } from "@playwright/test";
import { Logger } from "winston";
import PanelLocators from "../pages/Panel/PanelLocators";
import BrokersLocators from "../pages/Brokers/BrokersLocators";
import TopicsLocators from "../pages/Topics/TopicsLocators";
import ConsumersLocators from "../pages/Consumers/ConsumersLocators";
import SchemaRegistryLocators from "../pages/SchemaRegistry/SchemaRegistryLocators";
import ConnectorsLocators from "../pages/Connectors/ConnectorsLocators";
import ksqlDbLocators from "../pages/KSQLDB/ksqldbLocators";
import DashboardLocators from "../pages/Dashboard/DashboardLocators";

export const fixture = {
    // @ts-ignore
    page: undefined as Page,
    // @ts-ignore
    logger: undefined as Logger,
    // @ts-ignore
    navigationPanel: undefined as PanelLocators,
    // @ts-ignore
    brokers: undefined as BrokersLocators,
    // @ts-ignore
    topics: undefined as TopicsLocators,
    // @ts-ignore
    consumers: undefined as ConsumersLocators,
    // @ts-ignore
    schemaRegistry: undefined as SchemaRegistryLocators,
    // @ts-ignore
    connectors: undefined as ConnectorsLocators,
    // @ts-ignore
    ksqlDb: undefined as ksqlDbLocators,
    // @ts-ignore
    dashboard: undefined as DashboardLocators
}