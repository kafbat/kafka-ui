export function getAvroSchema(): string {
  return JSON.stringify({
    type: 'record',
    name: 'Student',
    namespace: 'DataFlair',
    fields: [
      { name: 'Name', type: 'string' },
      { name: 'Age', type: 'int' }
    ]
  }, null, 2);
}

export function getAvroUpdatedSchema(): string {
  return JSON.stringify({
    type: "record",
    name: "Message",
    namespace: "io.kafbat.ui",
    fields: [
      {
        name: "text",
        type: "string",
        default: null
      },
      {
        name: "value",
        type: "string",
        default: null
      }
    ]
  }, null, 2);
}

export function getJsonSchema(): string {
  return JSON.stringify({
    "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
    "connection.url": "jdbc:postgresql://postgres-db:5432/test",
    "connection.user": "dev_user",
    "connection.password": "12345",
    "topics": "topic_for_connector"
  }, null, 2);
}

export function getProtobufSchema(): string {
  return `
enum SchemaType {
  AVRO = 0;
  JSON = 1;
  PROTOBUF = 2;
}
`.trim();
}