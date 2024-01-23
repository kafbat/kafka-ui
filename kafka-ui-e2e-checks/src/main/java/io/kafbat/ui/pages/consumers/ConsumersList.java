package io.kafbat.ui.pages.consumers;

import static io.kafbat.ui.pages.panels.enums.MenuItem.CONSUMERS;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.pages.BasePage;
import io.qameta.allure.Step;

public class ConsumersList extends BasePage {

  @Step
  public ConsumersList waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    getPageTitleFromHeader(CONSUMERS).shouldBe(Condition.visible);
    return this;
  }
}
