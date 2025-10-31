import { aclApiClient as api } from 'lib/api';
import {
  QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';
import { showSuccessAlert } from 'lib/errorHandling';
import {
  CreateConsumerAcl,
  CreateProducerAcl,
  CreateStreamAppAcl,
  KafkaAcl,
} from 'generated-sources';

export function useAcls({
  clusterName,
  search,
  fts,
}: {
  clusterName: ClusterName;
  search?: string;
  fts?: boolean;
}) {
  return useQuery(
    ['clusters', clusterName, 'acls', { search, fts }],
    () =>
      api.listAcls({
        clusterName,
        search,
        fts,
      }),
    {
      keepPreviousData: true,
      suspense: false,
    }
  );
}

const onCreateAclSuccess = (queryClient: QueryClient, clusterName: string) => {
  showSuccessAlert({
    message: 'Your ACL was created successfully',
  });
  queryClient.invalidateQueries(['clusters', clusterName, 'acls']);
};

export function useCreateCustomAcl(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  const mutate = useMutation(
    (kafkaAcl: KafkaAcl) =>
      api.createAcl({
        clusterName,
        kafkaAcl,
      }),
    {
      onSuccess() {
        onCreateAclSuccess(queryClient, clusterName);
      },
    }
  );

  return {
    createResource: async (acl: KafkaAcl) => {
      return mutate.mutateAsync(acl);
    },
    ...mutate,
  };
}

export function useCreateConsumersAcl(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  const mutate = useMutation(
    (createConsumerAcl: CreateConsumerAcl) =>
      api.createConsumerAcl({
        clusterName,
        createConsumerAcl,
      }),
    {
      onSuccess() {
        onCreateAclSuccess(queryClient, clusterName);
      },
    }
  );

  return {
    createResource: async (acl: CreateConsumerAcl) => {
      return mutate.mutateAsync(acl);
    },
    ...mutate,
  };
}

export function useCreateProducerAcl(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  const mutate = useMutation(
    (createProducerAcl: CreateProducerAcl) =>
      api.createProducerAcl({
        clusterName,
        createProducerAcl,
      }),
    {
      onSuccess() {
        onCreateAclSuccess(queryClient, clusterName);
      },
    }
  );

  return {
    createResource: async (acl: CreateProducerAcl) => {
      return mutate.mutateAsync(acl);
    },
    ...mutate,
  };
}

export function useCreateStreamAppAcl(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  const mutate = useMutation(
    (createStreamAppAcl: CreateStreamAppAcl) =>
      api.createStreamAppAcl({
        clusterName,
        createStreamAppAcl,
      }),
    {
      onSuccess() {
        onCreateAclSuccess(queryClient, clusterName);
      },
    }
  );

  return {
    createResource: async (acl: CreateStreamAppAcl) => {
      return mutate.mutateAsync(acl);
    },
    ...mutate,
  };
}

export function useDeleteAclMutation(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  return useMutation(
    (acl: KafkaAcl) => api.deleteAcl({ clusterName, kafkaAcl: acl }),
    {
      onSuccess: () => {
        showSuccessAlert({ message: 'ACL deleted' });
        queryClient.invalidateQueries(['clusters', clusterName, 'acls']);
      },
    }
  );
}

export function useDeleteAcl(clusterName: ClusterName) {
  const mutate = useDeleteAclMutation(clusterName);

  return {
    deleteResource: async (param: KafkaAcl) => {
      return mutate.mutateAsync(param);
    },
    ...mutate,
  };
}
