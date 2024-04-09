package io.kafbat.ui.sanitysuite;

import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Topic;
import io.kafbat.ui.screens.topics.enums.CleanupPolicyValue;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TopicsTest extends BaseTest {

  private static final List<Topic> TOPIC_LIST = new ArrayList<>();

  @Test()
  public void verifyClearMessagesMenuStateAfterTopicUpdate() {
    Topic topic = Topic.createTopic()
        .setCleanupPolicyValue(CleanupPolicyValue.DELETE);
    navigateToTopics();
    topicsList
        .clickAddTopicBtn();
    topicCreateEditForm
        .waitUntilScreenReady()
        .setTopicName(topic.getName())
        .setNumberOfPartitions(topic.getNumberOfPartitions())
        .selectCleanupPolicy(topic.getCleanupPolicyValue())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady();
    TOPIC_LIST.add(topic);
    topicDetails
        .openDotMenu();
    Assert.assertTrue(topicDetails.isClearMessagesMenuEnabled(), "isClearMessagesMenuEnabled");
    topic.setCleanupPolicyValue(CleanupPolicyValue.COMPACT);
    editCleanUpPolicyAndOpenDotMenu(topic);
    Assert.assertFalse(topicDetails.isClearMessagesMenuEnabled(), "isClearMessagesMenuEnabled");
    topic.setCleanupPolicyValue(CleanupPolicyValue.DELETE);
    editCleanUpPolicyAndOpenDotMenu(topic);
    Assert.assertTrue(topicDetails.isClearMessagesMenuEnabled(), "isClearMessagesMenuEnabled");
  }

  @Step
  private void editCleanUpPolicyAndOpenDotMenu(Topic topic) {
    topicDetails
        .clickEditSettingsMenu();
    topicCreateEditForm
        .waitUntilScreenReady()
        .selectCleanupPolicy(topic.getCleanupPolicyValue())
        .clickSaveTopicBtn();
    topicDetails
        .waitUntilScreenReady()
        .openDotMenu();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    TOPIC_LIST.forEach(topic -> apiService.deleteTopic(topic.getName()));
  }
}
