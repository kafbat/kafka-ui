package io.kafbat.ui.settings.listeners;

import static java.nio.file.Files.newInputStream;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import com.codeborne.selenide.Screenshots;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.testng.AllureTestNg;
import java.io.File;
import java.io.IOException;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class AllureListener extends AllureTestNg implements ITestListener {

  private void takeScreenshot() {
    File screenshot = Screenshots.takeScreenShotAsFile();
    try {
      if (!isEmpty(screenshot)) {
        Allure.addAttachment(screenshot.getName(), newInputStream(screenshot.toPath()));
      } else {
        LoggerFactory.getLogger(AllureLifecycle.class).error("Could not take screenshot");
      }
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void onTestFailure(ITestResult result) {
    takeScreenshot();
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    takeScreenshot();
  }
}
