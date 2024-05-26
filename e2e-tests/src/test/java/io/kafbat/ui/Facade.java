package io.kafbat.ui;

import io.kafbat.ui.screens.brokers.BrokersConfigTab;
import io.kafbat.ui.screens.brokers.BrokersDetails;
import io.kafbat.ui.screens.brokers.BrokersList;
import io.kafbat.ui.screens.connectors.ConnectorCreateForm;
import io.kafbat.ui.screens.connectors.ConnectorDetails;
import io.kafbat.ui.screens.connectors.KafkaConnectList;
import io.kafbat.ui.screens.consumers.ConsumersDetails;
import io.kafbat.ui.screens.consumers.ConsumersList;
import io.kafbat.ui.screens.ksqldb.KsqlDbList;
import io.kafbat.ui.screens.ksqldb.KsqlQueryForm;
import io.kafbat.ui.screens.panels.NaviSideBar;
import io.kafbat.ui.screens.panels.TopPanel;
import io.kafbat.ui.screens.schemas.SchemaCreateForm;
import io.kafbat.ui.screens.schemas.SchemaDetails;
import io.kafbat.ui.screens.schemas.SchemaRegistryList;
import io.kafbat.ui.screens.topics.ProduceMessagePanel;
import io.kafbat.ui.screens.topics.TopicCreateEditForm;
import io.kafbat.ui.screens.topics.TopicDetails;
import io.kafbat.ui.screens.topics.TopicSettingsTab;
import io.kafbat.ui.screens.topics.TopicsList;
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
