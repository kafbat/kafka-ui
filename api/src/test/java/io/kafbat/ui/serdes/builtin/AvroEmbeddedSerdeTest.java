package io.kafbat.ui.serdes.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.PropertyResolverImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class AvroEmbeddedSerdeTest {

  private AvroEmbeddedSerde avroEmbeddedSerde;

  @BeforeEach
  void init() {
    avroEmbeddedSerde = new AvroEmbeddedSerde();
    avroEmbeddedSerde.configure(
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty()
    );
  }

  @ParameterizedTest
  @EnumSource
  void canDeserializeReturnsTrueForAllTargets(Serde.Target target) {
    assertThat(avroEmbeddedSerde.canDeserialize("anyTopic", target))
        .isTrue();
  }

  @ParameterizedTest
  @EnumSource
  void canSerializeReturnsFalseForAllTargets(Serde.Target target) {
    assertThat(avroEmbeddedSerde.canSerialize("anyTopic", target))
        .isFalse();
  }

  @Test
  void deserializerParsesAvroDataWithEmbeddedSchema() throws Exception {
    Schema schema = new Schema.Parser().parse("""
        {
          "type": "record",
          "name": "TestAvroRecord",
          "fields": [
            { "name": "field1", "type": "string" },
            { "name": "field2", "type": "int" }
          ]
        }
        """
    );
    GenericRecord record = new GenericData.Record(schema);
    record.put("field1", "this is test msg");
    record.put("field2", 100500);

    String jsonRecord = new String(AvroSchemaUtils.toJson(record));
    byte[] serializedRecordBytes = serializeAvroWithEmbeddedSchema(record);

    var deserializer = avroEmbeddedSerde.deserializer("anyTopic", Serde.Target.KEY);
    DeserializeResult result = deserializer.deserialize(null, serializedRecordBytes);
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.JSON);
    assertThat(result.getAdditionalProperties()).isEmpty();
    assertJsonEquals(jsonRecord, result.getResult());
  }

  @Test
  void readsIcebergEmbeddedSchemaAndNestedControlPayload() throws Exception {
    var schema = new Schema.Parser().parse("""
        {"type":"record","name":"Event","fields":[
          {"name":"group_id","type":"string"},
          {"name":"payload","type":{"type":"record","name":"Payload","fields":[
            {"name":"offset","type":"long"},
            {"name":"files","type":{"type":"array","items":"string"}}
          ]}}
        ]}
        """);
    var payload = new GenericData.Record(schema.getField("payload").schema());
    payload.put("offset", 1234567890123L);
    payload.put("files", java.util.List.of("данные.parquet"));
    var event = new GenericData.Record(schema);
    event.put("group_id", "test-connect-group");
    event.put("payload", payload);
    var bytes = serializeIceberg(event);
    var result = avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE).deserialize(null, bytes);
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.JSON);
    assertJsonEquals(new String(AvroSchemaUtils.toJson(event), java.nio.charset.StandardCharsets.UTF_8),
        result.getResult());
    assertThatThrownBy(() -> avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, Arrays.copyOf(bytes, bytes.length - 1))).isInstanceOf(Exception.class);
    assertThatThrownBy(() -> avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, Arrays.copyOf(bytes, bytes.length + 1))).isInstanceOf(IOException.class);
  }

  @Test
  void rendersLogicalUuidsWithoutChangingOrdinaryFixedValues() throws Exception {
    var schema = new Schema.Parser().parse("""
        {"type":"record","name":"Event","fields":[
          {"name":"id","type":{"type":"fixed","name":"uuid","size":16,"logicalType":"uuid"}},
          {"name":"payload","type":{"type":"record","name":"Payload","fields":[
            {"name":"commit_id","type":"uuid"},
            {"name":"ids","type":{"type":"array","items":"uuid"}},
            {"name":"by_name","type":{"type":"map","values":"uuid"}},
            {"name":"optional_id","type":["null","uuid"]},
            {"name":"binary","type":{"type":"fixed","name":"raw","size":16}}
          ]}}
        ]}
        """);
    // Fixed, known bytes test byte order and leading zeroes independently of the decoder.
    byte[] bytes = java.util.HexFormat.of().parseHex("00112233445566778899aabbccddeeff");
    final String expected = "00112233-4455-6677-8899-aabbccddeeff";
    var uuid = new GenericData.Fixed(schema.getField("id").schema(), bytes);
    var payload = new GenericData.Record(schema.getField("payload").schema());
    payload.put("commit_id", uuid);
    payload.put("ids", java.util.List.of(uuid));
    payload.put("by_name", java.util.Map.of("entry", uuid));
    payload.put("optional_id", uuid);
    payload.put("binary", new GenericData.Fixed(payload.getSchema().getField("binary").schema(), bytes));
    var event = new GenericData.Record(schema);
    event.put("id", uuid);
    event.put("payload", payload);
    var result = avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, serializeIceberg(event));
    var json = new JsonMapper().readTree(result.getResult());
    assertThat(json.at("/id").asText()).isEqualTo(expected);
    assertThat(json.at("/payload/commit_id").asText()).isEqualTo(expected);
    assertThat(json.at("/payload/ids/0").asText()).isEqualTo(expected);
    assertThat(json.at("/payload/by_name/entry").asText()).isEqualTo(expected);
    assertThat(json.at("/payload/optional_id/uuid").asText()).isEqualTo(expected);
    assertThat(json.at("/payload/binary").asText().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
        .isEqualTo(bytes);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rendersUuidInNonNullRootUnion(boolean recordBranch) throws Exception {
    var uuidSchema = new Schema.Parser().parse("""
        {"type":"fixed","name":"uuid","size":16,"logicalType":"uuid"}
        """);
    var fixed = new GenericData.Fixed(uuidSchema,
        java.util.HexFormat.of().parseHex("00112233445566778899aabbccddeeff"));
    Schema branch = uuidSchema;
    Object datum = fixed;
    if (recordBranch) {
      branch = org.apache.avro.SchemaBuilder.record("Event").fields()
          .name("id").type(uuidSchema).noDefault().endRecord();
      var record = new GenericData.Record(branch);
      record.put("id", fixed);
      datum = record;
    }
    Schema schema = Schema.createUnion(Schema.create(Schema.Type.NULL), branch);
    var output = new ByteArrayOutputStream();
    var header = new DataOutputStream(output);
    header.write(new byte[] {(byte) 0xC2, 0x01});
    header.writeUTF(schema.toString());
    var encoder = EncoderFactory.get().binaryEncoder(output, null);
    new GenericDatumWriter<>(schema).write(datum, encoder);
    encoder.flush();
    var result = avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, output.toByteArray());
    var json = new JsonMapper().readTree(result.getResult());
    assertThat((recordBranch ? json.get("id") : json).asText())
        .isEqualTo("00112233-4455-6677-8899-aabbccddeeff");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"null\"", "[\"null\",\"string\"]"})
  void readsNullIcebergDatum(String schemaJson) throws Exception {
    var output = new ByteArrayOutputStream();
    var header = new DataOutputStream(output);
    header.write(new byte[] {(byte) 0xC2, 0x01});
    header.writeUTF(schemaJson);
    var encoder = EncoderFactory.get().binaryEncoder(output, null);
    new GenericDatumWriter<>(new Schema.Parser().parse(schemaJson)).write(null, encoder);
    encoder.flush();
    var result = avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, output.toByteArray());
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.JSON);
    assertThat(result.getResult()).isEqualTo("null");
  }

  @Test
  void rejectsTruncatedIcebergSchema() {
    assertThatThrownBy(() -> avroEmbeddedSerde.deserializer("control", Serde.Target.VALUE)
        .deserialize(null, new byte[] {(byte) 0xC2, 0x01, 0, 10, '{'}))
        .isInstanceOf(IOException.class);
  }

  // Match Iceberg's AvroEncoderUtil wire format, including Java modified UTF-8 framing.
  private byte[] serializeIceberg(GenericRecord record) throws IOException {
    var output = new ByteArrayOutputStream();
    var header = new DataOutputStream(output);
    header.write(new byte[] {(byte) 0xC2, 0x01});
    header.writeUTF(record.getSchema().toString());
    var encoder = EncoderFactory.get().binaryEncoder(output, null);
    new GenericDatumWriter<GenericRecord>(record.getSchema()).write(record, encoder);
    encoder.flush();
    return output.toByteArray();
  }

  private void assertJsonEquals(String expected, String actual) throws IOException {
    var mapper = new JsonMapper();
    assertThat(mapper.readTree(actual)).isEqualTo(mapper.readTree(expected));
  }

  private byte[] serializeAvroWithEmbeddedSchema(GenericRecord record) throws IOException {
    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>());
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      writer.create(record.getSchema(), baos);
      writer.append(record);
      writer.flush();
      return baos.toByteArray();
    }
  }

}
