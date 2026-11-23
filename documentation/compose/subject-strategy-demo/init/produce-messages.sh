#!/bin/bash
set -e

echo "Waiting for Schema Registry to be ready..."
until curl -s $SCHEMA_REGISTRY_URL/subjects > /dev/null 2>&1; do
  echo "Schema Registry not ready yet, waiting..."
  sleep 2
done
echo "Schema Registry is ready!"

# Wait for topics to be created by init-topic container
echo "Waiting for topics to be available..."
sleep 5

#############################################
# TopicNameStrategy (default) - subject = topic-key / topic-value
#############################################
echo ""
echo "=== Producing messages to 'topic-strategy-topic' with TopicNameStrategy ==="
echo "Key Subject: topic-strategy-topic-key"
echo "Value Subject: topic-strategy-topic-value"

kafka-avro-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic topic-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-strategy.avsc)" \
  --property value.schema="$(cat /schemas/value-for-topic-strategy.avsc)" \
  --property parse.key=true \
  --property key.separator="|" <<EOF
{"id":"key-001"}|{"id":"topic-001","message":"Hello from TopicNameStrategy"}
{"id":"key-002"}|{"id":"topic-002","message":"This uses the default strategy"}
EOF

echo "TopicNameStrategy messages produced successfully!"

#############################################
# RecordNameStrategy - subject = fully qualified record name
#############################################
echo ""
echo "=== Producing messages to 'record-strategy-topic' with RecordNameStrategy ==="
echo "Key Subject: io.kafbat.test.UserKey"
echo "Value Subject: io.kafbat.test.UserRecord"

kafka-avro-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-record-strategy.avsc)" \
  --property value.schema="$(cat /schemas/value-for-record-strategy.avsc)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy <<EOF
{"userId":"user-001"}|{"userId":"user-001","name":"Alice","email":{"string":"alice@example.com"}}
{"userId":"user-002"}|{"userId":"user-002","name":"Bob","email":null}
{"userId":"user-003"}|{"userId":"user-003","name":"Charlie","email":{"string":"charlie@example.com"}}
EOF

echo "RecordNameStrategy messages produced successfully!"

#############################################
# TopicRecordNameStrategy - subject = topic-recordname
#############################################
echo ""
echo "=== Producing messages to 'topic-record-strategy-topic' with TopicRecordNameStrategy ==="
echo "Key Subject: topic-record-strategy-topic-io.kafbat.test.EventKey"
echo "Value Subject: topic-record-strategy-topic-io.kafbat.test.EventRecord"

kafka-avro-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic topic-record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-record-strategy.avsc)" \
  --property value.schema="$(cat /schemas/value-for-topic-record-strategy.avsc)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy <<EOF
{"eventId":"evt-001"}|{"eventId":"evt-001","eventType":"USER_CREATED","timestamp":1704067200000}
{"eventId":"evt-002"}|{"eventId":"evt-002","eventType":"USER_UPDATED","timestamp":1704153600000}
{"eventId":"evt-003"}|{"eventId":"evt-003","eventType":"USER_DELETED","timestamp":1704240000000}
EOF

echo "TopicRecordNameStrategy messages produced successfully!"

#############################################
# PROTOBUF - TopicNameStrategy (default)
#############################################
echo ""
echo "=== Producing Protobuf messages to 'proto-topic-strategy-topic' with TopicNameStrategy ==="
echo "Key Subject: proto-topic-strategy-topic-key"
echo "Value Subject: proto-topic-strategy-topic-value"

kafka-protobuf-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic proto-topic-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-strategy.proto)" \
  --property value.schema="$(cat /schemas/value-for-topic-strategy.proto)" \
  --property parse.key=true \
  --property key.separator="|" <<EOF
{"id":"key-001"}|{"id":"proto-001","message":"Hello from Protobuf TopicNameStrategy"}
{"id":"key-002"}|{"id":"proto-002","message":"Protobuf with default strategy"}
EOF

echo "Protobuf TopicNameStrategy messages produced successfully!"

#############################################
# PROTOBUF - RecordNameStrategy
#############################################
echo ""
echo "=== Producing Protobuf messages to 'proto-record-strategy-topic' with RecordNameStrategy ==="
echo "Key Subject: io.kafbat.test.ProtoUserKey"
echo "Value Subject: io.kafbat.test.ProtoUserRecord"

kafka-protobuf-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic proto-record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-record-strategy.proto)" \
  --property value.schema="$(cat /schemas/value-for-record-strategy.proto)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy <<EOF
{"userId":"user-001"}|{"userId":"user-001","name":"Alice","email":"alice@example.com"}
{"userId":"user-002"}|{"userId":"user-002","name":"Bob"}
{"userId":"user-003"}|{"userId":"user-003","name":"Charlie","email":"charlie@example.com"}
EOF

echo "Protobuf RecordNameStrategy messages produced successfully!"

#############################################
# PROTOBUF - TopicRecordNameStrategy
#############################################
echo ""
echo "=== Producing Protobuf messages to 'proto-topic-record-strategy-topic' with TopicRecordNameStrategy ==="
echo "Key Subject: proto-topic-record-strategy-topic-io.kafbat.test.ProtoEventKey"
echo "Value Subject: proto-topic-record-strategy-topic-io.kafbat.test.ProtoEventRecord"

kafka-protobuf-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic proto-topic-record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-record-strategy.proto)" \
  --property value.schema="$(cat /schemas/value-for-topic-record-strategy.proto)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy <<EOF
{"eventId":"evt-001"}|{"eventId":"evt-001","eventType":"USER_CREATED","timestamp":1704067200000}
{"eventId":"evt-002"}|{"eventId":"evt-002","eventType":"USER_UPDATED","timestamp":1704153600000}
{"eventId":"evt-003"}|{"eventId":"evt-003","eventType":"USER_DELETED","timestamp":1704240000000}
EOF

echo "Protobuf TopicRecordNameStrategy messages produced successfully!"

#############################################
# JSON SCHEMA - TopicNameStrategy (default)
#############################################
echo ""
echo "=== Producing JSON Schema messages to 'json-topic-strategy-topic' with TopicNameStrategy ==="
echo "Key Subject: json-topic-strategy-topic-key"
echo "Value Subject: json-topic-strategy-topic-value"

kafka-json-schema-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic json-topic-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-strategy.json)" \
  --property value.schema="$(cat /schemas/value-for-topic-strategy.json)" \
  --property parse.key=true \
  --property key.separator="|" <<EOF
{"id":"key-001"}|{"id":"json-001","message":"Hello from JSON Schema TopicNameStrategy"}
{"id":"key-002"}|{"id":"json-002","message":"JSON Schema with default strategy"}
EOF

echo "JSON Schema TopicNameStrategy messages produced successfully!"

#############################################
# JSON SCHEMA - RecordNameStrategy
#############################################
echo ""
echo "=== Producing JSON Schema messages to 'json-record-strategy-topic' with RecordNameStrategy ==="
echo "Key Subject: JsonUserKey"
echo "Value Subject: JsonUserRecord"

kafka-json-schema-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic json-record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-record-strategy.json)" \
  --property value.schema="$(cat /schemas/value-for-record-strategy.json)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy <<EOF
{"userId":"user-001"}|{"userId":"user-001","name":"Alice","email":"alice@example.com"}
{"userId":"user-002"}|{"userId":"user-002","name":"Bob","email":null}
{"userId":"user-003"}|{"userId":"user-003","name":"Charlie","email":"charlie@example.com"}
EOF

echo "JSON Schema RecordNameStrategy messages produced successfully!"

#############################################
# JSON SCHEMA - TopicRecordNameStrategy
#############################################
echo ""
echo "=== Producing JSON Schema messages to 'json-topic-record-strategy-topic' with TopicRecordNameStrategy ==="
echo "Key Subject: json-topic-record-strategy-topic-JsonEventKey"
echo "Value Subject: json-topic-record-strategy-topic-JsonEventRecord"

kafka-json-schema-console-producer \
  --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
  --topic json-topic-record-strategy-topic \
  --property schema.registry.url=$SCHEMA_REGISTRY_URL \
  --property key.schema="$(cat /schemas/key-for-topic-record-strategy.json)" \
  --property value.schema="$(cat /schemas/value-for-topic-record-strategy.json)" \
  --property parse.key=true \
  --property key.separator="|" \
  --property key.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy \
  --property value.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy <<EOF
{"eventId":"evt-001"}|{"eventId":"evt-001","eventType":"USER_CREATED","timestamp":1704067200000}
{"eventId":"evt-002"}|{"eventId":"evt-002","eventType":"USER_UPDATED","timestamp":1704153600000}
{"eventId":"evt-003"}|{"eventId":"evt-003","eventType":"USER_DELETED","timestamp":1704240000000}
EOF

echo "JSON Schema TopicRecordNameStrategy messages produced successfully!"

echo ""
echo "========================================"
echo "All messages produced successfully!"
echo "========================================"
echo ""
echo "Subjects registered in Schema Registry:"
echo ""
echo "=== AVRO ==="
echo "TopicNameStrategy (topic-strategy-topic):"
echo "  - Key:   topic-strategy-topic-key"
echo "  - Value: topic-strategy-topic-value"
echo ""
echo "RecordNameStrategy (record-strategy-topic):"
echo "  - Key:   io.kafbat.test.UserKey"
echo "  - Value: io.kafbat.test.UserRecord"
echo ""
echo "TopicRecordNameStrategy (topic-record-strategy-topic):"
echo "  - Key:   topic-record-strategy-topic-io.kafbat.test.EventKey"
echo "  - Value: topic-record-strategy-topic-io.kafbat.test.EventRecord"
echo ""
echo "=== PROTOBUF ==="
echo "TopicNameStrategy (proto-topic-strategy-topic):"
echo "  - Key:   proto-topic-strategy-topic-key"
echo "  - Value: proto-topic-strategy-topic-value"
echo ""
echo "RecordNameStrategy (proto-record-strategy-topic):"
echo "  - Key:   io.kafbat.test.ProtoUserKey"
echo "  - Value: io.kafbat.test.ProtoUserRecord"
echo ""
echo "TopicRecordNameStrategy (proto-topic-record-strategy-topic):"
echo "  - Key:   proto-topic-record-strategy-topic-io.kafbat.test.ProtoEventKey"
echo "  - Value: proto-topic-record-strategy-topic-io.kafbat.test.ProtoEventRecord"
echo ""
echo "=== JSON SCHEMA ==="
echo "TopicNameStrategy (json-topic-strategy-topic):"
echo "  - Key:   json-topic-strategy-topic-key"
echo "  - Value: json-topic-strategy-topic-value"
echo ""
echo "RecordNameStrategy (json-record-strategy-topic):"
echo "  - Key:   JsonUserKey"
echo "  - Value: JsonUserRecord"
echo ""
echo "TopicRecordNameStrategy (json-topic-record-strategy-topic):"
echo "  - Key:   json-topic-record-strategy-topic-JsonEventKey"
echo "  - Value: json-topic-record-strategy-topic-JsonEventRecord"
echo ""
echo "Init complete!"