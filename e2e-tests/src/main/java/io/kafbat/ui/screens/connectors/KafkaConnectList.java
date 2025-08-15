package io.kafbat.ui.screens.connectors;

import static com.codeborne.selenide.Selenide.$x;
import static io.kafbat.ui.screens.panels.enums.MenuItem.KAFKA_CONNECT;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.utilities.WebUtil;
import io.qameta.allure.Step;


public class KafkaConnectList extends BasePage {

  protected SelenideElement createConnectorBtn = $x("//button[contains(text(),'Create Connector')]");
  protected SelenideElement connectorsTab = $x("//a[contains(text(),'Connectors')]");

  public KafkaConnectList() {
    tableElementNameLocator = "//tbody//td[contains(text(),'%s')]";
  }

  @Step
  public KafkaConnectList clickConnectorsTab() {
    WebUtil.clickByJavaScript(connectorsTab);
    return this;
  }

  @Step
  public KafkaConnectList waitUntilScreenReady() {
    clickConnectorsTab();
    waitUntilSpinnerDisappear();
    getPageTitleFromHeader(KAFKA_CONNECT).shouldBe(Condition.visible);
    return this;
  }

  @Step
  public KafkaConnectList clickCreateConnectorBtn() {
    WebUtil.clickByJavaScript(createConnectorBtn);
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
    return WebUtil.isVisible(getTableElement(connectorName));
  }
}
