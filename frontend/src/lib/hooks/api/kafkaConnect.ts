import {
  Connect,
  Connector,
  ConnectorAction,
  FullConnectorInfo,
  NewConnector,
  Task,
  Topic,
} from 'generated-sources';
import { kafkaConnectApiClient as api } from 'lib/api';
import {
  useMutation,
  useQuery,
  useQueryClient,
  UseQueryOptions,
} from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';
import { apiFetch, ServerResponse, showSuccessAlert } from 'lib/errorHandling';
import { topicKeys } from 'lib/hooks/api/topics';

interface UseConnectorProps {
  clusterName: ClusterName;
  topicName?: Topic['name'];
  connectName: Connect['name'];
  connectorName: Connector['name'];
}
interface CreateConnectorProps {
  connectName: Connect['name'];
  newConnector: NewConnector;
}

const connectsKey = (clusterName: ClusterName, withStats?: boolean) => [
  'clusters',
  clusterName,
  'connects',
  withStats,
];
const connectorsKey = (
  clusterName: ClusterName,
  search?: string,
  fts?: boolean
) => {
  let base: Array<string | { search: string } | { fts: boolean }> = [
    'clusters',
    clusterName,
    'connectors',
  ];
  if (search) {
    base = [...base, { search }];
  }

  if (fts) {
    base = [...base, { fts }];
  }

  return base;
};
const connectorKey = (props: UseConnectorProps) => [
  'clusters',
  props.clusterName,
  'connects',
  props.connectName,
  'connectors',
  props.connectorName,
];
const connectorTasksKey = (props: UseConnectorProps) => [
  ...connectorKey(props),
  'tasks',
];

export function useConnects(
  clusterName: ClusterName,
  withStats?: boolean,
  options?: Omit<
    UseQueryOptions<Connect[], ServerResponse>,
    'queryKey' | 'queryFn'
  >
) {
  return useQuery<Connect[], ServerResponse>({
    queryKey: connectsKey(clusterName, withStats),
    queryFn: () => apiFetch(() => api.getConnects({ clusterName, withStats })),
    ...options,
  });
}
export function useConnectors(
  clusterName: ClusterName,
  search?: string,
  fts?: boolean,
  options?: Omit<
    UseQueryOptions<FullConnectorInfo[], ServerResponse>,
    'queryKey' | 'queryFn'
  >
) {
  return useQuery<FullConnectorInfo[], ServerResponse>({
    queryKey: connectorsKey(clusterName, search, fts),
    queryFn: () =>
      apiFetch(() => api.getAllConnectors({ clusterName, search, fts })),
    placeholderData: (previousData) => previousData,
    select: (data) =>
      [...data].sort((a, b) => {
        if (a.name < b.name) {
          return -1;
        }
        if (a.name > b.name) {
          return 1;
        }
        return 0;
      }),
    ...options,
  });
}
export function useConnector(
  props: UseConnectorProps,
  options?: Omit<
    UseQueryOptions<Connector, ServerResponse>,
    'queryKey' | 'queryFn'
  >
) {
  return useQuery<Connector, ServerResponse>({
    queryKey: connectorKey(props),
    queryFn: () => apiFetch(() => api.getConnector(props)),
    ...options,
  });
}
export function useConnectorTasks(
  props: UseConnectorProps,
  options?: Omit<
    UseQueryOptions<Task[], ServerResponse>,
    'queryKey' | 'queryFn'
  >
) {
  return useQuery<Task[], ServerResponse>({
    queryKey: connectorTasksKey(props),
    queryFn: () => apiFetch(() => api.getConnectorTasks(props)),
    select: (data) =>
      [...data].sort((a, b) => {
        const aid = a.status.id;
        const bid = b.status.id;

        if (aid < bid) {
          return -1;
        }

        if (aid > bid) {
          return 1;
        }
        return 0;
      }),
    ...options,
  });
}
export function useUpdateConnectorState(props: UseConnectorProps) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (action: ConnectorAction) =>
      api.updateConnectorState({ ...props, action }),
    onSuccess: () =>
      Promise.all([
        client.invalidateQueries({
          queryKey: connectorsKey(props.clusterName),
        }),
        client.invalidateQueries({ queryKey: connectorKey(props) }),
        props.topicName &&
          client.invalidateQueries({
            queryKey: topicKeys.connectors({
              clusterName: props.clusterName,
              topicName: props.topicName,
            }),
          }),
      ]),
  });
}
export function useRestartConnectorTask(props: UseConnectorProps) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (taskId: number) =>
      api.restartConnectorTask({ ...props, taskId }),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: connectorTasksKey(props) }),
  });
}
export function useConnectorConfig(props: UseConnectorProps) {
  return useQuery({
    queryKey: [...connectorKey(props), 'config'],
    queryFn: () => api.getConnectorConfig(props),
  });
}
export function useUpdateConnectorConfig(props: UseConnectorProps) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (requestBody: Connector['config']) =>
      api.setConnectorConfig({ ...props, requestBody }),
    onSuccess: () => {
      showSuccessAlert({
        message: `Config successfully updated.`,
      });
      client.invalidateQueries({ queryKey: connectorKey(props) });
    },
  });
}
function useCreateConnectorMutation(clusterName: ClusterName) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (props: CreateConnectorProps) =>
      api.createConnector({ ...props, clusterName }),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: connectorsKey(clusterName) }),
  });
}

// this will change later when we validate the request before
export function useCreateConnector(clusterName: ClusterName) {
  const mutate = useCreateConnectorMutation(clusterName);

  return {
    createResource: async (param: CreateConnectorProps) => {
      return mutate.mutateAsync(param);
    },
    ...mutate,
  };
}

export function useDeleteConnector(props: UseConnectorProps) {
  const client = useQueryClient();

  return useMutation({
    mutationFn: () => api.deleteConnector(props),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: connectorsKey(props.clusterName) }),
  });
}

export function useResetConnectorOffsets(props: UseConnectorProps) {
  const client = useQueryClient();

  return useMutation({
    mutationFn: () => api.resetConnectorOffsets(props),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: connectorKey(props) }),
  });
}
