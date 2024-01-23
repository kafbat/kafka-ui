package io.kafbat.ui.pages.topics;

import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.refresh;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.pages.BasePage;
import io.kafbat.ui.utilities.WebUtils;
import io.qameta.allure.Step;
import java.util.Arrays;

public class ProduceMessagePanel extends BasePage {

  protected SelenideElement keyTextArea = $x("//div[@id='key']/textarea");
  protected SelenideElement valueTextArea = $x("//div[@id='content']/textarea");
  protected SelenideElement headersTextArea = $x("//div[@id='headers']/textarea");
  protected SelenideElement submitProduceMessageBtn = headersTextArea.$x("../../../..//button[@type='submit']");
  protected SelenideElement partitionDdl = $x("//ul[@name='partition']");
  protected SelenideElement keySerdeDdl = $x("//ul[@name='keySerde']");
  protected SelenideElement contentSerdeDdl = $x("//ul[@name='valueSerde']");

  @Step
  public ProduceMessagePanel waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    Arrays.asList(partitionDdl, keySerdeDdl, contentSerdeDdl).forEach(element -> element.shouldBe(Condition.visible));
    return this;
  }

  @Step
  public ProduceMessagePanel setKeyField(String value) {
    WebUtils.clearByKeyboard(keyTextArea);
    keyTextArea.setValue(value);
    return this;
  }

  @Step
  public ProduceMessagePanel setValueFiled(String value) {
    WebUtils.clearByKeyboard(valueTextArea);
    valueTextArea.setValue(value);
    return this;
  }

  @Step
  public ProduceMessagePanel setHeadersFld(String value) {
    headersTextArea.setValue(value);
    return this;
  }

  @Step
  public ProduceMessagePanel submitProduceMessage() {
    WebUtils.clickByActions(submitProduceMessageBtn);
    submitProduceMessageBtn.shouldBe(Condition.disappear);
    refresh();
    return this;
  }
}
