package io.kafbat.ui.screens.consumers;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.utilities.WebUtil;
import io.qameta.allure.Step;

public class ConsumersDetails extends BasePage {

  protected String consumerIdHeaderLocator = "//h1[contains(text(),'%s')]";
  protected String topicElementLocator = "//tbody//td//a[text()='%s']";

  @Step
  public ConsumersDetails waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    tableGrid.shouldBe(Condition.visible);
    return this;
  }

  @Step
  public boolean isRedirectedConsumerTitleVisible(String consumerGroupId) {
    return WebUtil.isVisible($x(String.format(consumerIdHeaderLocator, consumerGroupId)));
  }

  @Step
  public boolean isTopicInConsumersDetailsVisible(String topicName) {
    tableGrid.shouldBe(Condition.visible);
    return WebUtil.isVisible($x(String.format(topicElementLocator, topicName)));
  }
}
