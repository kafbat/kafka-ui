package io.kafbat.ui.screens.brokers;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

public class BrokersDetails extends BasePage {

  protected String brokersTabLocator = "//a[text()='%s']";

  @Step
  public BrokersDetails waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    $x(String.format(brokersTabLocator, DetailsTab.LOG_DIRECTORIES.getValue())).shouldBe(Condition.visible);
    return this;
  }

  @Step
  public BrokersDetails openDetailsTab(DetailsTab menu) {
    $x(String.format(brokersTabLocator, menu.getValue())).shouldBe(Condition.enabled).click();
    waitUntilSpinnerDisappear();
    return this;
  }

  private List<SelenideElement> getVisibleColumnHeaders() {
    return Stream.of("Name", "Topics", "Error", "Partitions")
        .map(name -> $x(String.format(columnHeaderLocator, name)))
        .collect(Collectors.toList());
  }

  private List<SelenideElement> getEnabledColumnHeaders() {
    return Stream.of("Name", "Error")
        .map(name -> $x(String.format(columnHeaderLocator, name)))
        .collect(Collectors.toList());
  }

  private List<SelenideElement> getVisibleSummaryCells() {
    return Stream.of("Segment Size", "Segment Count", "Port", "Host")
        .map(name -> $x(String.format(summaryCellLocator, name)))
        .collect(Collectors.toList());
  }

  private List<SelenideElement> getDetailsTabs() {
    return Stream.of(DetailsTab.values())
        .map(name -> $x(String.format(brokersTabLocator, name)))
        .collect(Collectors.toList());
  }

  @Step
  public List<SelenideElement> getAllEnabledElements() {
    List<SelenideElement> enabledElements = new ArrayList<>(getEnabledColumnHeaders());
    enabledElements.addAll(getDetailsTabs());
    return enabledElements;
  }

  @Step
  public List<SelenideElement> getAllVisibleElements() {
    List<SelenideElement> visibleElements = new ArrayList<>(getVisibleSummaryCells());
    visibleElements.addAll(getVisibleColumnHeaders());
    visibleElements.addAll(getDetailsTabs());
    return visibleElements;
  }

  @Getter
  public enum DetailsTab {
    LOG_DIRECTORIES("Log directories"),
    CONFIGS("Configs"),
    METRICS("Metrics");

    private final String value;

    DetailsTab(String value) {
      this.value = value;
    }
  }
}
