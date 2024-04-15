package io.kafbat.ui.smokesuite.topics;

import static io.kafbat.ui.screens.BasePage.AlertHeader.SUCCESS;
import static io.kafbat.ui.variables.Common.FILTER_CODE_JSON;
import static io.kafbat.ui.variables.Common.FILTER_CODE_STRING;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomUtils.nextInt;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Topic;
import io.kafbat.ui.screens.topics.TopicDetails;
import io.kafbat.ui.screens.topics.enums.CleanupPolicyValue;
import io.kafbat.ui.screens.topics.enums.CustomParameterType;
import io.kafbat.ui.screens.topics.enums.MaxSizeOnDisk;
import io.kafbat.ui.screens.topics.enums.TimeToRetain;
import io.qameta.allure.Issue;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class TopicsTest extends BaseTest {

  private static final Topic CREATE_TOPIC = Topic.createTopic()
      .setCustomParameterType(CustomParameterType.COMPRESSION_TYPE)
      .setCustomParameterValue("producer")
      .setCleanupPolicyValue(CleanupPolicyValue.DELETE);
  private static final Topic UPDATE_TOPIC = Topic.createTopic()
      .setCleanupPolicyValue(CleanupPolicyValue.DELETE)
      .setTimeToRetain(TimeToRetain.BTN_7_DAYS)
      .setMaxSizeOnDisk(MaxSizeOnDisk.NOT_SET)
      .setMaxMessageBytes("1048588")
      .setMessageKey(randomAlphabetic(5))
      .setMessageValue(randomAlphabetic(10));
  private static final Topic DELETE_TOPIC = Topic.createTopic();
  private static final Topic SETTINGS_TOPIC = Topic.createTopic()
      .setMaxMessageBytes("1000012")
      .setMaxSizeOnDisk(MaxSizeOnDisk.NOT_SET);
  private static final Topic FILTERS_TOPIC = Topic.createTopic();
  private static final List<Topic> TOPIC_LIST = new ArrayList<>();

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    TOPIC_LIST.addAll(List.of(UPDATE_TOPIC, DELETE_TOPIC, FILTERS_TOPIC));
    TOPIC_LIST.forEach(topic -> apiService.createTopic(topic));
  }

  @Test(priority = 1)
  public void createTopicCheck() {
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(CREATE_TOPIC.getName())
        .setNumberOfPartitions(CREATE_TOPIC.getNumberOfPartitions())
        .selectCleanupPolicy(CREATE_TOPIC.getCleanupPolicyValue())
        .clickSaveTopicBtn();
    navigateToTopicsAndOpenDetails(CREATE_TOPIC.getName());
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(topicDetails.isTopicHeaderVisible(CREATE_TOPIC.getName()),
        String.format("isTopicHeaderVisible()[%s]", CREATE_TOPIC.getName()));
    softly.assertEquals(topicDetails.getCleanUpPolicy(), CREATE_TOPIC.getCleanupPolicyValue().toString(),
        "getCleanUpPolicy()");
    softly.assertEquals(topicDetails.getPartitions(), CREATE_TOPIC.getNumberOfPartitions(), "getPartitions()");
    softly.assertAll();
    navigateToTopics();
    Assert.assertTrue(topicsList.isTopicVisible(CREATE_TOPIC.getName()),
        String.format("isTopicVisible()[%s]", CREATE_TOPIC.getName()));
    TOPIC_LIST.add(CREATE_TOPIC);
  }

  @Test(priority = 2)
  void availableOperationsCheck() {
    navigateToTopics();
    topicsList
        .getTopicItem(UPDATE_TOPIC.getName())
        .selectItem(true);
    verifyElementsCondition(topicsList.getActionButtons(), Condition.enabled);
    topicsList
        .getTopicItem(FILTERS_TOPIC.getName())
        .selectItem(true);
    Assert.assertFalse(topicsList.isCopySelectedTopicBtnEnabled(), "isCopySelectedTopicBtnEnabled()");
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/279")
  @Test(priority = 3)
  public void customParametersWithinEditExistingTopicCheck() {
    navigateToTopicsAndOpenDetails(UPDATE_TOPIC.getName());
    topicDetails
        .openDotMenu()
        .clickEditSettingsMenu();
    SoftAssert softly = new SoftAssert();
    topicCreateEditForm
        .waitUntilScreenReady()
        .clickAddCustomParameterTypeButton()
        .openCustomParameterTypeDdl()
        .getAllDdlOptions()
        .asFixedIterable()
        .forEach(option ->
            softly.assertTrue(!option.is(Condition.attribute("disabled")),
                option.getText() + " is enabled:"));
    softly.assertAll();
  }

  @Test(priority = 4)
  public void updateTopicCheck() {
    navigateToTopicsAndOpenDetails(UPDATE_TOPIC.getName());
    topicDetails
        .openDotMenu()
        .clickEditSettingsMenu();
    topicCreateEditForm
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertEquals(topicCreateEditForm.getCleanupPolicy(),
        UPDATE_TOPIC.getCleanupPolicyValue().getVisibleText(), "getCleanupPolicy()");
    softly.assertEquals(topicCreateEditForm.getTimeToRetain(),
        UPDATE_TOPIC.getTimeToRetain().getValue(), "getTimeToRetain()");
    softly.assertEquals(topicCreateEditForm.getMaxSizeOnDisk(),
        UPDATE_TOPIC.getMaxSizeOnDisk().getVisibleText(), "getMaxSizeOnDisk()");
    softly.assertEquals(topicCreateEditForm.getMaxMessageBytes(),
        UPDATE_TOPIC.getMaxMessageBytes(), "getMaxMessageBytes()");
    softly.assertAll();
    UPDATE_TOPIC
        .setCleanupPolicyValue(CleanupPolicyValue.COMPACT)
        .setTimeToRetain(TimeToRetain.BTN_2_DAYS)
        .setMaxSizeOnDisk(MaxSizeOnDisk.SIZE_50_GB).setMaxMessageBytes("1048589");
    topicCreateEditForm
        .selectCleanupPolicy((UPDATE_TOPIC.getCleanupPolicyValue()))
        .setTimeToRetainDataByButtons(UPDATE_TOPIC.getTimeToRetain())
        .setMaxSizeOnDiskInGB(UPDATE_TOPIC.getMaxSizeOnDisk())
        .setMaxMessageBytes(UPDATE_TOPIC.getMaxMessageBytes())
        .clickSaveTopicBtn();
    softly.assertTrue(topicDetails.isAlertWithMessageVisible(SUCCESS, "Topic successfully updated."),
        "isAlertWithMessageVisible()");
    softly.assertTrue(topicDetails.isTopicHeaderVisible(UPDATE_TOPIC.getName()),
        String.format("isTopicHeaderVisible()[%s]", UPDATE_TOPIC.getName()));
    softly.assertAll();
    topicDetails
        .waitUntilScreenReady();
    navigateToTopicsAndOpenDetails(UPDATE_TOPIC.getName());
    topicDetails
        .openDotMenu()
        .clickEditSettingsMenu();
    softly.assertFalse(topicCreateEditForm.isNameFieldEnabled(), "isNameFieldEnabled()");
    softly.assertEquals(topicCreateEditForm.getCleanupPolicy(),
        UPDATE_TOPIC.getCleanupPolicyValue().getVisibleText(), "getCleanupPolicy()");
    softly.assertEquals(topicCreateEditForm.getTimeToRetain(),
        UPDATE_TOPIC.getTimeToRetain().getValue(), "getTimeToRetain()");
    softly.assertEquals(topicCreateEditForm.getMaxSizeOnDisk(),
        UPDATE_TOPIC.getMaxSizeOnDisk().getVisibleText(), "getMaxSizeOnDisk()");
    softly.assertEquals(topicCreateEditForm.getMaxMessageBytes(),
        UPDATE_TOPIC.getMaxMessageBytes(), "getMaxMessageBytes()");
    softly.assertAll();
  }

  @Test(priority = 5)
  public void removeTopicFromListCheck() {
    navigateToTopics();
    topicsList
        .openDotMenuByTopicName(UPDATE_TOPIC.getName())
        .clickRemoveTopicBtn()
        .clickConfirmBtnMdl();
    Assert.assertTrue(topicsList.isAlertWithMessageVisible(SUCCESS,
            String.format("Topic %s successfully deleted!", UPDATE_TOPIC.getName())),
        "isAlertWithMessageVisible()");
    TOPIC_LIST.remove(UPDATE_TOPIC);
  }

  @Test(priority = 6)
  public void deleteTopicCheck() {
    navigateToTopicsAndOpenDetails(DELETE_TOPIC.getName());
    topicDetails
        .openDotMenu()
        .clickDeleteTopicMenu()
        .clickConfirmBtnMdl();
    navigateToTopics();
    Assert.assertFalse(topicsList.isTopicVisible(DELETE_TOPIC.getName()),
        String.format("isTopicVisible()[%s]", DELETE_TOPIC.getName()));
    TOPIC_LIST.remove(DELETE_TOPIC);
  }

  @Test(priority = 7)
  public void redirectToConsumerFromTopicCheck() {
    String topicName = "source-activities";
    String consumerGroupId = "connect-sink_postgres_activities";
    navigateToTopicsAndOpenDetails(topicName);
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.CONSUMERS)
        .openConsumerGroup(consumerGroupId);
    consumersDetails
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(consumersDetails.isRedirectedConsumerTitleVisible(consumerGroupId),
        "isRedirectedConsumerTitleVisible()");
    softly.assertTrue(consumersDetails.isTopicInConsumersDetailsVisible(topicName),
        "isTopicInConsumersDetailsVisible()");
    softly.assertAll();
  }

  @Test(priority = 8)
  public void createTopicPossibilityCheck() {
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady();
    Assert.assertFalse(topicCreateEditForm.isCreateTopicButtonEnabled(), "isCreateTopicButtonEnabled()");
    topicCreateEditForm
        .setTopicName("testName");
    Assert.assertFalse(topicCreateEditForm.isCreateTopicButtonEnabled(), "isCreateTopicButtonEnabled()");
    topicCreateEditForm
        .setTopicName(null)
        .setNumberOfPartitions(nextInt(1, 10));
    Assert.assertFalse(topicCreateEditForm.isCreateTopicButtonEnabled(), "isCreateTopicButtonEnabled()");
    topicCreateEditForm
        .setTopicName("testName");
    Assert.assertTrue(topicCreateEditForm.isCreateTopicButtonEnabled(), "isCreateTopicButtonEnabled()");
  }

  @Test(priority = 9)
  public void timeToRetainDataCustomValueWithEditingTopicCheck() {
    Topic retainDataTopic = Topic.createTopic()
        .setTimeToRetainData("86400000");
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(retainDataTopic.getName())
        .setNumberOfPartitions(1)
        .setTimeToRetainDataInMs("604800000");
    Assert.assertEquals(topicCreateEditForm.getTimeToRetain(), "604800000", "getTimeToRetain()");
    topicCreateEditForm
        .setTimeToRetainDataInMs(retainDataTopic.getTimeToRetainData())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady()
        .openDotMenu()
        .clickEditSettingsMenu();
    Assert.assertEquals(topicCreateEditForm.getTimeToRetain(), retainDataTopic.getTimeToRetainData(),
        "getTimeToRetain()");
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.SETTINGS);
    Assert.assertEquals(topicDetails.getSettingsGridValueByKey("retention.ms"), retainDataTopic.getTimeToRetainData(),
        "getTimeToRetainData()");
    TOPIC_LIST.add(retainDataTopic);
  }

  @Test(priority = 10)
  public void customParametersWithinCreateNewTopicCheck() {
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(CREATE_TOPIC.getName())
        .clickAddCustomParameterTypeButton()
        .setCustomParameterType(CREATE_TOPIC.getCustomParameterType());
    Assert.assertTrue(topicCreateEditForm.isDeleteCustomParameterButtonEnabled(),
        "isDeleteCustomParameterButtonEnabled()");
    topicCreateEditForm
        .clearCustomParameterValue();
    Assert.assertTrue(topicCreateEditForm.isValidationMessageCustomParameterValueVisible(),
        "isValidationMessageCustomParameterValueVisible()");
  }

  @Test(priority = 11)
  public void topicListElementsCheck() {
    navigateToTopics();
    verifyElementsCondition(topicsList.getAllVisibleElements(), Condition.visible);
    verifyElementsCondition(topicsList.getAllEnabledElements(), Condition.enabled);
  }

  @Test(priority = 12)
  public void addNewFilterWithinTopicCheck() {
    String filterName = randomAlphabetic(5);
    navigateToTopicsAndOpenDetails(FILTERS_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .clickMessagesAddFiltersBtn()
        .waitUntilAddFiltersMdlVisible();
    verifyElementsCondition(topicDetails.getAllAddFilterModalVisibleElements(), Condition.visible);
    verifyElementsCondition(topicDetails.getAllAddFilterModalEnabledElements(), Condition.enabled);
    verifyElementsCondition(topicDetails.getAllAddFilterModalDisabledElements(), Condition.disabled);
    Assert.assertFalse(topicDetails.isSaveThisFilterCheckBoxSelected(), "isSaveThisFilterCheckBoxSelected()");
    topicDetails
        .setFilterCodeFldAddFilterMdl(FILTER_CODE_STRING)
        .setDisplayNameFldAddFilterMdl(filterName);
    Assert.assertTrue(topicDetails.isAddFilterBtnAddFilterMdlEnabled(), "isAddFilterBtnAddFilterMdlEnabled()");
    topicDetails.clickAddFilterBtnAndCloseMdl(true);
    Assert.assertTrue(topicDetails.isActiveFilterVisible(filterName),
        String.format("isActiveFilterVisible()[%s]", filterName));
  }

  @Test(priority = 13)
  public void editActiveSmartFilterCheck() {
    String filterCode = FILTER_CODE_STRING;
    String filterName = randomAlphabetic(5);
    navigateToTopicsAndOpenDetails(FILTERS_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .clickMessagesAddFiltersBtn()
        .waitUntilAddFiltersMdlVisible()
        .setFilterCodeFldAddFilterMdl(filterCode)
        .setDisplayNameFldAddFilterMdl(filterName)
        .clickAddFilterBtnAndCloseMdl(true)
        .clickEditActiveFilterBtn(filterName)
        .waitUntilAddFiltersMdlVisible();
    SoftAssert softly = new SoftAssert();
    softly.assertEquals(topicDetails.getFilterCodeValue(), filterCode, "getFilterCodeValue()");
    softly.assertEquals(topicDetails.getFilterNameValue(), filterName, "getFilterNameValue()");
    softly.assertAll();
    String newFilterName = randomAlphabetic(5);
    topicDetails
        .setFilterCodeFldAddFilterMdl(FILTER_CODE_JSON)
        .setDisplayNameFldAddFilterMdl(newFilterName)
        .clickSaveFilterBtnAndCloseMdl(true);
    Assert.assertTrue(topicDetails.isActiveFilterVisible(newFilterName),
        String.format("isActiveFilterVisible()[%s]", newFilterName));
  }

  @Test(priority = 14)
  public void saveSmartFilterCheck() {
    String filterName = randomAlphabetic(5);
    navigateToTopicsAndOpenDetails(FILTERS_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .clickMessagesAddFiltersBtn()
        .waitUntilAddFiltersMdlVisible()
        .setFilterCodeFldAddFilterMdl(FILTER_CODE_JSON)
        .selectSaveThisFilterCheckboxMdl(true)
        .setDisplayNameFldAddFilterMdl(filterName);
    Assert.assertTrue(topicDetails.isAddFilterBtnAddFilterMdlEnabled(),
        "isAddFilterBtnAddFilterMdlEnabled()");
    topicDetails
        .clickAddFilterBtnAndCloseMdl(false);
    Assert.assertTrue(topicDetails.isFilterVisibleAtSavedFiltersMdl(filterName),
        String.format("isFilterVisibleAtSavedFiltersMdl()[%s]", filterName));
  }

  @Test(priority = 15)
  public void applySavedFilterWithinTopicMessagesCheck() {
    String filterName = randomAlphabetic(5);
    navigateToTopicsAndOpenDetails(FILTERS_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .clickMessagesAddFiltersBtn()
        .waitUntilAddFiltersMdlVisible()
        .setFilterCodeFldAddFilterMdl(FILTER_CODE_JSON)
        .selectSaveThisFilterCheckboxMdl(true)
        .setDisplayNameFldAddFilterMdl(filterName)
        .clickAddFilterBtnAndCloseMdl(false)
        .selectFilterAtSavedFiltersMdl(filterName);
    Assert.assertTrue(topicDetails.isActiveFilterVisible(filterName),
        String.format("isActiveFilterVisible()[%s]", filterName));
  }

  @Test(priority = 16)
  public void showInternalTopicsButtonCheck() {
    navigateToTopics();
    topicsList
        .setShowInternalRadioButton(true);
    Assert.assertFalse(topicsList.getInternalTopics().isEmpty(), "getInternalTopics().isEmpty()");
    topicsList
        .goToLastPage();
    Assert.assertFalse(topicsList.getNonInternalTopics().isEmpty(), "getNonInternalTopics().isEmpty()");
    topicsList
        .setShowInternalRadioButton(false);
    SoftAssert softly = new SoftAssert();
    softly.assertEquals(topicsList.getInternalTopics().size(), 0, "getInternalTopics().size()");
    softly.assertTrue(!topicsList.getNonInternalTopics().isEmpty(), "getNonInternalTopics().isEmpty()");
    softly.assertAll();
  }

  @Test(priority = 17)
  public void internalTopicsNamingCheck() {
    navigateToTopics();
    SoftAssert softly = new SoftAssert();
    topicsList
        .setShowInternalRadioButton(true)
        .getInternalTopics()
        .forEach(topic -> softly.assertTrue(topic.getName().startsWith("_"),
            String.format("'%s' starts with '_'", topic.getName())));
    softly.assertAll();
  }

  @Test(priority = 18)
  public void retentionBytesAccordingToMaxSizeOnDiskCheck() {
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(SETTINGS_TOPIC.getName())
        .setNumberOfPartitions(SETTINGS_TOPIC.getNumberOfPartitions())
        .setMaxMessageBytes(SETTINGS_TOPIC.getMaxMessageBytes())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady();
    TOPIC_LIST.add(SETTINGS_TOPIC);
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.SETTINGS);
    topicSettingsTab
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertEquals(topicSettingsTab.getValueByKey("retention.bytes"),
        SETTINGS_TOPIC.getMaxSizeOnDisk().getOptionValue(), "getValueOfKey(retention.bytes)");
    softly.assertEquals(topicSettingsTab.getValueByKey("max.message.bytes"),
        SETTINGS_TOPIC.getMaxMessageBytes(), "getValueOfKey(max.message.bytes)");
    softly.assertAll();
    SETTINGS_TOPIC
        .setMaxSizeOnDisk(MaxSizeOnDisk.SIZE_1_GB)
        .setMaxMessageBytes("1000056");
    topicDetails
        .openDotMenu()
        .clickEditSettingsMenu();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setMaxSizeOnDiskInGB(SETTINGS_TOPIC.getMaxSizeOnDisk())
        .setMaxMessageBytes(SETTINGS_TOPIC.getMaxMessageBytes())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady()
        .openDetailsTab(TopicDetails.TopicMenu.SETTINGS);
    topicSettingsTab
        .waitUntilScreenReady();
    softly.assertEquals(topicSettingsTab.getValueByKey("retention.bytes"),
        SETTINGS_TOPIC.getMaxSizeOnDisk().getOptionValue(), "getMaxSizeOnDisk()");
    softly.assertEquals(topicSettingsTab.getValueByKey("max.message.bytes"),
        SETTINGS_TOPIC.getMaxMessageBytes(), "getMaxMessageBytes()");
    softly.assertAll();
  }

  @Test(priority = 19)
  public void recreateTopicFromTopicProfileCheck() {
    Topic recreateTopic = Topic.createTopic();
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(recreateTopic.getName())
        .setNumberOfPartitions(recreateTopic.getNumberOfPartitions())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady();
    TOPIC_LIST.add(recreateTopic);
    topicDetails
        .openDotMenu()
        .clickRecreateTopicMenu();
    Assert.assertTrue(topicDetails.isConfirmationMdlVisible(), "isConfirmationMdlVisible()");
    topicDetails
        .clickConfirmBtnMdl();
    Assert.assertTrue(topicDetails.isAlertWithMessageVisible(SUCCESS,
            String.format("Topic %s successfully recreated!", recreateTopic.getName())),
        "isAlertWithMessageVisible()");
  }

  @Test(priority = 20)
  public void copyTopicPossibilityCheck() {
    Topic copyTopic = Topic.createTopic();
    navigateToTopics();
    topicsList
        .getAnyNonInternalTopic()
        .selectItem(true)
        .clickCopySelectedTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady();
    Assert.assertFalse(topicCreateEditForm.isCreateTopicButtonEnabled(), "isCreateTopicButtonEnabled()");
    topicCreateEditForm
        .setTopicName(copyTopic.getName())
        .setNumberOfPartitions(copyTopic.getNumberOfPartitions())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady();
    TOPIC_LIST.add(copyTopic);
    Assert.assertTrue(topicDetails.isTopicHeaderVisible(copyTopic.getName()),
        String.format("isTopicHeaderVisible()[%s]", copyTopic.getName()));
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    TOPIC_LIST.forEach(topic -> apiService.deleteTopic(topic.getName()));
  }
}
