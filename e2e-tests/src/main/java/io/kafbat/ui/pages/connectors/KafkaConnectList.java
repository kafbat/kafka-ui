package io.kafbat.ui.pages.connectors;

import static com.codeborne.selenide.Selenide.$x;
import static io.kafbat.ui.pages.panels.enums.MenuItem.KAFKA_CONNECT;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.pages.BasePage;
import io.kafbat.ui.utilities.WebUtils;
import io.qameta.allure.Step;


public class KafkaConnectList extends BasePage {

  protected SelenideElement createConnectorBtn = $x("//button[contains(text(),'Create Connector')]");

  public KafkaConnectList() {
    tableElementNameLocator = "//tbody//td[contains(text(),'%s')]";
  }

  @Step
  public KafkaConnectList waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    getPageTitleFromHeader(KAFKA_CONNECT).shouldBe(Condition.visible);
    return this;
  }

  @Step
  public KafkaConnectList clickCreateConnectorBtn() {
    WebUtils.clickByJavaScript(createConnectorBtn);
    return this;
  }

  @Step
  public KafkaConnectList openConnector(String connectorName) {
    getTableElement(connectorName).shouldBe(Condition.enabled).click();
    return this;
  }

  @Step
  public boolean isConnectorVisible(String connectorName) {
    tableGrid.shouldBe(Condition.visible);
    return WebUtils.isVisible(getTableElement(connectorName));
  }
}
