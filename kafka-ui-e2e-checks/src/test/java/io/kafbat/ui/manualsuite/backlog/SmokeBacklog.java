package io.kafbat.ui.manualsuite.backlog;

import io.kafbat.ui.manualsuite.BaseManualTest;
import io.kafbat.ui.utilities.qase.annotations.Automation;
import io.kafbat.ui.utilities.qase.annotations.Suite;
import io.kafbat.ui.qasesuite.BaseQaseTest;
import io.kafbat.ui.utilities.qase.enums.State;
import io.qase.api.annotation.QaseId;
import org.testng.annotations.Test;

public class SmokeBacklog extends BaseManualTest {

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.TOPICS_PROFILE_SUITE_ID)
  @QaseId(335)
  @Test
  public void testCaseA() {
  }

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.TOPICS_PROFILE_SUITE_ID)
  @QaseId(336)
  @Test
  public void testCaseB() {
  }

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.TOPICS_PROFILE_SUITE_ID)
  @QaseId(343)
  @Test
  public void testCaseC() {
  }

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.SCHEMAS_SUITE_ID)
  @QaseId(345)
  @Test
  public void testCaseD() {
  }

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.SCHEMAS_SUITE_ID)
  @QaseId(346)
  @Test
  public void testCaseE() {
  }

  @Automation(state = State.TO_BE_AUTOMATED)
  @Suite(id = BaseQaseTest.TOPICS_PROFILE_SUITE_ID)
  @QaseId(347)
  @Test
  public void testCaseF() {
  }

  @Automation(state = State.NOT_AUTOMATED)
  @Suite(id = BaseQaseTest.TOPICS_SUITE_ID)
  @QaseId(50)
  @Test
  public void testCaseG() {
  }

  @Automation(state = State.NOT_AUTOMATED)
  @Suite(id = BaseQaseTest.SCHEMAS_SUITE_ID)
  @QaseId(351)
  @Test
  public void testCaseH() {
  }
}
