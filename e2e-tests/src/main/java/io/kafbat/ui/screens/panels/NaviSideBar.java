package io.kafbat.ui.screens.panels;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.screens.panels.enums.MenuItem;
import io.kafbat.ui.settings.BaseSource;
import io.kafbat.ui.utilities.WebUtil;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openqa.selenium.By;

public class NaviSideBar extends BasePage {

  protected SelenideElement dashboardMenuItem = $x("//a[@title='Dashboard']");
  protected String sideMenuOptionElementLocator = ".//ul/a[@title='%s']";
  protected String clusterElementLocator = "//aside/ul/li[contains(.,'%s')]";

  private SelenideElement expandCluster(String clusterName) {
    SelenideElement clusterElement = $x(String.format(clusterElementLocator, clusterName)).shouldBe(Condition.visible);
    if (clusterElement.parent().$$x(".//ul").isEmpty()) {
      WebUtil.clickByActions(clusterElement);
    }
    return clusterElement;
  }

  @Step
  public NaviSideBar waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    dashboardMenuItem.shouldBe(Condition.visible, Duration.ofSeconds(30));
    return this;
  }

  @Step
  public String getPagePath(MenuItem menuItem) {
    return getPagePathFromHeader(menuItem)
        .shouldBe(Condition.visible)
        .getText().trim();
  }

  @Step
  public NaviSideBar openSideMenu(String clusterName, MenuItem menuItem) {
    WebUtil.clickByActions(expandCluster(clusterName).parent()
        .find(By.linkText(menuItem.getNaviTitle())));
    return this;
  }

  @Step
  public NaviSideBar openSideMenu(MenuItem menuItem) {
    openSideMenu(BaseSource.CLUSTER_NAME, menuItem);
    return this;
  }

  public List<SelenideElement> getAllMenuButtons() {
    expandCluster(BaseSource.CLUSTER_NAME);
    return Stream.of(MenuItem.values())
        .map(menuItem -> $x(String.format(sideMenuOptionElementLocator, menuItem.getNaviTitle())))
        .collect(Collectors.toList());
  }
}
