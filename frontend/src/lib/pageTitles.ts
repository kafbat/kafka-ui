import {
  clusterConnectConnectorConfigPath,
  clusterConnectConnectorPath,
  clusterConnectConnectorTopicsPath,
  clusterConnectConnectorTasksPath,
  clusterKsqlDbPath,
  clusterKsqlDbQueryPath,
  clusterKsqlDbStreamsPath,
  clusterKsqlDbTablesPath,
  clusterTopicAclsRelativePath,
  clusterTopicConnectorsRelativePath,
  clusterTopicConsumerGroupsPath,
  clusterTopicEditPath,
  clusterTopicMessagesPath,
  clusterTopicPath,
  clusterTopicSettingsPath,
  clusterTopicStatisticsPath,
  kafkaConnectClustersRelativePath,
} from 'lib/paths';

const APP_NAME = 'Kafbat UI';

export const buildPageTitle = (...parts: Array<string | undefined | null>) =>
  [...parts.filter((part): part is string => !!part?.trim()), APP_NAME].join(
    ' | '
  );

export const getKafkaConnectPageTitle = (
  currentPath: string | undefined,
  clusterName: string
) => {
  if (currentPath === kafkaConnectClustersRelativePath) {
    return buildPageTitle('Clusters', 'Kafka Connect', clusterName);
  }

  return buildPageTitle('Connectors', 'Kafka Connect', clusterName);
};

export const getKsqlDbPageTitle = (pathname: string, clusterName: string) => {
  if (pathname === clusterKsqlDbQueryPath(clusterName)) {
    return buildPageTitle('Query', 'KSQL DB', clusterName);
  }

  if (pathname === clusterKsqlDbStreamsPath(clusterName)) {
    return buildPageTitle('Streams', 'KSQL DB', clusterName);
  }

  if (
    pathname === clusterKsqlDbTablesPath(clusterName) ||
    pathname === clusterKsqlDbPath(clusterName)
  ) {
    return buildPageTitle('Tables', 'KSQL DB', clusterName);
  }

  return buildPageTitle('Tables', 'KSQL DB', clusterName);
};

export const getConnectDetailsPageTitle = (
  pathname: string,
  clusterName: string,
  connectName: string,
  connectorName: string
) => {
  if (
    pathname ===
    clusterConnectConnectorConfigPath(clusterName, connectName, connectorName)
  ) {
    return buildPageTitle('Config', connectorName, clusterName);
  }

  if (
    pathname ===
    clusterConnectConnectorTopicsPath(clusterName, connectName, connectorName)
  ) {
    return buildPageTitle('Topics', connectorName, clusterName);
  }

  if (
    pathname ===
      clusterConnectConnectorTasksPath(
        clusterName,
        connectName,
        connectorName
      ) ||
    pathname ===
      clusterConnectConnectorPath(clusterName, connectName, connectorName)
  ) {
    return buildPageTitle('Tasks', connectorName, clusterName);
  }

  return buildPageTitle(connectorName, clusterName);
};

export const getTopicPageTitle = (
  pathname: string,
  clusterName: string,
  topicName: string
) => {
  if (pathname === clusterTopicMessagesPath(clusterName, topicName)) {
    return buildPageTitle('Messages', topicName, clusterName);
  }

  if (pathname === clusterTopicSettingsPath(clusterName, topicName)) {
    return buildPageTitle('Settings', topicName, clusterName);
  }

  if (pathname === clusterTopicConsumerGroupsPath(clusterName, topicName)) {
    return buildPageTitle('Consumers', topicName, clusterName);
  }

  if (pathname === clusterTopicStatisticsPath(clusterName, topicName)) {
    return buildPageTitle('Statistics', topicName, clusterName);
  }

  if (
    pathname ===
    `${clusterTopicPath(clusterName, topicName)}/${clusterTopicAclsRelativePath}`
  ) {
    return buildPageTitle('ACLs', topicName, clusterName);
  }

  if (
    pathname ===
    `${clusterTopicPath(clusterName, topicName)}/${clusterTopicConnectorsRelativePath}`
  ) {
    return buildPageTitle('Connectors', topicName, clusterName);
  }

  if (pathname === clusterTopicEditPath(clusterName, topicName)) {
    return buildPageTitle('Edit', topicName, clusterName);
  }

  return buildPageTitle(topicName, clusterName);
};
