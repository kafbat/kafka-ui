import { appConfigApiClient as api } from 'lib/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ApplicationConfig,
  ApplicationConfigPropertiesKafkaClusters,
} from 'generated-sources';
import { QUERY_REFETCH_OFF_OPTIONS } from 'lib/constants';

export function useAuthSettings() {
  return useQuery(
    ['app', 'authSettings'],
    () => api.getAuthenticationSettings(),
    QUERY_REFETCH_OFF_OPTIONS
  );
}

export function useAppInfo() {
  return useQuery(
    ['app', 'info'],
    () => api.getApplicationInfo(),
    QUERY_REFETCH_OFF_OPTIONS
  );
}

export function useAppConfig() {
  return useQuery(['app', 'config'], () => api.getCurrentConfig());
}

function aggregateClusters(
  cluster: ApplicationConfigPropertiesKafkaClusters,
  existingConfig: ApplicationConfig,
  initialName?: string,
  deleteCluster?: boolean
): ApplicationConfigPropertiesKafkaClusters[] {
  const existingClusters = existingConfig.properties?.kafka?.clusters || [];

  if (!initialName) {
    return [...existingClusters, cluster];
  }

  if (!deleteCluster) {
    return existingClusters.map((c) => (c.name === initialName ? cluster : c));
  }

  return existingClusters.filter((c) => c.name !== initialName);
}

export function useUpdateAppConfig({
  initialName,
  deleteCluster,
}: {
  initialName?: string;
  deleteCluster?: boolean;
}) {
  const client = useQueryClient();
  return useMutation(
    async (cluster: ApplicationConfigPropertiesKafkaClusters) => {
      const existingConfig = await api.getCurrentConfig();

      const clusters = aggregateClusters(
        cluster,
        existingConfig,
        initialName,
        deleteCluster
      );

      const config = {
        ...existingConfig,
        properties: {
          ...existingConfig.properties,
          kafka: { clusters },
        },
      };
      return api.restartWithConfig({ restartRequest: { config } });
    },
    {
      onSuccess: () => client.invalidateQueries(['app', 'config']),
    }
  );
}

export function useAppConfigFilesUpload() {
  return useMutation((payload: FormData) =>
    fetch('/api/config/relatedfiles', {
      method: 'POST',
      body: payload,
    }).then((res) => res.json())
  );
}

export function useValidateAppConfig() {
  return useMutation((config: ApplicationConfigPropertiesKafkaClusters) =>
    api.validateConfig({
      applicationConfig: { properties: { kafka: { clusters: [config] } } },
    })
  );
}
