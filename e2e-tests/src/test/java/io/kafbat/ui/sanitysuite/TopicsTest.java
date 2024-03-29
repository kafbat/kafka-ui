package io.kafbat.ui.sanitysuite;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.kafbat.ui.BaseTest;
import io.kafbat.ui.models.Topic;
import io.kafbat.ui.pages.topics.enums.CleanupPolicyValue;
import io.qase.api.annotation.QaseId;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TopicsTest extends BaseTest {

  private static final List<Topic> TOPIC_LIST = new ArrayList<>();

  @QaseId(285)
  @Test()
  public void verifyClearMessagesMenuStateAfterTopicUpdate() {
    Topic topic = new Topic()
        .setName("topic-" + randomAlphabetic(5))
        .setNumberOfPartitions(1)
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
