package io.kafbat.ui;

import io.kafbat.ui.pages.brokers.BrokersConfigTab;
import io.kafbat.ui.pages.brokers.BrokersDetails;
import io.kafbat.ui.pages.brokers.BrokersList;
import io.kafbat.ui.pages.connectors.ConnectorCreateForm;
import io.kafbat.ui.pages.connectors.ConnectorDetails;
import io.kafbat.ui.pages.connectors.KafkaConnectList;
import io.kafbat.ui.pages.consumers.ConsumersDetails;
import io.kafbat.ui.pages.consumers.ConsumersList;
import io.kafbat.ui.pages.ksqldb.KsqlDbList;
import io.kafbat.ui.pages.ksqldb.KsqlQueryForm;
import io.kafbat.ui.pages.panels.NaviSideBar;
import io.kafbat.ui.pages.panels.TopPanel;
import io.kafbat.ui.pages.schemas.SchemaCreateForm;
import io.kafbat.ui.pages.schemas.SchemaDetails;
import io.kafbat.ui.pages.schemas.SchemaRegistryList;
import io.kafbat.ui.pages.topics.ProduceMessagePanel;
import io.kafbat.ui.pages.topics.TopicCreateEditForm;
import io.kafbat.ui.pages.topics.TopicDetails;
import io.kafbat.ui.pages.topics.TopicSettingsTab;
import io.kafbat.ui.pages.topics.TopicsList;
import io.kafbat.ui.services.ApiService;

public abstract class Facade {

  protected ApiService apiService = new ApiService();
  protected ConnectorCreateForm connectorCreateForm = new ConnectorCreateForm();
  protected KafkaConnectList kafkaConnectList = new KafkaConnectList();
  protected ConnectorDetails connectorDetails = new ConnectorDetails();
  protected SchemaCreateForm schemaCreateForm = new SchemaCreateForm();
  protected SchemaDetails schemaDetails = new SchemaDetails();
  protected SchemaRegistryList schemaRegistryList = new SchemaRegistryList();
  protected ProduceMessagePanel produceMessagePanel = new ProduceMessagePanel();
  protected TopicCreateEditForm topicCreateEditForm = new TopicCreateEditForm();
  protected TopicsList topicsList = new TopicsList();
  protected TopicDetails topicDetails = new TopicDetails();
  protected ConsumersDetails consumersDetails = new ConsumersDetails();
  protected ConsumersList consumersList = new ConsumersList();
  protected NaviSideBar naviSideBar = new NaviSideBar();
  protected TopPanel topPanel = new TopPanel();
  protected BrokersList brokersList = new BrokersList();
  protected BrokersDetails brokersDetails = new BrokersDetails();
  protected BrokersConfigTab brokersConfigTab = new BrokersConfigTab();
  protected TopicSettingsTab topicSettingsTab = new TopicSettingsTab();
  protected KsqlQueryForm ksqlQueryForm = new KsqlQueryForm();
  protected KsqlDbList ksqlDbList = new KsqlDbList();
}
