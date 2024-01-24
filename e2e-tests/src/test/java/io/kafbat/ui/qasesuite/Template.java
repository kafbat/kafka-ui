package io.kafbat.ui.qasesuite;

import io.kafbat.ui.utilities.qase.annotations.Automation;
import io.kafbat.ui.utilities.qase.annotations.Status;
import io.kafbat.ui.utilities.qase.annotations.Suite;
import io.kafbat.ui.utilities.qase.enums.State;
import io.qase.api.annotation.QaseTitle;
import io.qase.api.annotation.Step;

public class Template extends BaseQaseTest {

  /**
   * this class is a kind of placeholder or example, use is as template to create new one
   * copy Template into e2e-tests/src/test/java/io/kafbat/ui/qaseSuite/
   * place it into regarding folder and rename according to test case summary from Qase.io
   * uncomment @Test and set all annotations according to e2e-tests/QASE.md
   */

  @Automation(state = State.NOT_AUTOMATED)
  @QaseTitle("testCaseA title")
  @Status(status = io.kafbat.ui.utilities.qase.enums.Status.DRAFT)
  @Suite(id = 0)
  //  @org.testng.annotations.Test
  public void testCaseA() {
    stepA();
    stepB();
    stepC();
    stepD();
    stepE();
    stepF();
  }

  @Step("stepA action")
  private void stepA() {
  }

  @Step("stepB action")
  private void stepB() {
  }

  @Step("stepC action")
  private void stepC() {
  }

  @Step("stepD action")
  private void stepD() {
  }

  @Step("stepE action")
  private void stepE() {
  }

  @Step("stepF action")
  private void stepF() {
  }
}
