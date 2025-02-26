package io.kafbat.ui.settings.drivers;

import static com.codeborne.selenide.Selenide.clearBrowserCookies;
import static com.codeborne.selenide.Selenide.clearBrowserLocalStorage;
import static com.codeborne.selenide.Selenide.refresh;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.kafbat.ui.settings.BaseSource;
import io.qameta.allure.Step;
import io.qameta.allure.selenide.AllureSelenide;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;

@Slf4j
public abstract class WebDriver {

  private static final String MAC_OS_CHROME_BIN_PATH = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
  private static final String SELENIDE_RESULTS_PATH = "build/selenide-results";

  @Step
  public static void browserSetup() {
    Configuration.headless = BaseSource.HEADLESS;
    Configuration.browser = "chrome";
    Configuration.browserSize = "1920x1080";
    Configuration.pageLoadTimeout = 180000;
    Configuration.savePageSource = true;
    Configuration.screenshots = true;
    Configuration.downloadsFolder = String.format("%s/downloads", SELENIDE_RESULTS_PATH);
    Configuration.reportsFolder = String.format("%s/reports", SELENIDE_RESULTS_PATH);
    ChromeOptions chromeOptions = new ChromeOptions()
        //.addArguments("--remote-allow-origins=*")
        .addArguments("--disable-dev-shm-usage")
        .addArguments("--disable-extensions")
        .addArguments("--disable-gpu")
        .addArguments("--no-sandbox")
        .addArguments("--lang=en_US");
    if (true) {
      Configuration.remote = BaseSource.REMOTE_URL;
      Configuration.remoteConnectionTimeout = 180000;
      Configuration.remoteReadTimeout = 180000;
      Map<String, Object> selenoidOptions = new HashMap<>();
      selenoidOptions.put("enableVNC", true);
      selenoidOptions.put("enableLog", true);
      selenoidOptions.put("enableVideo", false);
      selenoidOptions.put("sessionTimeout", "30m");
      chromeOptions.setCapability("selenoid:options", selenoidOptions);
    }
    /*} else if (System.getProperty("os.name").equals("Mac OS X")) {
      Configuration.browserBinary = MAC_OS_CHROME_BIN_PATH;
    }*/
    System.setProperty("webdriver.chrome.verboseLogging", "true");
    Configuration.browserCapabilities = chromeOptions;
  }

  private static org.openqa.selenium.WebDriver getWebDriver() {
    try {
      return WebDriverRunner.getWebDriver();
    } catch (IllegalStateException ex) {
      browserSetup();
      Selenide.open();
      return WebDriverRunner.getWebDriver();
    }
  }

  @Step
  public static void openUrl(String url) {
    org.openqa.selenium.WebDriver driver = getWebDriver();
    if (!driver.getCurrentUrl().equals(url)) {
      driver.get(url);
    }
  }

  @Step
  public static void browserClear() {
    getWebDriver();
    try {
      clearBrowserCookies();
      clearBrowserLocalStorage();
    } catch (Throwable ignored) {
    }
    refresh();
  }

  @Step
  public static void browserQuit() {
    org.openqa.selenium.WebDriver driver = null;
    try {
      driver = WebDriverRunner.getWebDriver();
    } catch (Throwable ignored) {
    }
    if (driver != null) {
      driver.quit();
    }
  }

  @Step
  public static void selenideLoggerSetup() {
    SelenideLogger.addListener("AllureSelenide", new AllureSelenide()
        .savePageSource(true)
        .screenshots(true));
  }
}
