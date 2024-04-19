package io.kafbat.ui.utilities;

import com.codeborne.selenide.Selenide;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeUtil {

  public static void waitUntilNewMinuteStarted() {
    int secondsLeft = 60 - LocalTime.now().getSecond();
    log.debug("\nwaitUntilNewMinuteStarted: {}s", secondsLeft);
    Selenide.sleep(secondsLeft * 1000);
  }
}
