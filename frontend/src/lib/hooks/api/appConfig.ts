import {
  appConfigApiClient as appConfig,
  internalApiClient as internalApi,
} from 'lib/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ApplicationConfig,
  ApplicationConfigPropertiesKafkaClusters,
  ApplicationInfo,
} from 'generated-sources';
import { QUERY_REFETCH_OFF_OPTIONS } from 'lib/constants';

export function useAuthSettings() {
  return useQuery(
    ['app', 'authSettings'],
    () => appConfig.getAuthenticationSettings(),
    QUERY_REFETCH_OFF_OPTIONS
  );
}

export function useAuthenticate() {
  return useMutation({
    mutationFn: (params: { username: string; password: string }) =>
      internalApi.authenticateRaw(params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      }),
  });
}

export function useAppInfo() {
  return useQuery(
    ['app', 'info'],
    async () => {
      const data = await appConfig.getApplicationInfoRaw()

      let response: ApplicationInfo = {}
      try {
        response = await data.value()
      } catch {
        response = {}
      }

      return {
        redirect: data.raw.url.includes('auth'),
        response,
      }
    },
  );
}

export function useAppConfig() {
  return useQuery(['app', 'config'], () => appConfig.getCurrentConfig());
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
      const existingConfig = await appConfig.getCurrentConfig();

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
      return appConfig.restartWithConfig({ restartRequest: { config } });
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
    appConfig.validateConfig({
      applicationConfig: { properties: { kafka: { clusters: [config] } } },
    })
  );
}
