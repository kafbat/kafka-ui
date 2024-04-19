package io.kafbat.ui;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.kafbat.ui.screens.panels.enums.MenuItem;
import io.kafbat.ui.settings.BaseSource;
import io.kafbat.ui.settings.drivers.WebDriver;
import io.kafbat.ui.settings.listeners.AllureListener;
import io.kafbat.ui.settings.listeners.ResultsLogger;
import io.qameta.allure.Step;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.asserts.SoftAssert;

@Slf4j
@Listeners({AllureListener.class, ResultsLogger.class})
public abstract class BaseTest extends Facade {

  @BeforeSuite(alwaysRun = true)
  public void beforeSuite() {
    WebDriverManager.chromedriver().setup();
    WebDriver.selenideLoggerSetup();
    WebDriver.browserSetup();
  }

  @AfterSuite(alwaysRun = true)
  public void afterSuite() {
    WebDriver.browserQuit();
  }

  @BeforeMethod(alwaysRun = true)
  public void beforeMethod() {
    WebDriver.openUrl(BaseSource.BASE_UI_URL);
    naviSideBar.waitUntilScreenReady();
  }

  @AfterMethod(alwaysRun = true)
  public void afterMethod() {
    WebDriver.browserClear();
  }

  @Step
  protected void navigateToBrokers() {
    naviSideBar
        .openSideMenu(MenuItem.BROKERS);
    brokersList
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToBrokersAndOpenDetails(int brokerId) {
    navigateToBrokers();
    brokersList
        .openBroker(brokerId);
    brokersDetails
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToTopics() {
    naviSideBar
        .openSideMenu(MenuItem.TOPICS);
    topicsList
        .waitUntilScreenReady()
        .setShowInternalRadioButton(false);
  }

  @Step
  protected void navigateToTopicsAndOpenDetails(String topicName) {
    navigateToTopics();
    topicsList
        .openTopic(topicName);
    topicDetails
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToConsumers() {
    naviSideBar
        .openSideMenu(MenuItem.CONSUMERS);
    consumersList
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToSchemaRegistry() {
    naviSideBar
        .openSideMenu(MenuItem.SCHEMA_REGISTRY);
    schemaRegistryList
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToSchemaRegistryAndOpenDetails(String schemaName) {
    navigateToSchemaRegistry();
    schemaRegistryList
        .openSchema(schemaName);
    schemaDetails
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToConnectors() {
    naviSideBar
        .openSideMenu(MenuItem.KAFKA_CONNECT);
    kafkaConnectList
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToConnectorsAndOpenDetails(String connectorName) {
    navigateToConnectors();
    kafkaConnectList
        .openConnector(connectorName);
    connectorDetails
        .waitUntilScreenReady();
  }

  @Step
  protected void navigateToKsqlDb() {
    naviSideBar
        .openSideMenu(MenuItem.KSQL_DB);
    ksqlDbList
        .waitUntilScreenReady();
  }

  @Step
  protected void verifyElementsCondition(List<SelenideElement> elementList, WebElementCondition expectedCondition) {
    SoftAssert softly = new SoftAssert();
    elementList.forEach(element -> softly.assertTrue(element.is(expectedCondition),
        element.getSearchCriteria() + " is " + expectedCondition));
    softly.assertAll();
  }
}
