package io.kafbat.ui.emitter;

import static io.kafbat.ui.emitter.MessageFilters.celScriptFilter;
import static io.kafbat.ui.emitter.MessageFilters.containsStringFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.exception.CelException;
import io.kafbat.ui.model.TopicMessageDTO;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessageFiltersTest {

  @Nested
  class StringContainsFilter {

    Predicate<TopicMessageDTO> filter = containsStringFilter("abC");

    @Test
    void returnsTrueWhenStringContainedInKeyOrContentOrInBoth() {
      assertTrue(
          filter.test(msg().key("contains abCd").content("some str"))
      );

      assertTrue(
          filter.test(msg().key("some str").content("contains abCd"))
      );

      assertTrue(
          filter.test(msg().key("contains abCd").content("contains abCd"))
      );
    }

    @Test
    void returnsFalseOtherwise() {
      assertFalse(
          filter.test(msg().key("some str").content("some str"))
      );

      assertFalse(
          filter.test(msg().key(null).content(null))
      );

      assertFalse(
          filter.test(msg().key("aBc").content("AbC"))
      );
    }

  }

  @Nested
  class CelScriptFilter {

    @Test
    void throwsExceptionOnInvalidCelSyntax() {
      assertThrows(CelException.class,
          () -> celScriptFilter("this is an invalid CEL syntax = 1"));
    }

    @Test
    void canCheckPartition() {
      var f = celScriptFilter("partition == 1");
      assertTrue(f.test(msg().partition(1)));
      assertFalse(f.test(msg().partition(0)));
    }

    @Test
    void canCheckOffset() {
      var f = celScriptFilter("offset == 100");
      assertTrue(f.test(msg().offset(100L)));
      assertFalse(f.test(msg().offset(200L)));
    }

    @Test
    void canCheckHeaders() {
      var f = celScriptFilter("headers.size() == 2 && headers['k1'] == 'v1'");
      assertTrue(f.test(msg().headers(Map.of("k1", "v1", "k2", "v2"))));
      assertFalse(f.test(msg().headers(Map.of("k1", "unexpected", "k2", "v2"))));

      f = celScriptFilter("headers.size() == 1 && !has(headers.k1)");
      assertTrue(f.test(msg().headers(Map.of("k2", "v2"))));

      f = celScriptFilter("headers.size() == 0");
      assertTrue(f.test(msg().headers(Map.of())));
      assertTrue(f.test(msg()));
    }

    @Test
    void canCheckTimestampMs() {
      var ts = OffsetDateTime.now();
      var f = celScriptFilter("timestampMs == " + ts.toInstant().toEpochMilli());
      assertTrue(f.test(msg().timestamp(ts)));
      assertFalse(f.test(msg().timestamp(ts.plus(1L, ChronoUnit.SECONDS))));
    }

    @Test
    void canCheckValueAsText() {
      var f = celScriptFilter("valueAsText == 'some text'");
      assertTrue(f.test(msg().content("some text")));
      assertFalse(f.test(msg().content("some other text")));
    }

    @Test
    void canCheckKeyAsText() {
      var f = celScriptFilter("keyAsText == 'some text'");
      assertTrue(f.test(msg().key("some text")));
      assertFalse(f.test(msg().key("some other text")));
    }

    @Test
    void canCheckKeyAsJsonObjectIfItCanBeParsedToJson() {
      var f = celScriptFilter("has(key.name.first) && key.name.first == 'user1'");
      assertTrue(f.test(msg().key("{ \"name\" : { \"first\" : \"user1\" } }")));
      assertFalse(f.test(msg().key("{ \"name\" : { \"first\" : \"user2\" } }")));
      assertFalse(f.test(msg().key("{ \"name\" : { \"second\" : \"user2\" } }")));
    }

    @Test
    void keySetToKeyStringIfCantBeParsedToJson() {
      var f = celScriptFilter("key == \"not json\"");
      assertTrue(f.test(msg().key("not json")));
    }

    @Test
    void keyAndKeyAsTextSetToNullIfRecordsKeyIsNull() {
      var f = celScriptFilter("key.size() == 0");
      assertTrue(f.test(msg().key(null)));

      f = celScriptFilter("keyAsText.size() == 0");
      assertTrue(f.test(msg().key(null)));
    }

    @Test
    void canCheckValueAsJsonObjectIfItCanBeParsedToJson() {
      var f = celScriptFilter("has(value.name.first) && value.name.first == 'user1'");
      assertTrue(f.test(msg().content("{ \"name\" : { \"first\" : \"user1\" } }")));
      assertFalse(f.test(msg().content("{ \"name\" : { \"first\" : \"user2\" } }")));
      assertFalse(f.test(msg().content("{ \"name\" : { \"second\" : \"user2\" } }")));
    }

    @Test
    void valueSetToContentStringIfCantBeParsedToJson() {
      var f = celScriptFilter("value == \"not json\"");
      assertTrue(f.test(msg().content("not json")));
    }

    @Test
    void valueAndValueAsTextSetToNullIfRecordsContentIsNull() {
      var f = celScriptFilter("value.size() == 0");
      assertTrue(f.test(msg().content(null)));

      f = celScriptFilter("valueAsText.size() == 0");
      assertTrue(f.test(msg().content(null)));
    }

    @Test
    void filterSpeedIsAtLeast5kPerSec() {
      var f = celScriptFilter("value.name.first == 'user1' && keyAsText.startsWith('a') ");

      List<TopicMessageDTO> toFilter = new ArrayList<>();
      for (int i = 0; i < 5_000; i++) {
        String name = i % 2 == 0 ? "user1" : RandomStringUtils.randomAlphabetic(10);
        String randString = RandomStringUtils.randomAlphabetic(30);
        String jsonContent = String.format(
            "{ \"name\" : {  \"randomStr\": \"%s\", \"first\" : \"%s\"} }",
            randString, name);
        toFilter.add(msg().content(jsonContent).key(randString));
      }
      // first iteration for warmup
      toFilter.stream().filter(f).count();

      long before = System.currentTimeMillis();
      long matched = toFilter.stream().filter(f).count();
      long took = System.currentTimeMillis() - before;

      assertThat(took).isLessThan(1000);
      assertThat(matched).isGreaterThan(0);
    }
  }

  private TopicMessageDTO msg() {
    return new TopicMessageDTO()
        .timestamp(OffsetDateTime.now())
        .offset(-1L)
        .partition(1);
  }
}
