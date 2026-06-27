import {
  clusterConnectConnectorConfigPath,
  clusterConnectorsRelativePath,
  clusterKsqlDbQueryPath,
  clusterTopicMessagesPath,
} from 'lib/paths';
import {
  buildPageTitle,
  getConnectDetailsPageTitle,
  getKafkaConnectPageTitle,
  getKsqlDbPageTitle,
  getTopicPageTitle,
} from 'lib/pageTitles';

describe('pageTitles', () => {
  it('builds titles from non-empty parts', () => {
    expect(buildPageTitle('Messages', '', 'orders')).toBe(
      'Messages | orders | Kafbat UI'
    );
  });

  it('maps topic detail routes to section titles', () => {
    expect(
      getTopicPageTitle(
        clusterTopicMessagesPath('local', 'orders'),
        'local',
        'orders'
      )
    ).toBe('Messages | orders | local | Kafbat UI');
  });

  it('maps connector detail routes to section titles', () => {
    expect(
      getConnectDetailsPageTitle(
        clusterConnectConnectorConfigPath('local', 'main-connect', 'sink-a'),
        'local',
        'main-connect',
        'sink-a'
      )
    ).toBe('Config | sink-a | local | Kafbat UI');
  });

  it('maps kafka connect list routes to page titles', () => {
    expect(
      getKafkaConnectPageTitle(clusterConnectorsRelativePath, 'local')
    ).toBe('Connectors | Kafka Connect | local | Kafbat UI');
  });

  it('maps ksqldb routes to page titles', () => {
    expect(getKsqlDbPageTitle(clusterKsqlDbQueryPath('local'), 'local')).toBe(
      'Query | KSQL DB | local | Kafbat UI'
    );
  });
});
