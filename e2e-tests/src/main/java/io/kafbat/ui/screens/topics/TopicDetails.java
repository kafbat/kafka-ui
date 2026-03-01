package io.kafbat.ui.screens.topics;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.sleep;
import static io.kafbat.ui.screens.topics.TopicDetails.TopicMenu.OVERVIEW;
import static org.apache.commons.lang3.RandomUtils.nextInt;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.screens.BasePage;
import io.kafbat.ui.screens.topics.enums.PollingMode;
import io.kafbat.ui.utilities.WebUtil;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.Getter;

public class TopicDetails extends BasePage {

  protected SelenideElement clearMessagesBtn = $x(("//div[contains(text(), 'Clear messages')]"));
  protected SelenideElement recreateTopicBtn = $x("//div[text()='Recreate Topic']");
  protected SelenideElement messageAmountCell = $x("//tbody/tr/td[5]");
  protected SelenideElement overviewTab = $x("//a[contains(text(),'Overview')]");
  protected SelenideElement messagesTab = $x("//a[contains(text(),'Messages')]");
  protected SelenideElement modeDdl = $x("//ul[@id='selectMode']//li");
  protected SelenideElement modeField = $x("//ul[@id='selectMode']/../..//input");
  protected SelenideElement addFiltersBtn = $x("//button[text()='Add Filters']");
  protected SelenideElement savedFiltersLink = $x("//div[text()='Saved Filters']");
  protected SelenideElement addFilterCodeModalTitle = $x("//label[text()='Filter code']");
  protected SelenideElement addFilterCodeEditor = $x("//div[@id='ace-editor']");
  protected SelenideElement addFilterCodeTextarea = $x("//div[@id='ace-editor']//textarea");
  protected SelenideElement saveThisFilterCheckBoxAddFilterMdl = $x("//input[@name='saveFilter']");
  protected SelenideElement displayNameInputAddFilterMdl = $x("//input[@placeholder='Enter Name']");
  protected SelenideElement cancelBtnAddFilterMdl = $x("//button[text()='Cancel']");
  protected SelenideElement addFilterBtnAddFilterMdl = $x("//button[text()='Add Filter']");
  protected SelenideElement saveFilterBtnEditFilterMdl = $x("//button[text()='Edit Filter']");
  protected SelenideElement addFiltersBtnMessages = $x("//button[text()='Add Filters']");
  protected SelenideElement editSettingsMenu = $x("//li[@role][contains(text(),'Edit settings')]");
  protected SelenideElement removeTopicBtn = $x("//ul[@role='menu']//div[contains(text(),'Remove Topic')]");
  protected SelenideElement produceMessageBtn = $x("//div//button[text()='Produce Message']");
  protected SelenideElement contentMessageTab = $x("//html//div[@id='root']/div/main//table//p");
  protected SelenideElement cleanUpPolicyField = $x("//div[contains(text(),'Clean Up Policy')]/../span/*");
  protected SelenideElement partitionsField = $x("//div[contains(text(),'Partitions')]/../span");
  protected ElementsCollection messageGridItems = $$x("//tbody//tr");
  protected SelenideElement actualCalendarDate = $x("//div[@class='react-datepicker__current-month']");
  protected SelenideElement previousMonthButton = $x("//button[@aria-label='Previous Month']");
  protected SelenideElement nextMonthButton = $x("//button[@aria-label='Next Month']");
  protected SelenideElement calendarTimeFld = $x("//input[@placeholder='Time']");
  protected String detailsTabLtr = "//nav//a[contains(text(),'%s')]";
  protected String dayCellLtr = "//div[@role='option'][contains(text(),'%d')]";
  protected String modeFilterDdlLocator = "//ul[@id='selectMode']/ul/li[text()='%s']";
  protected String savedFilterNameLocator = "//div[@role='savedFilter']/div[contains(text(),'%s')]";
  protected String consumerIdLocator = "//a[@title='%s']";
  protected String topicHeaderLocator = "//h1[contains(text(),'%s')]";
  protected String activeFilterNameLocator = "//div[@data-testid='activeSmartFilter']/div[1][contains(text(),'%s')]";
  protected String editActiveFilterBtnLocator = "//div[text()='%s']/../button[1]";
  protected String settingsGridValueLocator = "//tbody/tr/td/span[text()='%s']//ancestor::tr/td[2]/span";

  @Step
  public TopicDetails waitUntilScreenReady() {
    waitUntilSpinnerDisappear();
    $x(String.format(detailsTabLtr, OVERVIEW.getValue())).shouldBe(Condition.visible);
    return this;
  }

  @Step
  public TopicDetails openDetailsTab(TopicMenu menu) {
    $x(String.format(detailsTabLtr, menu.getValue())).shouldBe(enabled).click();
    waitUntilSpinnerDisappear();
    return this;
  }

  @Step
  public String getSettingsGridValueByKey(String key) {
    return $x(String.format(settingsGridValueLocator, key)).scrollTo().shouldBe(Condition.visible).getText();
  }

  @Step
  public TopicDetails openDotMenu() {
    WebUtil.clickByJavaScript(dotMenuBtn);
    return this;
  }

  @Step
  public boolean isAlertWithMessageVisible(AlertHeader header, String message) {
    return isAlertVisible(header, message);
  }

  @Step
  public TopicDetails clickEditSettingsMenu() {
    editSettingsMenu.shouldBe(Condition.visible).click();
    return this;
  }

  @Step
  public boolean isConfirmationMdlVisible() {
    return isConfirmationModalVisible();
  }

  @Step
  public TopicDetails clickClearMessagesMenu() {
    clearMessagesBtn.shouldBe(Condition.visible).click();
    return this;
  }

  @Step
  public boolean isClearMessagesMenuEnabled() {
    return !Objects.requireNonNull(clearMessagesBtn.shouldBe(Condition.visible)
            .$x("./..").getAttribute("class"))
        .contains("disabled");
  }

  @Step
  public TopicDetails clickRecreateTopicMenu() {
    recreateTopicBtn.shouldBe(Condition.visible).click();
    return this;
  }

  @Step
  public String getCleanUpPolicy() {
    return cleanUpPolicyField.getText();
  }

  @Step
  public int getPartitions() {
    return Integer.parseInt(partitionsField.getText().trim());
  }

  @Step
  public boolean isTopicHeaderVisible(String topicName) {
    return WebUtil.isVisible($x(String.format(topicHeaderLocator, topicName)));
  }

  @Step
  public TopicDetails clickDeleteTopicMenu() {
    removeTopicBtn.shouldBe(Condition.visible).click();
    return this;
  }

  @Step
  public TopicDetails clickConfirmBtnMdl() {
    clickConfirmButton();
    return this;
  }

  @Step
  public TopicDetails clickProduceMessageBtn() {
    WebUtil.clickByJavaScript(produceMessageBtn);
    return this;
  }

  @Step
  public TopicDetails selectModeDdlMessagesTab(PollingMode pollingMode) {
    modeDdl.shouldBe(enabled).click();
    $x(String.format(modeFilterDdlLocator, pollingMode.getValue())).shouldBe(Condition.visible).click();
    return this;
  }

  @Step
  public TopicDetails setModeValueFldMessagesTab(String modeValue) {
    modeField.shouldBe(enabled).sendKeys(modeValue);
    return this;
  }

  @Step
  public TopicDetails clickSubmitFiltersBtnMessagesTab() {
    WebUtil.clickByJavaScript(submitBtn);
    waitUntilSpinnerDisappear();
    return this;
  }

  @Step
  public TopicDetails clickMessagesAddFiltersBtn() {
    addFiltersBtn.shouldBe(enabled).click();
    return this;
  }

  @Step
  public TopicDetails clickEditActiveFilterBtn(String filterName) {
    $x(String.format(editActiveFilterBtnLocator, filterName))
        .shouldBe(enabled).click();
    return this;
  }

  @Step
  public TopicDetails clickNextButton() {
    clickNextBtn();
    waitUntilSpinnerDisappear();
    return this;
  }

  @Step
  public boolean isFilterVisibleAtSavedFiltersMdl(String filterName) {
    return WebUtil.isVisible($x(String.format(savedFilterNameLocator, filterName)));
  }

  @Step
  public TopicDetails selectFilterAtSavedFiltersMdl(String filterName) {
    $x(String.format(savedFilterNameLocator, filterName)).shouldBe(enabled).click();
    return this;
  }

  @Step
  public TopicDetails waitUntilAddFiltersMdlVisible() {
    addFilterCodeModalTitle.shouldBe(Condition.visible);
    return this;
  }

  @Step
  public TopicDetails setFilterCodeFldAddFilterMdl(String filterCode) {
    addFilterCodeTextarea.shouldBe(enabled).clear();
    addFilterCodeTextarea.sendKeys(filterCode);
    return this;
  }

  @Step
  public String getFilterCodeValue() {
    addFilterCodeEditor.shouldBe(enabled).click();
    String value = addFilterCodeTextarea.getValue();
    if (value == null) {
      return null;
    } else {
      return value.substring(0, value.length() - 2);
    }
  }

  @Step
  public String getFilterNameValue() {
    return displayNameInputAddFilterMdl.shouldBe(enabled).getValue();
  }

  @Step
  public TopicDetails selectSaveThisFilterCheckboxMdl(boolean select) {
    WebUtil.selectElement(saveThisFilterCheckBoxAddFilterMdl, select);
    return this;
  }

  @Step
  public boolean isSaveThisFilterCheckBoxSelected() {
    return WebUtil.isSelected(saveThisFilterCheckBoxAddFilterMdl);
  }

  @Step
  public TopicDetails setDisplayNameFldAddFilterMdl(String displayName) {
    displayNameInputAddFilterMdl.shouldBe(enabled).setValue(displayName);
    return this;
  }

  @Step
  public TopicDetails clickAddFilterBtnAndCloseMdl(boolean closeModal) {
    addFilterBtnAddFilterMdl.shouldBe(enabled).click();
    if (closeModal) {
      addFilterCodeModalTitle.shouldBe(Condition.hidden);
    } else {
      addFilterCodeModalTitle.shouldBe(Condition.visible);
    }
    return this;
  }

  @Step
  public TopicDetails clickSaveFilterBtnAndCloseMdl(boolean closeModal) {
    saveFilterBtnEditFilterMdl.shouldBe(enabled).click();
    if (closeModal) {
      addFilterCodeModalTitle.shouldBe(Condition.hidden);
    } else {
      addFilterCodeModalTitle.shouldBe(Condition.visible);
    }
    return this;
  }

  @Step
  public boolean isAddFilterBtnAddFilterMdlEnabled() {
    return WebUtil.isEnabled(addFilterBtnAddFilterMdl);
  }

  @Step
  public boolean isBackButtonEnabled() {
    return WebUtil.isEnabled(backBtn);
  }

  @Step
  public boolean isNextButtonEnabled() {
    return WebUtil.isEnabled(nextBtn);
  }

  @Step
  public boolean isActiveFilterVisible(String filterName) {
    return WebUtil.isVisible($x(String.format(activeFilterNameLocator, filterName)));
  }

  @Step
  public String getSearchFieldValue() {
    return searchFld.shouldBe(Condition.visible).getValue();
  }

  public List<SelenideElement> getAllAddFilterModalVisibleElements() {
    return Arrays.asList(savedFiltersLink, displayNameInputAddFilterMdl, addFilterBtnAddFilterMdl,
        cancelBtnAddFilterMdl);
  }

  public List<SelenideElement> getAllAddFilterModalEnabledElements() {
    return Arrays.asList(displayNameInputAddFilterMdl, cancelBtnAddFilterMdl);
  }

  public List<SelenideElement> getAllAddFilterModalDisabledElements() {
    return Collections.singletonList(addFilterBtnAddFilterMdl);
  }

  @Step
  public TopicDetails openConsumerGroup(String consumerId) {
    $x(String.format(consumerIdLocator, consumerId)).click();
    return this;
  }

  private void selectYear(int expectedYear) {
    while (getActualCalendarDate().getYear() > expectedYear) {
      WebUtil.clickByJavaScript(previousMonthButton);
      sleep(1000);
      if (LocalTime.now().plusMinutes(3).isBefore(LocalTime.now())) {
        throw new IllegalArgumentException("Unable to select year");
      }
    }
  }

  private void selectMonth(int expectedMonth) {
    while (getActualCalendarDate().getMonthValue() > expectedMonth) {
      WebUtil.clickByJavaScript(previousMonthButton);
      sleep(1000);
      if (LocalTime.now().plusMinutes(3).isBefore(LocalTime.now())) {
        throw new IllegalArgumentException("Unable to select month");
      }
    }
  }

  private void selectDay(int expectedDay) {
    Objects.requireNonNull($$x(String.format(dayCellLtr, expectedDay)).asFixedIterable().stream()
        .filter(day -> !Objects.requireNonNull(day.getAttribute("class")).contains("outside-month"))
        .findFirst().orElseThrow()).shouldBe(enabled).click();
  }

  private void setTime(LocalDateTime dateTime) {
    calendarTimeFld.shouldBe(enabled)
        .sendKeys(String.valueOf(dateTime.getHour()), String.valueOf(dateTime.getMinute()));
  }

  @Step
  public TopicDetails selectDateAndTimeByCalendar(LocalDateTime dateTime) {
    setTime(dateTime);
    selectYear(dateTime.getYear());
    selectMonth(dateTime.getMonthValue());
    selectDay(dateTime.getDayOfMonth());
    return this;
  }

  private LocalDate getActualCalendarDate() {
    String monthAndYearStr = actualCalendarDate.getText().trim();
    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ofPattern("MMMM yyyy"))
        .toFormatter(Locale.ENGLISH);
    YearMonth yearMonth = formatter.parse(monthAndYearStr, YearMonth::from);
    return yearMonth.atDay(1);
  }

  @Step
  public TopicDetails openCalendarMode() {
    modeField.shouldBe(enabled).click();
    actualCalendarDate.shouldBe(Condition.visible);
    return this;
  }

  @Step
  public int getMessageCountAmount() {
    return Integer.parseInt(messageAmountCell.getText().trim());
  }

  private List<TopicDetails.MessageGridItem> initItems() {
    List<TopicDetails.MessageGridItem> gridItemList = new ArrayList<>();
    gridItems.shouldHave(CollectionCondition.sizeGreaterThan(0))
        .forEach(item -> gridItemList.add(new TopicDetails.MessageGridItem(item)));
    return gridItemList;
  }

  @Step
  public TopicDetails.MessageGridItem getMessageByOffset(int offset) {
    return initItems().stream()
        .filter(e -> e.getOffset() == offset)
        .findFirst().orElseThrow();
  }

  @Step
  public TopicDetails.MessageGridItem getMessageByKey(String key) {
    return initItems().stream()
        .filter(e -> e.getKey().equals(key))
        .findFirst().orElseThrow();
  }

  @Step
  public List<MessageGridItem> getAllMessages() {
    return initItems();
  }

  @Step
  public TopicDetails.MessageGridItem getRandomMessage() {
    return getMessageByOffset(nextInt(0, initItems().size() - 1));
  }

  @Getter
  public enum TopicMenu {
    OVERVIEW("Overview"),
    MESSAGES("Messages"),
    CONSUMERS("Consumers"),
    SETTINGS("Settings");

    private final String value;

    TopicMenu(String value) {
      this.value = value;
    }
  }

  public static class MessageGridItem extends BasePage {

    private final SelenideElement element;

    private MessageGridItem(SelenideElement element) {
      this.element = element;
    }

    @Step
    public MessageGridItem clickExpand() {
      WebUtil.clickByJavaScript(element.$x("./td[1]/span"));
      return this;
    }

    private SelenideElement getOffsetElm() {
      return element.$x("./td[2]");
    }

    @Step
    public int getOffset() {
      return Integer.parseInt(getOffsetElm().getText().trim());
    }

    @Step
    public int getPartition() {
      return Integer.parseInt(element.$x("./td[3]").getText().trim());
    }

    @Step
    public LocalDateTime getTimestamp() {
      String timestampValue = element.$x("./td[4]/div").getText().trim();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy, HH:mm:ss");
      return LocalDateTime.parse(timestampValue, formatter);
    }

    @Step
    public String getKey() {
      return element.$x("./td[5]").getText().trim();
    }

    @Step
    public String getValue() {
      return element.$x("./td[6]").getAttribute("title");
    }

    @Step
    public MessageGridItem openDotMenu() {
      getOffsetElm().hover();
      element.$x("./td[7]/div/button[@aria-label='Dropdown Toggle']")
          .shouldBe(Condition.visible).click();
      return this;
    }

    @Step
    public MessageGridItem clickCopyToClipBoard() {
      WebUtil.clickByJavaScript(element.$x("./td[7]//li[text() = 'Copy to clipboard']")
          .shouldBe(Condition.visible));
      return this;
    }

    @Step
    public MessageGridItem clickSaveAsFile() {
      WebUtil.clickByJavaScript(element.$x("./td[7]//li[text() = 'Save as a file']")
          .shouldBe(Condition.visible));
      return this;
    }
  }
}
