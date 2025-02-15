package io.kafbat.ui.serdes.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.util.ResourceUtil;
import java.io.IOException;
import java.util.Map;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class ProtobufRemoteFileSerdeTest {

  private static final String samplePersonMsgJson =
      "{ \"name\": \"My Name\",\"id\": 101, \"email\": \"user1@example.com\", \"phones\":[] }";

  private static final String sampleBookMsgJson = "{\"version\": 1, \"people\": ["
      + "{ \"name\": \"My Name\",\"id\": 102, \"email\": \"addrBook@example.com\", \"phones\":[]}]}";

  // Sample message of type `test.Person`
  private byte[] personMessageBytes;

  @BeforeEach
  void setUp() throws Exception {
    var schema = new ClassPathResource("protobuf-serde/address-book.proto");

    var addressBookSchema = new ProtobufSchema(ResourceUtil.readAsString(schema));
    var builder = addressBookSchema.newMessageBuilder("test.Person");
    JsonFormat.parser().merge(samplePersonMsgJson, builder);
    personMessageBytes = builder.build().toByteArray();
  }

  @Test
  void serializeUsesTopicsMappingToFindMsgDescriptor() throws IOException {
    var httpClient = mock(HttpClient.class);

    HttpClient.ResponseReceiver responseReceiver = mock(HttpClient.ResponseReceiver.class);
    when(httpClient.get()).thenReturn(responseReceiver);
    when(responseReceiver.uri(anyString())).thenReturn(responseReceiver);
    when(responseReceiver.responseSingle(any())).thenReturn(Mono.just(
        new ProtobufRemoteFileSerde.RemoteResponse(
            HttpResponseStatus.OK,
            new ProtobufRemoteFileSerde.ResolvedSchema(
                "test.Person",
                ResourceUtil.readAsString(new ClassPathResource("protobuf-serde/address-book.proto"))
            )
        )
    ));

    var serde = new ProtobufRemoteFileSerde();
    serde.configure(
        new ProtobufRemoteFileSerde.Configuration(
            httpClient,
            "/test",
            Map.of("test", "test")
        )
    );

    var deserializedPerson = serde.deserializer("persons", Serde.Target.VALUE)
        .deserialize(null, personMessageBytes);
    assertJsonEquals(samplePersonMsgJson, deserializedPerson.getResult());
  }

  @SneakyThrows
  private void assertJsonEquals(String expectedJson, String actualJson) {
    var mapper = new JsonMapper();
    assertThat(mapper.readTree(actualJson)).isEqualTo(mapper.readTree(expectedJson));
  }
}
