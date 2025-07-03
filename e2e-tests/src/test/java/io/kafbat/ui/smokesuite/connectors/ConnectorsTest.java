package io.kafbat.ui.smokesuite.connectors;

import static io.kafbat.ui.models.Topic.createTopic;
import static io.kafbat.ui.screens.BasePage.AlertHeader.SUCCESS;
import static io.kafbat.ui.utilities.FileUtil.resourceToString;

import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Connector;
import io.kafbat.ui.models.Topic;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class ConnectorsTest extends BaseTest {

  private static final String MESSAGE_CONTENT = resourceToString("testdata/topics/create_topic_content.json");
  private static final Topic CREATE_TOPIC = createTopic().setMessageValue(MESSAGE_CONTENT);
  private static final Topic DELETE_TOPIC = createTopic().setMessageValue(MESSAGE_CONTENT);
  private static final Topic UPDATE_TOPIC = createTopic().setMessageValue(MESSAGE_CONTENT);
  private static final Connector DELETE_CONNECTOR =
      Connector.createConnector(resourceToString("testdata/connectors/delete_config.json"));
  private static final Connector UPDATE_CONNECTOR =
      Connector.createConnector(resourceToString("testdata/connectors/create_config_api.json"));
  private static final List<Topic> TOPIC_LIST = new ArrayList<>();
  private static final List<Connector> CONNECTOR_LIST = new ArrayList<>();

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    TOPIC_LIST.addAll(List.of(CREATE_TOPIC, DELETE_TOPIC, UPDATE_TOPIC));
    TOPIC_LIST.forEach(topic -> apiService
        .createTopic(topic)
        .sendMessage(topic));
    CONNECTOR_LIST.addAll(List.of(DELETE_CONNECTOR, UPDATE_CONNECTOR));
    CONNECTOR_LIST.forEach(connector -> apiService.createConnector(connector));
  }

  @Ignore
  @Test
  public void createConnectorCheck() {
    Connector createConnector =
        Connector.createConnector(resourceToString("testdata/connectors/create_config.json"));
    navigateToConnectors();
    kafkaConnectList
        .clickCreateConnectorBtn();
    connectorCreateForm
        .waitUntilScreenReady()
        .setConnectorDetails(createConnector.getName(), createConnector.getConfig())
        .clickSubmitButton();
    connectorDetails
        .waitUntilScreenReady();
    navigateToConnectorsAndOpenDetails(createConnector.getName());
    Assert.assertTrue(connectorDetails.isConnectorHeaderVisible(createConnector.getName()),
        String.format("isConnectorHeaderVisible()[%s]", createConnector.getName()));
    navigateToConnectors();
    Assert.assertTrue(kafkaConnectList.isConnectorVisible(DELETE_CONNECTOR.getName()),
        String.format("isConnectorVisible()[%s]", DELETE_CONNECTOR.getName()));
    CONNECTOR_LIST.add(createConnector);
  }

  @Ignore
  @Test
  public void updateConnectorCheck() {
    navigateToConnectorsAndOpenDetails(UPDATE_CONNECTOR.getName());
    connectorDetails
        .openConfigTab()
        .setConfig(UPDATE_CONNECTOR.getConfig())
        .clickSubmitButton();
    Assert.assertTrue(connectorDetails.isAlertWithMessageVisible(SUCCESS, "Config successfully updated."),
        "isAlertWithMessageVisible()");
    navigateToConnectors();
    Assert.assertTrue(kafkaConnectList.isConnectorVisible(UPDATE_CONNECTOR.getName()),
        String.format("isConnectorVisible()[%s]", UPDATE_CONNECTOR.getName()));
  }

  @Ignore
  @Test
  public void deleteConnectorCheck() {
    navigateToConnectorsAndOpenDetails(DELETE_CONNECTOR.getName());
    connectorDetails
        .openDotMenu()
        .clickDeleteBtn()
        .clickConfirmBtn();
    navigateToConnectors();
    Assert.assertFalse(kafkaConnectList.isConnectorVisible(DELETE_CONNECTOR.getName()),
        String.format("isConnectorVisible()[%s]", DELETE_CONNECTOR.getName()));
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    CONNECTOR_LIST.forEach(connector ->
        apiService.deleteConnector(connector.getName()));
    TOPIC_LIST.forEach(topic -> apiService.deleteTopic(topic.getName()));
  }
}
