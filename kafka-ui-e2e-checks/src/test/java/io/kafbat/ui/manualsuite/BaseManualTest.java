package io.kafbat.ui.manualsuite;

import io.kafbat.ui.settings.listeners.QaseResultListener;
import io.kafbat.ui.utilities.qase.QaseSetup;
import io.kafbat.ui.utilities.qase.annotations.Automation;
import io.kafbat.ui.utilities.qase.enums.State;
import java.lang.reflect.Method;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

@Listeners(QaseResultListener.class)
public abstract class BaseManualTest {

  @BeforeSuite
  public void beforeSuite() {
    QaseSetup.qaseIntegrationSetup();
  }

  @BeforeMethod
  public void beforeMethod(Method method) {
    if (method.getAnnotation(Automation.class).state().equals(State.NOT_AUTOMATED)
        || method.getAnnotation(Automation.class).state().equals(State.TO_BE_AUTOMATED)) {
      throw new SkipException("Skip test exception");
    }
  }
}
