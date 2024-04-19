package io.kafbat.ui.screens.consumers;

import static io.kafbat.ui.screens.panels.enums.MenuItem.CONSUMERS;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.screens.BasePage;
import io.qameta.allure.Step;

public class ConsumersList extends BasePage {

  @Step
  public ConsumersList waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    getPageTitleFromHeader(CONSUMERS).shouldBe(Condition.visible);
    return this;
  }
}
