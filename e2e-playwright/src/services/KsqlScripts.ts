
export function createStreamQuery(
  streamName: string,
  topicName: string,
  valueFormat: string = "json",
  partitions: number = 1
): string {
  return `CREATE STREAM ${streamName} (profileId VARCHAR, latitude DOUBLE, longitude DOUBLE ) WITH (kafka_topic='${topicName}', value_format='${valueFormat}', partitions=${partitions});`;
}

export function createTableQuery(tableName: string, streamName: string): string {
  return `CREATE TABLE ${tableName} AS SELECT profileId, LATEST_BY_OFFSET(latitude) AS la, LATEST_BY_OFFSET(longitude) AS lo FROM ${streamName} GROUP BY profileId EMIT CHANGES;`;
}