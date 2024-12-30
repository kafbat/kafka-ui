import {
  Connect,
  Connector,
  ConnectorAction,
  NewConnector,
} from 'generated-sources';
import { kafkaConnectApiClient as api } from 'lib/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';
import { showSuccessAlert } from 'lib/errorHandling';

interface UseConnectorProps {
  clusterName: ClusterName;
  connectName: Connect['name'];
  connectorName: Connector['name'];
}
interface CreateConnectorProps {
  connectName: Connect['name'];
  newConnector: NewConnector;
}

const connectsKey = (clusterName: ClusterName) => [
  'clusters',
  clusterName,
  'connects',
];
const connectorsKey = (clusterName: ClusterName, search?: string) => {
  const base = ['clusters', clusterName, 'connectors'];
  if (search) {
    return [...base, { search }];
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

export function useConnects(clusterName: ClusterName) {
  return useQuery(connectsKey(clusterName), () =>
    api.getConnects({ clusterName })
  );
}
export function useConnectors(clusterName: ClusterName, search?: string) {
  return useQuery(
    connectorsKey(clusterName, search),
    () => api.getAllConnectors({ clusterName, search }),
    {
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
    }
  );
}
export function useConnector(props: UseConnectorProps) {
  return useQuery(connectorKey(props), () => api.getConnector(props));
}
export function useConnectorTasks(props: UseConnectorProps) {
  return useQuery(
    connectorTasksKey(props),
    () => api.getConnectorTasks(props),
    {
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
    }
  );
}
export function useUpdateConnectorState(clusterName: ClusterName) {
  const client = useQueryClient();

  return useMutation({
    mutationFn: ({
      props,
      action,
    }: {
      props: UseConnectorProps;
      action: ConnectorAction;
    }) => api.updateConnectorState({ ...props, action }),
    onSuccess: () =>
      client.invalidateQueries(['clusters', clusterName, 'connectors']),
  });
}
export function useRestartConnectorTask(props: UseConnectorProps) {
  const client = useQueryClient();
  return useMutation(
    (taskId: number) => api.restartConnectorTask({ ...props, taskId }),
    {
      onSuccess: () => client.invalidateQueries(connectorTasksKey(props)),
    }
  );
}
export function useConnectorConfig(props: UseConnectorProps) {
  return useQuery([...connectorKey(props), 'config'], () =>
    api.getConnectorConfig(props)
  );
}
export function useUpdateConnectorConfig(props: UseConnectorProps) {
  const client = useQueryClient();
  return useMutation(
    (requestBody: Connector['config']) =>
      api.setConnectorConfig({ ...props, requestBody }),
    {
      onSuccess: () => {
        showSuccessAlert({
          message: `Config successfully updated.`,
        });
        client.invalidateQueries(connectorKey(props));
      },
    }
  );
}
function useCreateConnectorMutation(clusterName: ClusterName) {
  const client = useQueryClient();
  return useMutation(
    (props: CreateConnectorProps) =>
      api.createConnector({ ...props, clusterName }),
    {
      onSuccess: () => client.invalidateQueries(connectorsKey(clusterName)),
    }
  );
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

export function useDeleteConnector(clusterName: ClusterName) {
  const client = useQueryClient();

  return useMutation({
    mutationFn: ({ props }: { props: UseConnectorProps }) =>
      api.deleteConnector(props),
    onSuccess: () => client.invalidateQueries(connectorsKey(clusterName)),
  });
}
