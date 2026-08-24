export const getConnectorNameFromConsumerGroup = (
  consumerGroupId: string
): string | null => {
  const CONNECTOR_PREFIX = 'connect-';

  if (consumerGroupId && consumerGroupId.startsWith(CONNECTOR_PREFIX)) {
    return consumerGroupId.substring(CONNECTOR_PREFIX.length);
  }

  return null;
};
