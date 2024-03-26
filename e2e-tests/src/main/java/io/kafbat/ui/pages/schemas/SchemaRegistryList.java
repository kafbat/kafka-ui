package io.kafbat.ui.pages.schemas;

import static com.codeborne.selenide.Selenide.$x;
import static io.kafbat.ui.pages.panels.enums.MenuItem.SCHEMA_REGISTRY;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.pages.BasePage;
import io.kafbat.ui.utilities.WebUtil;
import io.qameta.allure.Step;

public class SchemaRegistryList extends BasePage {

  protected SelenideElement createSchemaBtn = $x("//button[contains(text(),'Create Schema')]");

  @Step
  public SchemaRegistryList waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    getPageTitleFromHeader(SCHEMA_REGISTRY).shouldBe(Condition.visible);
    return this;
  }

  @Step
  public SchemaRegistryList clickCreateSchema() {
    WebUtil.clickByJavaScript(createSchemaBtn);
    return this;
  }

  @Step
  public SchemaRegistryList openSchema(String schemaName) {
    getTableElement(schemaName)
        .shouldBe(Condition.enabled).click();
    return this;
  }

  @Step
  public boolean isSchemaVisible(String schemaName) {
    tableGrid.shouldBe(Condition.visible);
    return WebUtil.isVisible(getTableElement(schemaName));
  }
}


