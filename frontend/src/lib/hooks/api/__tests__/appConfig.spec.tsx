import React from 'react';
import { waitFor } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import { ApplicationConfigPropertiesKafkaClusters } from 'generated-sources';
import { useUpdateAppConfig } from 'lib/hooks/api/appConfig';

const getCurrentConfigMock = jest.fn();
const restartWithConfigMock = jest.fn();

jest.mock('lib/api', () => ({
  appConfigApiClient: {
    getCurrentConfig: (...args: unknown[]) => getCurrentConfigMock(...args),
    restartWithConfig: (...args: unknown[]) => restartWithConfigMock(...args),
  },
  internalApiClient: {},
}));

const Probe = ({
  cluster,
}: {
  cluster: ApplicationConfigPropertiesKafkaClusters;
}) => {
  const mutation = useUpdateAppConfig({ initialName: 'cluster-a' });
  const startedRef = React.useRef(false);

  React.useEffect(() => {
    if (!startedRef.current) {
      startedRef.current = true;
      mutation.mutate(cluster);
    }
  }, [cluster, mutation]);

  return null;
};

describe('useUpdateAppConfig', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('preserves existing kafka fields when updating clusters', async () => {
    const existingConfig = {
      properties: {
        kafka: {
          polling: {
            pollTimeoutMs: 1000,
            maxPageSize: 500,
            maxMessagesToScanPerPoll: 500,
          },
          adminClientTimeout: 15000,
          internalTopicPrefix: '_internal',
          clusters: [
            {
              name: 'cluster-a',
              bootstrapServers: 'localhost:9092',
            },
          ],
        },
      },
    };

    getCurrentConfigMock.mockResolvedValue(existingConfig);
    restartWithConfigMock.mockResolvedValue({});

    const updatedCluster: ApplicationConfigPropertiesKafkaClusters = {
      name: 'cluster-a',
      bootstrapServers: 'localhost:19092',
    };

    render(<Probe cluster={updatedCluster} />);

    await waitFor(() => {
      expect(restartWithConfigMock).toHaveBeenCalledTimes(1);
    });

    const payload =
      restartWithConfigMock.mock.calls[0][0].restartRequest.config;
    expect(payload.properties.kafka.polling).toEqual(
      existingConfig.properties.kafka.polling
    );
    expect(payload.properties.kafka.adminClientTimeout).toBe(15000);
    expect(payload.properties.kafka.internalTopicPrefix).toBe('_internal');
    expect(payload.properties.kafka.clusters).toEqual([updatedCluster]);
  });
});
