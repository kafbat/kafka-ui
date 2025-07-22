package io.kafbat.ui.smokesuite.brokers;

import static io.kafbat.ui.utilities.IntUtil.getIntegerFromString;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.BaseTest;
import io.kafbat.ui.screens.brokers.BrokersConfigTab;
import io.kafbat.ui.screens.brokers.BrokersDetails;
import io.kafbat.ui.utilities.StringUtil;
import io.kafbat.ui.variables.Common;
import io.qameta.allure.Issue;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class BrokersTest extends BaseTest {

  public static final int DEFAULT_BROKER_ID = 1;

  @Test
  public void brokersOverviewCheck() {
    navigateToBrokers();
    Assert.assertFalse(brokersList.getAllBrokers().isEmpty(), "getAllBrokers()");
    verifyElementsCondition(brokersList.getAllVisibleElements(), Condition.visible);
    verifyElementsCondition(brokersList.getAllEnabledElements(), Condition.enabled);
  }

  @Test
  public void existingBrokersInClusterCheck() {
    navigateToBrokers();
    Assert.assertFalse(brokersList.getAllBrokers().isEmpty(), "getAllBrokers().isEmpty()");
    brokersList
        .openBroker(DEFAULT_BROKER_ID);
    brokersDetails
        .waitUntilScreenReady();
    verifyElementsCondition(brokersDetails.getAllVisibleElements(), Condition.visible);
    verifyElementsCondition(brokersDetails.getAllEnabledElements(), Condition.enabled);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    brokersConfigTab
        .waitUntilScreenReady();
    verifyElementsCondition(brokersConfigTab.getColumnHeaders(), Condition.visible);
    Assert.assertTrue(brokersConfigTab.isSearchByKeyVisible(), "isSearchByKeyVisible()");
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/209")
  @Test
  public void brokersConfigFirstPageSearchCheck() {
    navigateToBrokersAndOpenDetails(DEFAULT_BROKER_ID);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    String anyConfigKeyFirstPage = brokersConfigTab
        .getAllConfigs().stream()
        .findAny().orElseThrow()
        .getKey();
    brokersConfigTab
        .clickNextButton();
    Assert.assertFalse(brokersConfigTab.getAllConfigs().stream()
            .map(BrokersConfigTab.BrokersConfigItem::getKey)
            .toList().contains(anyConfigKeyFirstPage),
        String.format("getAllConfigs().contains()[%s]", anyConfigKeyFirstPage));
    brokersConfigTab
        .searchConfig(anyConfigKeyFirstPage);
    Assert.assertTrue(brokersConfigTab.getAllConfigs().stream()
            .map(BrokersConfigTab.BrokersConfigItem::getKey)
            .toList().contains(anyConfigKeyFirstPage),
        String.format("getAllConfigs().contains()[%s]", anyConfigKeyFirstPage));
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/209")
  @Test
  public void brokersConfigSecondPageSearchCheck() {
    navigateToBrokersAndOpenDetails(DEFAULT_BROKER_ID);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    brokersConfigTab
        .clickNextButton();
    String anyConfigKeySecondPage = brokersConfigTab
        .getAllConfigs().stream()
        .findAny().orElseThrow()
        .getKey();
    brokersConfigTab
        .clickPreviousButton();
    Assert.assertFalse(brokersConfigTab.getAllConfigs().stream()
            .map(BrokersConfigTab.BrokersConfigItem::getKey)
            .toList().contains(anyConfigKeySecondPage),
        String.format("getAllConfigs().contains()[%s]", anyConfigKeySecondPage));
    brokersConfigTab
        .searchConfig(anyConfigKeySecondPage);
    Assert.assertTrue(brokersConfigTab.getAllConfigs().stream()
            .map(BrokersConfigTab.BrokersConfigItem::getKey)
            .toList().contains(anyConfigKeySecondPage),
        String.format("getAllConfigs().contains()[%s]", anyConfigKeySecondPage));
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/209")
  @Test
  public void brokersConfigCaseInsensitiveSearchCheck() {
    navigateToBrokersAndOpenDetails(DEFAULT_BROKER_ID);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    String anyConfigKeyFirstPage = brokersConfigTab
        .getAllConfigs().stream()
        .findAny().orElseThrow()
        .getKey();
    brokersConfigTab
        .clickNextButton();
    Assert.assertFalse(brokersConfigTab.getAllConfigs().stream()
            .map(BrokersConfigTab.BrokersConfigItem::getKey)
            .toList().contains(anyConfigKeyFirstPage),
        String.format("getAllConfigs().contains()[%s]", anyConfigKeyFirstPage));
    SoftAssert softly = new SoftAssert();
    List.of(anyConfigKeyFirstPage.toLowerCase(), anyConfigKeyFirstPage.toUpperCase(),
            StringUtil.getMixedCase(anyConfigKeyFirstPage))
        .forEach(configCase -> {
          brokersConfigTab
              .searchConfig(configCase);
          softly.assertTrue(brokersConfigTab.getAllConfigs().stream()
                  .map(BrokersConfigTab.BrokersConfigItem::getKey)
                  .toList().contains(anyConfigKeyFirstPage),
              String.format("getAllConfigs().contains()[%s]", configCase));
        });
    softly.assertAll();
  }

  @Test
  public void brokersSourceInfoCheck() {
    navigateToBrokersAndOpenDetails(DEFAULT_BROKER_ID);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    String sourceInfoTooltip = brokersConfigTab
        .hoverOnSourceInfoIcon()
        .getSourceInfoTooltipText();
    Assert.assertEquals(sourceInfoTooltip, Common.BROKER_SOURCE_INFO_TOOLTIP, "getSourceInfoTooltipText()");
  }

  @Test(enabled = false) // flaky, TODO issues/322
  public void brokersConfigEditCheck() {
    navigateToBrokersAndOpenDetails(DEFAULT_BROKER_ID);
    brokersDetails
        .openDetailsTab(BrokersDetails.DetailsTab.CONFIGS);
    String configKey = "log.cleaner.min.compaction.lag.ms";
    BrokersConfigTab.BrokersConfigItem configItem = brokersConfigTab
        .searchConfig(configKey)
        .getConfig(configKey);
    int defaultValue = getIntegerFromString(configItem.getValue(), false);
    configItem
        .clickEditBtn();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(configItem.getSaveBtn().isDisplayed(), "getSaveBtn().isDisplayed()");
    softly.assertTrue(configItem.getCancelBtn().isDisplayed(), "getCancelBtn().isDisplayed()");
    softly.assertTrue(configItem.getValueFld().isEnabled(), "getValueFld().isEnabled()");
    softly.assertAll();
    int newValue = defaultValue + 1;
    configItem
        .setValue(String.valueOf(newValue))
        .clickCancelBtn();
    Assert.assertEquals(getIntegerFromString(configItem.getValue(), true), defaultValue,
        "configItem.getValue()");
    configItem
        .clickEditBtn()
        .setValue(String.valueOf(newValue))
        .clickSaveBtn()
        .clickConfirm();
    configItem = brokersConfigTab
        .searchConfig(configKey)
        .getConfig(configKey);
    softly.assertFalse(configItem.getSaveBtn().isDisplayed(), "getSaveBtn().isDisplayed()");
    softly.assertFalse(configItem.getCancelBtn().isDisplayed(), "getCancelBtn().isDisplayed()");
    softly.assertTrue(configItem.getEditBtn().isDisplayed(), "getEditBtn().isDisplayed()");
    softly.assertEquals(getIntegerFromString(configItem.getValue(), true), newValue,
        "configItem.getValue()");
    softly.assertAll();
  }
}
