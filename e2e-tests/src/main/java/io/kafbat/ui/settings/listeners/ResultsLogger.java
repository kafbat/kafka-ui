package io.kafbat.ui.settings.listeners;

import static io.kafbat.ui.utilities.StringUtil.getDuplicates;

import java.text.DecimalFormat;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

@Slf4j
public class ResultsLogger extends TestListenerAdapter {

  private static final String PREFIX = "[INFO] ";
  private static final String PASSED_STATUS = "PASSED";
  private static final String FAILED_STATUS = "FAILED";
  private static final String SKIPPED_STATUS = "SKIPPED";

  private static void logTestStatus(String status, ITestResult result) {
    String testStatus = String.format("\n[INFO] TEST %s: %s",
        status, result.getMethod().getQualifiedName());
    int dashCount = testStatus.length() - PREFIX.length();
    String separator = String.format("\n%s%s", PREFIX,
        getDuplicates("-", dashCount - 1));
    log.info("{}{}{}", separator, testStatus, separator);
  }

  private static String alignTitleToLength(String title, int target) {
    int indentDiff = (title.length() - target) / 2;
    String result = title.substring(indentDiff, title.length() - indentDiff);
    if (result.length() > target) {
      result = result.substring(1);
    }
    return result;
  }

  private static int appendResults(StringBuilder suiteResults, Set<ITestResult> testResults, String testStatus) {
    if (testResults.isEmpty()) {
      return 0;
    }
    StringBuilder subResults = new StringBuilder();
    subResults.append(String.format("\n%s\n%s%s TESTS: %d", PREFIX, PREFIX, testStatus, testResults.size()));
    if (!testStatus.equals(PASSED_STATUS)) {
      testResults.forEach(result ->
          subResults.append(String.format("\n%s%s", PREFIX, result.getMethod().getQualifiedName())));
    }
    suiteResults.append(subResults);
    return testResults.size();
  }

  @Override
  public void onTestStart(final ITestResult testResult) {
    logTestStatus("STARTED", testResult);
  }

  @Override
  public void onTestSuccess(final ITestResult testResult) {
    logTestStatus(PASSED_STATUS, testResult);
  }

  @Override
  public void onTestFailure(final ITestResult testResult) {
    logTestStatus(FAILED_STATUS, testResult);
  }

  @Override
  public void onTestSkipped(final ITestResult testResult) {
    logTestStatus(SKIPPED_STATUS, testResult);
  }

  @Override
  public void onFinish(ITestContext context) {
    String separator = getDuplicates("=", 72);
    String titleIndent = getDuplicates("-", separator.length() / 2);
    String header = String.format("%s< %s >%s", titleIndent, context.getName(), titleIndent);
    header = alignTitleToLength(header, separator.length());
    String logHeader = String.format("\n%s%s\n%s%s", PREFIX, separator, PREFIX, header);
    StringBuilder suiteResults = new StringBuilder();
    suiteResults.append(logHeader);
    int testCount = 0;
    Set<ITestResult> passedResults = context.getPassedTests().getAllResults();
    testCount += appendResults(suiteResults, passedResults, PASSED_STATUS);
    testCount += appendResults(suiteResults, context.getFailedTests().getAllResults(), FAILED_STATUS);
    testCount += appendResults(suiteResults, context.getSkippedTests().getAllResults(), SKIPPED_STATUS);
    String total = new DecimalFormat("##0.00%").format((double) passedResults.size() / testCount);
    String footer = String.format("%s[ %s ]%s", titleIndent, total, titleIndent);
    footer = alignTitleToLength(footer, separator.length());
    String logFooter = String.format("\n%s\n%s%s\n%s%s", PREFIX, PREFIX, footer, PREFIX, separator);
    suiteResults.append(logFooter);
    log.info(suiteResults.toString());
  }
}
