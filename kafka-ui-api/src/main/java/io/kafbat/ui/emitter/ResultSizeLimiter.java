package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageEventDTO;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class ResultSizeLimiter implements Predicate<TopicMessageEventDTO> {
  private final AtomicInteger processed = new AtomicInteger();
  private final int limit;

  public ResultSizeLimiter(int limit) {
    this.limit = limit;
  }

  @Override
  public boolean test(TopicMessageEventDTO event) {
    if (event.getType().equals(TopicMessageEventDTO.TypeEnum.MESSAGE)) {
      final int i = processed.incrementAndGet();
      return i <= limit;
    }
    return true;
  }
}
