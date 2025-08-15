package io.kafbat.ui.utilities;

import static com.codeborne.selenide.Condition.enabled;
import static io.kafbat.ui.variables.Common.LOG_RESULT;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Keys;

@Slf4j
public class WebUtil {

  private static int getTimeout(int... timeoutInSeconds) {
    return (timeoutInSeconds != null && timeoutInSeconds.length > 0) ? timeoutInSeconds[0] : 4;
  }

  public static void sendKeysAfterClear(SelenideElement element, String keys) {
    log.debug("\nsendKeysAfterClear: {{}} \nsend keys '{}'", element.getSearchCriteria(), keys);
    element.shouldBe(Condition.enabled).clear();
    if (keys != null) {
      element.sendKeys(keys);
    }
  }

  public static void clickByActions(SelenideElement element) {
    log.debug("\nclickByActions: {{}}", element.getSearchCriteria());
    element.shouldBe(Condition.enabled);
    Selenide.actions()
        .moveToElement(element)
        .click(element)
        .perform();
  }

  public static void sendKeysByActions(SelenideElement element, String keys) {
    log.debug("\nsendKeysByActions: {{}} \nsend keys '{}'", element.getSearchCriteria(), keys);
    element.shouldBe(Condition.enabled);
    Selenide.actions()
        .moveToElement(element)
        .sendKeys(element, keys)
        .perform();
  }

  public static void clickByJavaScript(SelenideElement element) {
    log.debug("\nclickByJavaScript: {{}}", element.getSearchCriteria());
    element.shouldBe(Condition.enabled);
    String script = "arguments[0].click();";
    Selenide.executeJavaScript(script, element);
  }

  public static void clearByKeyboard(SelenideElement field) {
    log.debug("\nclearByKeyboard: {{}}", field.getSearchCriteria());
    field.shouldBe(enabled).sendKeys(Keys.PAGE_DOWN);
    Selenide.actions()
        .keyDown(Keys.SHIFT)
        .sendKeys(Keys.PAGE_UP)
        .keyUp(Keys.SHIFT)
        .sendKeys(Keys.DELETE)
        .perform();
  }

  public static boolean isVisible(SelenideElement element, int... timeoutInSeconds) {
    log.debug("\nisVisible: {{}}", element.getSearchCriteria());
    boolean isVisible = false;
    try {
      element.shouldBe(Condition.visible,
          Duration.ofSeconds(getTimeout(timeoutInSeconds)));
      isVisible = true;
    } catch (Throwable ignored) {
    }
    log.debug(LOG_RESULT, isVisible);
    return isVisible;
  }

  public static boolean isEnabled(SelenideElement element, int... timeoutInSeconds) {
    log.debug("\nisEnabled: {{}}", element.getSearchCriteria());
    boolean isEnabled = false;
    try {
      element.shouldBe(Condition.enabled,
          Duration.ofSeconds(getTimeout(timeoutInSeconds)));
      isEnabled = true;
    } catch (Throwable ignored) {
    }
    log.debug(LOG_RESULT, isEnabled);
    return isEnabled;
  }

  public static boolean isSelected(SelenideElement element, int... timeoutInSeconds) {
    log.debug("\nisSelected: {{}}", element.getSearchCriteria());
    boolean isSelected = false;
    try {
      element.shouldBe(Condition.selected,
          Duration.ofSeconds(getTimeout(timeoutInSeconds)));
      isSelected = true;
    } catch (Throwable ignored) {
    }
    log.debug(LOG_RESULT, isSelected);
    return isSelected;
  }

  public static void selectElement(SelenideElement element, boolean select) {
    log.debug("\nselectElement: {{}} \nselect '{}'", element.getSearchCriteria(), select);
    if (select) {
      if (!element.isSelected()) {
        clickByJavaScript(element);
      }
    } else {
      if (element.isSelected()) {
        clickByJavaScript(element);
      }
    }
  }
}
