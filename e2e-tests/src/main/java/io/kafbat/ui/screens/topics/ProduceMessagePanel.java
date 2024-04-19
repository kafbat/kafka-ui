package io.kafbat.ui.screens.topics;

import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.refresh;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.utilities.WebUtil;
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
    WebUtil.clearByKeyboard(keyTextArea);
    keyTextArea.setValue(value);
    return this;
  }

  @Step
  public ProduceMessagePanel setValueFiled(String value) {
    WebUtil.clearByKeyboard(valueTextArea);
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
    WebUtil.clickByActions(submitProduceMessageBtn);
    submitProduceMessageBtn.shouldBe(Condition.disappear);
    refresh();
    return this;
  }
}
