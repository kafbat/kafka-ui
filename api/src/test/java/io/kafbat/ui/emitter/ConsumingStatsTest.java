package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.kafbat.ui.model.TopicMessageEventDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.FluxSink;

class ConsumingStatsTest {

  FluxSink<TopicMessageEventDTO> sink;
  ArgumentCaptor<TopicMessageEventDTO> eventCaptor;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    sink = mock(FluxSink.class);
    eventCaptor = ArgumentCaptor.forClass(TopicMessageEventDTO.class);
  }

  @Test
  void sendConsumingEvtInRangeAccumulatesAndEmits() {
    ConsumingStats stats = new ConsumingStats();
    stats.sendConsumingEvt(sink, 5, 100L, 10L);
    stats.sendConsumingEvt(sink, 3, 50L, 5L);

    verify(sink, times(2)).next(eventCaptor.capture());
    List<TopicMessageEventDTO> events = eventCaptor.getAllValues();
    assertThat(events.get(0).getType()).isEqualTo(TopicMessageEventDTO.TypeEnum.CONSUMING);
    assertThat(events.get(0).getConsuming().getMessagesConsumed()).isEqualTo(5);
    assertThat(events.get(0).getConsuming().getBytesConsumed()).isEqualTo(100L);
    assertThat(events.get(0).getConsuming().getElapsedMs()).isEqualTo(10L);

    assertThat(events.get(1).getConsuming().getMessagesConsumed()).isEqualTo(8);
    assertThat(events.get(1).getConsuming().getBytesConsumed()).isEqualTo(150L);
    assertThat(events.get(1).getConsuming().getElapsedMs()).isEqualTo(15L);
  }

  @Test
  void sendFinishEventIncludesAccumulatedInRangeStats() {
    ConsumingStats stats = new ConsumingStats();
    stats.sendConsumingEvt(sink, 2, 200L, 100L);
    stats.sendFinishEvent(sink, null);

    verify(sink, times(2)).next(eventCaptor.capture());
    TopicMessageEventDTO doneEvent = eventCaptor.getAllValues().get(1);
    assertThat(doneEvent.getType()).isEqualTo(TopicMessageEventDTO.TypeEnum.DONE);
    assertThat(doneEvent.getConsuming().getMessagesConsumed()).isEqualTo(2);
    assertThat(doneEvent.getConsuming().getBytesConsumed()).isEqualTo(200L);
    assertThat(doneEvent.getConsuming().getElapsedMs()).isEqualTo(100L);
  }
}
