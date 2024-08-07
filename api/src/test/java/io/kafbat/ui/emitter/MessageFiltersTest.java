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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
      var f = celScriptFilter("record.partition == 1");
      assertTrue(f.test(msg().partition(1)));
      assertFalse(f.test(msg().partition(0)));
    }

    @Test
    void canCheckOffset() {
      var f = celScriptFilter("record.offset == 100");
      assertTrue(f.test(msg().offset(100L)));
      assertFalse(f.test(msg().offset(200L)));
    }

    @Test
    void canCheckHeaders() {
      var f = celScriptFilter("record.headers.size() == 2 && record.headers['k1'] == 'v1'");
      assertTrue(f.test(msg().headers(Map.of("k1", "v1", "k2", "v2"))));
      assertFalse(f.test(msg().headers(Map.of("k1", "unexpected", "k2", "v2"))));

      f = celScriptFilter("record.headers.size() == 1 && !has(record.headers.k1) && record.headers['k2'] == 'v2'");
      assertTrue(f.test(msg().headers(Map.of("k2", "v2"))));

      f = celScriptFilter("!has(record.headers) || record.headers.size() == 0");
      assertTrue(f.test(msg().headers(Map.of())));
      assertTrue(f.test(msg()));
    }

    @Test
    void canCheckTimestampMs() {
      var ts = OffsetDateTime.now();
      var f = celScriptFilter("record.timestampMs == " + ts.toInstant().toEpochMilli());
      assertTrue(f.test(msg().timestamp(ts)));
      assertFalse(f.test(msg().timestamp(ts.plusSeconds(1L))));
    }

    @Test
    void canCheckValueAsText() {
      var f = celScriptFilter("record.valueAsText == 'some text'");
      assertTrue(f.test(msg().content("some text")));
      assertFalse(f.test(msg().content("some other text")));
    }

    @Test
    void canCheckKeyAsText() {
      var f = celScriptFilter("record.keyAsText == 'some text'");
      assertTrue(f.test(msg().key("some text")));
      assertFalse(f.test(msg().key("some other text")));
    }

    @Test
    void canCheckKeyAsJsonObjectIfItCanBeParsedToJson() {
      var f = celScriptFilter("has(record.key.name.first) && record.key.name.first == 'user1'");
      assertTrue(f.test(msg().key("{ \"name\" : { \"first\" : \"user1\" } }")));
      assertFalse(f.test(msg().key("{ \"name\" : { \"first\" : \"user2\" } }")));
      assertFalse(f.test(msg().key("{ \"name\" : { \"second\" : \"user2\" } }")));
    }

    @Test
    void keySetToKeyStringIfCantBeParsedToJson() {
      var f = celScriptFilter("has(record.keyAsText) && record.keyAsText == 'not json' && record.key == 'not json'");
      assertTrue(f.test(msg().key("not json")));
    }

    @Test
    void keyAndKeyAsTextSetToNullIfRecordsKeyIsNull() {
      var f = celScriptFilter("!has(record.key)");
      assertTrue(f.test(msg().key(null)));

      f = celScriptFilter("!has(record.keyAsText)");
      assertTrue(f.test(msg().key(null)));
    }

    @Test
    void canCheckValueAsJsonObjectIfItCanBeParsedToJson() {
      var f = celScriptFilter("has(record.value.name.first) && record.value.name.first == 'user1'");
      assertTrue(f.test(msg().content("{ \"name\" : { \"first\" : \"user1\" } }")));
      assertFalse(f.test(msg().content("{ \"name\" : { \"first\" : \"user2\" } }")));
      assertFalse(f.test(msg().content("{ \"name\" : { \"second\" : \"user2\" } }")));
    }

    @Test
    void valueSetToContentStringIfCantBeParsedToJson() {
      var f = celScriptFilter("record.value == \"not json\"");
      assertTrue(f.test(msg().content("not json")));
    }

    @Test
    void valueAndValueAsTextSetToNullIfRecordsContentIsNull() {
      var f = celScriptFilter("!has(record.value)");
      assertTrue(f.test(msg().content(null)));

      f = celScriptFilter("!has(record.valueAsText)");
      assertTrue(f.test(msg().content(null)));
    }

    @Test
    void filterSpeedIsAtLeast5kPerSec() {
      var f = celScriptFilter("record.value.name.first == 'user1' && record.keyAsText.startsWith('a') ");

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
      // noinspection ResultOfMethodCallIgnored
      toFilter.stream().filter(f).count();

      long before = System.currentTimeMillis();
      long matched = toFilter.stream().filter(f).count();
      long took = System.currentTimeMillis() - before;

      assertThat(took).isLessThan(1000);
      assertThat(matched).isGreaterThan(0);
    }
  }

  @Test
  void testBase64DecodingWorks() {
    var uuid = UUID.randomUUID().toString();
    var msg = "test." + Base64.getEncoder().encodeToString(uuid.getBytes());
    var f = celScriptFilter("string(base64.decode(record.value.split('.')[1])).contains('" + uuid + "')");
    assertTrue(f.test(msg().content(msg)));
  }

  private TopicMessageDTO msg() {
    return TopicMessageDTO.builder()
        .partition(1)
        .offset(-1L)
        .timestamp(OffsetDateTime.now())
        .build();
  }
}
