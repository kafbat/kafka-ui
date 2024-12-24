package io.kafbat.ui.screens.topics;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TopicSettingsTab extends BasePage {

  protected SelenideElement defaultValueColumnHeaderLocator = $x("//div[text() = 'Default Value']");
  protected SelenideElement nextButton = $x("//button[contains(text(), 'Next')]");
  protected SelenideElement previousButton = $x("//button[contains(text(), 'Previous')]");

  @Step
  public TopicSettingsTab waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    defaultValueColumnHeaderLocator.shouldBe(Condition.visible);
    return this;
  }

  private List<SettingsGridItem> initGridItems() {
    List<SettingsGridItem> gridItemList = new ArrayList<>();
    gridItems.shouldHave(CollectionCondition.sizeGreaterThan(0))
        .forEach(item -> gridItemList.add(new SettingsGridItem(item)));
    return gridItemList;
  }

  private TopicSettingsTab.SettingsGridItem getItemByKey(String key) {
    return initGridItems().stream()
        .filter(e -> e.getKey().equals(key))
        .findFirst().orElseThrow();
  }

  @Step
  public String getValueByKey(String key) {
    while (true) {
      try {
        String value = getItemByKey(key).getValue();
        resetPageNavigation();
        return value;
      } catch (NoSuchElementException e) {
        if (nextButton.isEnabled()) {
          nextButton.click();
        } else {
          throw e;
        }
      }
    }
  }

  private void resetPageNavigation() {
    while (previousButton.isEnabled()) {
      previousButton.click();
    }
  }

  public static class SettingsGridItem extends BasePage {

    private final SelenideElement element;

    public SettingsGridItem(SelenideElement element) {
      this.element = element;
    }

    @Step
    public String getKey() {
      return element.$x("./td[1]/span").getText().trim();
    }

    @Step
    public String getValue() {
      return element.$x("./td[2]/span").getText().trim();
    }

    @Step
    public String getDefaultValue() {
      return element.$x("./td[3]/span").getText().trim();
    }
  }
}
