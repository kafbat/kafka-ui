package io.kafbat.ui.smokesuite.topics;

import static io.kafbat.ui.models.Topic.createTopic;

import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Topic;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.screens.topics.TopicDetails;
import io.kafbat.ui.screens.topics.enums.PollingMode;
import io.kafbat.ui.utilities.TimeUtil;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class MessagesTest extends BaseTest {

  private static final Topic PRODUCE_MESSAGE_TOPIC = createTopic();
  private static final Topic CLEAR_MESSAGE_TOPIC = createTopic();
  private static final Topic CHECK_FILTERS_TOPIC = createTopic();
  private static final Topic RECREATE_TOPIC = createTopic();
  private static final Topic MESSAGES_COUNT_TOPIC = createTopic();
  private static final List<Topic> TOPIC_LIST = new ArrayList<>();

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    TOPIC_LIST.addAll(List.of(PRODUCE_MESSAGE_TOPIC, CHECK_FILTERS_TOPIC, CLEAR_MESSAGE_TOPIC,
        RECREATE_TOPIC, MESSAGES_COUNT_TOPIC));
    TOPIC_LIST.forEach(topic -> apiService.createTopic(topic));
    IntStream.range(1, 3).forEach(i -> apiService.sendMessage(CHECK_FILTERS_TOPIC));
    TimeUtil.waitUntilNewMinuteStarted();
    IntStream.range(1, 3).forEach(i -> apiService.sendMessage(CHECK_FILTERS_TOPIC));
    IntStream.range(1, 110).forEach(i -> apiService.sendMessage(MESSAGES_COUNT_TOPIC));
  }

  @Test(priority = 1)
  public void produceMessageCheck() {
    navigateToTopicsAndOpenDetails(PRODUCE_MESSAGE_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES);
    produceMessage(PRODUCE_MESSAGE_TOPIC);
    Assert.assertEquals(topicDetails.getMessageByKey(PRODUCE_MESSAGE_TOPIC.getMessageKey()).getValue(),
        PRODUCE_MESSAGE_TOPIC.getMessageValue(), "message.getValue()");
  }

  @Test(priority = 2)
  public void clearMessageCheck() {
    navigateToTopicsAndOpenDetails(PRODUCE_MESSAGE_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.OVERVIEW);
    int messageAmount = topicDetails.getMessageCountAmount();
    produceMessage(PRODUCE_MESSAGE_TOPIC);
    Assert.assertEquals(topicDetails.getMessageCountAmount(), messageAmount + 1, "getMessageCountAmount()");
    topicDetails
        .openDotMenu()
        .clickClearMessagesMenu()
        .clickConfirmBtnMdl()
        .waitUntilScreenReady();
    Assert.assertEquals(topicDetails.getMessageCountAmount(), 0, "getMessageCountAmount()");
  }

  @Test(priority = 3)
  public void clearTopicMessageCheck() {
    navigateToTopicsAndOpenDetails(CLEAR_MESSAGE_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.OVERVIEW);
    produceMessage(CLEAR_MESSAGE_TOPIC);
    navigateToTopics();
    Assert.assertEquals(topicsList.getTopicItem(CLEAR_MESSAGE_TOPIC.getName()).getNumberOfMessages(), 1,
        "getNumberOfMessages()");
    topicsList
        .openDotMenuByTopicName(CLEAR_MESSAGE_TOPIC.getName())
        .clickClearMessagesBtn()
        .clickConfirmBtnMdl();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(topicsList.isAlertWithMessageVisible(BasePage.AlertHeader.SUCCESS,
            String.format("%s messages have been successfully cleared!", CLEAR_MESSAGE_TOPIC.getName())),
        "isAlertWithMessageVisible()");
    softly.assertEquals(topicsList.getTopicItem(CLEAR_MESSAGE_TOPIC.getName()).getNumberOfMessages(), 0,
        "getNumberOfMessages()");
    softly.assertAll();
  }

  @Test(priority = 4)
  public void purgeMessagePossibilityCheck() {
    navigateToTopics();
    int messageAmount = topicsList.getTopicItem(CLEAR_MESSAGE_TOPIC.getName()).getNumberOfMessages();
    topicsList
        .openTopic(CLEAR_MESSAGE_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.OVERVIEW);
    produceMessage(CLEAR_MESSAGE_TOPIC);
    navigateToTopics();
    Assert.assertEquals(topicsList.getTopicItem(CLEAR_MESSAGE_TOPIC.getName()).getNumberOfMessages(),
        messageAmount + 1, "getNumberOfMessages()");
    topicsList
        .getTopicItem(CLEAR_MESSAGE_TOPIC.getName())
        .selectItem(true)
        .clickPurgeMessagesOfSelectedTopicsBtn();
    Assert.assertTrue(topicsList.isConfirmationMdlVisible(), "isConfirmationMdlVisible()");
    topicsList
        .clickCancelBtnMdl()
        .clickPurgeMessagesOfSelectedTopicsBtn()
        .clickConfirmBtnMdl();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(topicsList.isAlertWithMessageVisible(BasePage.AlertHeader.SUCCESS,
            String.format("%s messages have been successfully cleared!", CLEAR_MESSAGE_TOPIC.getName())),
        "isAlertWithMessageVisible()");
    softly.assertEquals(topicsList.getTopicItem(CLEAR_MESSAGE_TOPIC.getName()).getNumberOfMessages(), 0,
        "getNumberOfMessages()");
    softly.assertAll();
  }

  @Test(priority = 6)
  public void messageFilteringByOffsetCheck() {
    navigateToTopicsAndOpenDetails(CHECK_FILTERS_TOPIC.getName());
    int nextOffset = topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .selectModeDdlMessagesTab(PollingMode.FROM_OFFSET)
        .getAllMessages().stream()
        .findFirst().orElseThrow().getOffset() + 1;
    topicDetails
        .setModeValueFldMessagesTab(String.valueOf(nextOffset))
        .clickSubmitFiltersBtnMessagesTab();
    SoftAssert softly = new SoftAssert();
    topicDetails.getAllMessages().forEach(message ->
        softly.assertTrue(message.getOffset() >= nextOffset,
            String.format("Expected offset not less: %s, but found: %s", nextOffset, message.getOffset())));
    softly.assertAll();
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/281")
  @Issue("https://github.com/kafbat/kafka-ui/issues/282")
  @Test(priority = 7)
  public void messageFilteringByTimestampCheck() {
    navigateToTopicsAndOpenDetails(CHECK_FILTERS_TOPIC.getName());
    LocalDateTime firstTimestamp = topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES)
        .getMessageByOffset(0).getTimestamp();
    LocalDateTime nextTimestamp = topicDetails.getAllMessages().stream()
        .filter(message -> message.getTimestamp().getMinute() != firstTimestamp.getMinute())
        .findFirst().orElseThrow().getTimestamp();
    topicDetails
        .selectModeDdlMessagesTab(PollingMode.SINCE_TIME)
        .openCalendarMode()
        .selectDateAndTimeByCalendar(nextTimestamp)
        .clickSubmitFiltersBtnMessagesTab();
    SoftAssert softly = new SoftAssert();
    topicDetails.getAllMessages().forEach(message ->
        softly.assertFalse(message.getTimestamp().isBefore(nextTimestamp),
            String.format("Expected that %s is not before %s.", message.getTimestamp(), nextTimestamp)));
    softly.assertAll();
  }

  @Test(priority = 8)
  public void clearTopicMessageFromOverviewTabCheck() {
    navigateToTopicsAndOpenDetails(CHECK_FILTERS_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.OVERVIEW)
        .openDotMenu()
        .clickClearMessagesMenu()
        .clickConfirmBtnMdl();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(topicDetails.isAlertWithMessageVisible(BasePage.AlertHeader.SUCCESS,
            String.format("%s messages have been successfully cleared!", CHECK_FILTERS_TOPIC.getName())),
        "isAlertWithMessageVisible()");
    softly.assertEquals(topicDetails.getMessageCountAmount(), 0, "getMessageCountAmount()");
    softly.assertAll();
  }

  @Test(priority = 9)
  public void recreateTopicCheck() {
    navigateToTopicsAndOpenDetails(RECREATE_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.OVERVIEW);
    produceMessage(RECREATE_TOPIC);
    navigateToTopics();
    Assert.assertEquals(topicsList.getTopicItem(RECREATE_TOPIC.getName()).getNumberOfMessages(), 1,
        "getNumberOfMessages()");
    topicsList
        .openDotMenuByTopicName(RECREATE_TOPIC.getName())
        .clickRecreateTopicBtn()
        .clickConfirmBtnMdl();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(topicDetails.isAlertWithMessageVisible(BasePage.AlertHeader.SUCCESS,
            String.format("Topic %s successfully recreated!", RECREATE_TOPIC.getName())),
        "isAlertWithMessageVisible()");
    softly.assertEquals(topicsList.getTopicItem(RECREATE_TOPIC.getName()).getNumberOfMessages(), 0,
        "getNumberOfMessages()");
    softly.assertAll();
  }

  @Ignore
  @Issue("https://github.com/kafbat/kafka-ui/issues/270")
  @Test(priority = 10)
  public void messagesCountPerPageCheck() {
    navigateToTopicsAndOpenDetails(MESSAGES_COUNT_TOPIC.getName());
    topicDetails
        .openDetailsTab(TopicDetails.TopicMenu.MESSAGES);
    int messagesPerPage = topicDetails.getAllMessages().size();
    SoftAssert softly = new SoftAssert();
    softly.assertEquals(messagesPerPage, 100, "getAllMessages()");
    softly.assertFalse(topicDetails.isBackButtonEnabled(), "isBackButtonEnabled()");
    softly.assertTrue(topicDetails.isNextButtonEnabled(), "isNextButtonEnabled()");
    softly.assertAll();
    int lastOffsetOnPage = topicDetails.getAllMessages()
        .get(messagesPerPage - 1).getOffset();
    topicDetails
        .clickNextButton();
    softly.assertEquals(topicDetails.getAllMessages().stream().findFirst().orElseThrow().getOffset(),
        lastOffsetOnPage - 1, "getAllMessages().findFirst().getOffset()");
    softly.assertTrue(topicDetails.isBackButtonEnabled(), "isBackButtonEnabled()");
    softly.assertFalse(topicDetails.isNextButtonEnabled(), "isNextButtonEnabled()");
    softly.assertAll();
  }

  @Step
  private void produceMessage(Topic topic) {
    topicDetails
        .clickProduceMessageBtn();
    produceMessagePanel
        .waitUntilScreenReady()
        .setKeyField(topic.getMessageKey())
        .setValueFiled(topic.getMessageValue())
        .submitProduceMessage();
    topicDetails
        .waitUntilScreenReady();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    TOPIC_LIST.forEach(topic -> apiService.deleteTopic(topic.getName()));
  }
}
