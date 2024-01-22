package io.kafbat.ui.manualsuite.suite;

import io.kafbat.ui.manualsuite.BaseManualTest;
import io.kafbat.ui.utilities.qase.annotations.Automation;
import io.kafbat.ui.utilities.qase.enums.State;
import io.qase.api.annotation.QaseId;
import org.testng.annotations.Test;

public class DataMaskingTest extends BaseManualTest {

  @Automation(state = State.NOT_AUTOMATED)
  @QaseId(262)
  @Test
  public void testCaseA() {
  }

  @Automation(state = State.NOT_AUTOMATED)
  @QaseId(264)
  @Test
  public void testCaseB() {
  }

  @Automation(state = State.NOT_AUTOMATED)
  @QaseId(265)
  @Test
  public void testCaseC() {
  }
}
