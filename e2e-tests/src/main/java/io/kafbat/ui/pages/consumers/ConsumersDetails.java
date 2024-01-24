package io.kafbat.ui.pages.consumers;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.pages.BasePage;
import io.kafbat.ui.utilities.WebUtils;
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
    return WebUtils.isVisible($x(String.format(consumerIdHeaderLocator, consumerGroupId)));
  }

  @Step
  public boolean isTopicInConsumersDetailsVisible(String topicName) {
    tableGrid.shouldBe(Condition.visible);
    return WebUtils.isVisible($x(String.format(topicElementLocator, topicName)));
  }
}
