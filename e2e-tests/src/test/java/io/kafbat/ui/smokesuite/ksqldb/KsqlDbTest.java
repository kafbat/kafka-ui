package io.kafbat.ui.smokesuite.ksqldb;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.kafbat.ui.BaseTest;
import io.kafbat.ui.pages.ksqldb.enums.KsqlMenuTabs;
import io.kafbat.ui.pages.ksqldb.enums.KsqlQueryConfig;
import io.kafbat.ui.pages.ksqldb.models.Stream;
import io.kafbat.ui.pages.ksqldb.models.Table;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class KsqlDbTest extends BaseTest {

  private static final Stream DEFAULT_STREAM = new Stream()
      .setName("DEFAULT_STREAM_" + randomAlphabetic(4).toUpperCase())
      .setTopicName("DEFAULT_TOPIC_" + randomAlphabetic(4).toUpperCase());
  private static final Table FIRST_TABLE = new Table()
      .setName("FIRST_TABLE_" + randomAlphabetic(4).toUpperCase())
      .setStreamName(DEFAULT_STREAM.getName());
  private static final Table SECOND_TABLE = new Table()
      .setName("SECOND_TABLE_" + randomAlphabetic(4).toUpperCase())
      .setStreamName(DEFAULT_STREAM.getName());
  private static final List<String> TOPIC_NAMES_LIST = new ArrayList<>();

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    apiService
        .createStream(DEFAULT_STREAM)
        .createTables(FIRST_TABLE, SECOND_TABLE);
    TOPIC_NAMES_LIST.addAll(List.of(DEFAULT_STREAM.getTopicName(),
        FIRST_TABLE.getName(), SECOND_TABLE.getName()));
  }

  @Test(priority = 1)
  public void streamsAndTablesVisibilityCheck() {
    navigateToKsqlDb();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(ksqlDbList.getTableByName(FIRST_TABLE.getName()).isVisible(), "getTableByName()");
    softly.assertTrue(ksqlDbList.getTableByName(SECOND_TABLE.getName()).isVisible(), "getTableByName()");
    softly.assertAll();
    ksqlDbList
        .openDetailsTab(KsqlMenuTabs.STREAMS)
        .waitUntilScreenReady();
    Assert.assertTrue(ksqlDbList.getStreamByName(DEFAULT_STREAM.getName()).isVisible(), "getStreamByName()");
  }

  @Test(priority = 2)
  public void clearEnteredQueryCheck() {
    navigateToKsqlDbAndExecuteRequest(KsqlQueryConfig.SHOW_TABLES.getQuery());
    Assert.assertFalse(ksqlQueryForm.getEnteredQuery().isEmpty(), "getEnteredQuery()");
    ksqlQueryForm
        .clickClearBtn();
    Assert.assertTrue(ksqlQueryForm.getEnteredQuery().isEmpty(), "getEnteredQuery()");
  }

  @Test(priority = 3)
  public void clearResultsButtonCheck() {
    String notValidQuery = "some not valid request";
    navigateToKsqlDb();
    ksqlDbList
        .clickExecuteKsqlRequestBtn();
    ksqlQueryForm
        .waitUntilScreenReady()
        .setQuery(notValidQuery);
    Assert.assertFalse(ksqlQueryForm.isClearResultsBtnEnabled(), "isClearResultsBtnEnabled()");
    ksqlQueryForm
        .clickExecuteBtn(notValidQuery);
    Assert.assertFalse(ksqlQueryForm.isClearResultsBtnEnabled(), "isClearResultsBtnEnabled()");
  }

  @Test(priority = 4)
  public void checkShowTablesRequestExecution() {
    navigateToKsqlDbAndExecuteRequest(KsqlQueryConfig.SHOW_TABLES.getQuery());
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(ksqlQueryForm.areResultsVisible(), "areResultsVisible()");
    softly.assertTrue(ksqlQueryForm.getItemByName(FIRST_TABLE.getName()).isVisible(),
        String.format("getItemByName(%s)", FIRST_TABLE.getName()));
    softly.assertTrue(ksqlQueryForm.getItemByName(SECOND_TABLE.getName()).isVisible(),
        String.format("getItemByName(%s)", SECOND_TABLE.getName()));
    softly.assertAll();
  }

  @Test(priority = 5)
  public void checkShowStreamsRequestExecution() {
    navigateToKsqlDbAndExecuteRequest(KsqlQueryConfig.SHOW_STREAMS.getQuery());
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(ksqlQueryForm.areResultsVisible(), "areResultsVisible()");
    softly.assertTrue(ksqlQueryForm.getItemByName(DEFAULT_STREAM.getName()).isVisible(),
        String.format("getItemByName(%s)", FIRST_TABLE.getName()));
    softly.assertAll();
  }

  @Test(priority = 6)
  public void clearResultsForExecutedRequest() {
    navigateToKsqlDbAndExecuteRequest(KsqlQueryConfig.SHOW_TABLES.getQuery());
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(ksqlQueryForm.areResultsVisible(), "areResultsVisible()");
    softly.assertAll();
    ksqlQueryForm
        .clickClearResultsBtn();
    softly.assertFalse(ksqlQueryForm.areResultsVisible(), "areResultsVisible()");
    softly.assertAll();
  }

  @Test(priority = 7)
  public void stopQueryFunctionalCheck() {
    navigateToKsqlDbAndExecuteRequest(String.format(KsqlQueryConfig.SELECT_ALL_FROM.getQuery(), FIRST_TABLE.getName()));
    Assert.assertTrue(ksqlQueryForm.isAbortBtnVisible(), "isAbortBtnVisible()");
    ksqlQueryForm
        .clickAbortBtn();
    Assert.assertTrue(ksqlQueryForm.isCancelledAlertVisible(), "isCancelledAlertVisible()");
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    TOPIC_NAMES_LIST.forEach(topicName -> apiService.deleteTopic(topicName));
  }

  @Step
  private void navigateToKsqlDbAndExecuteRequest(String query) {
    navigateToKsqlDb();
    ksqlDbList
        .clickExecuteKsqlRequestBtn();
    ksqlQueryForm
        .waitUntilScreenReady()
        .setQuery(query)
        .clickExecuteBtn(query);
  }
}
