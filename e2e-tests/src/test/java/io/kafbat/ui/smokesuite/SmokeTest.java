package io.kafbat.ui.smokesuite;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Connector;
import io.kafbat.ui.models.Schema;
import io.kafbat.ui.models.Topic;
import io.kafbat.ui.pages.panels.enums.MenuItem;
import io.kafbat.ui.settings.BaseSource;
import io.kafbat.ui.utilities.FileUtil;
import io.kafbat.ui.variables.Url;
import io.qameta.allure.Step;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SmokeTest extends BaseTest {

  private static final int BROKER_ID = 1;
  private static final Schema TEST_SCHEMA = Schema.createSchemaAvro();
  private static final Topic TEST_TOPIC = new Topic()
      .setName("new-topic-" + randomAlphabetic(5))
      .setNumberOfPartitions(1);
  private static final Connector TEST_CONNECTOR = new Connector()
      .setName("new-connector-" + randomAlphabetic(5))
      .setConfig(FileUtil.getResourceAsString("testData/connectors/config_for_create_connector_via_api.json"));

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    apiService
        .createTopic(TEST_TOPIC)
        .createSchema(TEST_SCHEMA)
        .createConnector(TEST_CONNECTOR);
  }

  @Test
  public void checkBasePageElements() {
    verifyElementsCondition(
        Stream.concat(topPanel.getAllVisibleElements().stream(), naviSideBar.getAllMenuButtons().stream())
            .collect(Collectors.toList()), Condition.visible);
    verifyElementsCondition(
        Stream.concat(topPanel.getAllEnabledElements().stream(), naviSideBar.getAllMenuButtons().stream())
            .collect(Collectors.toList()), Condition.enabled);
  }

  @Test
  public void checkUrlWhileNavigating() {
    navigateToBrokers();
    verifyCurrentUrl(Url.BROKERS_LIST_URL);
    navigateToTopics();
    verifyCurrentUrl(Url.TOPICS_LIST_URL);
    navigateToConsumers();
    verifyCurrentUrl(Url.CONSUMERS_LIST_URL);
    navigateToSchemaRegistry();
    verifyCurrentUrl(Url.SCHEMA_REGISTRY_LIST_URL);
    navigateToConnectors();
    verifyCurrentUrl(Url.KAFKA_CONNECT_LIST_URL);
    navigateToKsqlDb();
    verifyCurrentUrl(Url.KSQL_DB_LIST_URL);
  }

  @Test
  public void checkPathWhileNavigating() {
    navigateToBrokersAndOpenDetails(BROKER_ID);
    verifyComponentsPath(MenuItem.BROKERS, String.format("Broker %d", BROKER_ID));
    navigateToTopicsAndOpenDetails(TEST_TOPIC.getName());
    verifyComponentsPath(MenuItem.TOPICS, TEST_TOPIC.getName());
    navigateToSchemaRegistryAndOpenDetails(TEST_SCHEMA.getName());
    verifyComponentsPath(MenuItem.SCHEMA_REGISTRY, TEST_SCHEMA.getName());
    navigateToConnectorsAndOpenDetails(TEST_CONNECTOR.getName());
    verifyComponentsPath(MenuItem.KAFKA_CONNECT, TEST_CONNECTOR.getName());
  }

  @Step
  private void verifyCurrentUrl(String expectedUrl) {
    String urlWithoutParameters = WebDriverRunner.getWebDriver().getCurrentUrl();
    if (urlWithoutParameters.contains("?")) {
      urlWithoutParameters = urlWithoutParameters.substring(0, urlWithoutParameters.indexOf("?"));
    }
    Assert.assertEquals(urlWithoutParameters, String.format(expectedUrl, BaseSource.BASE_HOST), "getCurrentUrl()");
  }

  @Step
  private void verifyComponentsPath(MenuItem menuItem, String expectedPath) {
    Assert.assertEquals(naviSideBar.getPagePath(menuItem), expectedPath,
        String.format("getPagePath() for %s", menuItem.getPageTitle().toUpperCase()));
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    apiService
        .deleteTopic(TEST_TOPIC.getName())
        .deleteSchema(TEST_SCHEMA.getName())
        .deleteConnector(TEST_CONNECTOR.getName());
  }
}
