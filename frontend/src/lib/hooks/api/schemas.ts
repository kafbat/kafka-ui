import {
  useMutation,
  useQuery,
  useQueryClient,
  UseQueryOptions,
  useSuspenseQuery,
} from '@tanstack/react-query';
import {
  GLOBAL_COMPATIBILITY_SCHEMAS_QUERY_KEY,
  LATEST_SCHEMA_QUERY_KEY,
  SCHEMA_QUERY_KEY,
  SCHEMAS_VERSION_QUERY_KEY,
} from 'lib/queries';
import {
  CompatibilityLevel,
  GetAllVersionsBySubjectRequest,
  GetLatestSchemaRequest,
  GetSchemasRequest,
  NewSchemaSubject,
  SchemaSubject,
  SchemaSubjectsResponse,
  UpdateGlobalSchemaCompatibilityLevelRequest,
  UpdateSchemaCompatibilityLevelRequest,
} from 'generated-sources';
import { schemasApiClient } from 'lib/api';
import { ClusterName } from 'lib/interfaces/cluster';

export function useGetLatestSchema(
  param: GetLatestSchemaRequest,
  options?: Omit<UseQueryOptions<SchemaSubject>, 'queryKey' | 'queryFn'>
) {
  return useSuspenseQuery<SchemaSubject>({
    queryKey: [
      SCHEMA_QUERY_KEY,
      LATEST_SCHEMA_QUERY_KEY,
      param.clusterName,
      param.subject,
    ],
    queryFn: () => schemasApiClient.getLatestSchema(param),
    ...options,
  });
}

export function useGetSchemas({
  clusterName,
  page,
  perPage,
  search,
  orderBy,
  sortOrder,
  fts,
}: GetSchemasRequest) {
  return useQuery<SchemaSubjectsResponse>({
    queryKey: [
      SCHEMA_QUERY_KEY,
      clusterName,
      page,
      perPage,
      search,
      sortOrder,
      orderBy,
      fts,
    ],
    placeholderData: (previousData) => previousData,
    queryFn: () =>
      schemasApiClient.getSchemas({
        clusterName,
        page,
        perPage,
        search: search || undefined,
        sortOrder,
        orderBy,
        fts,
      }),
  });
}

export function useGetSchemasVersions({
  clusterName,
  subject,
}: GetAllVersionsBySubjectRequest) {
  return useSuspenseQuery<Array<SchemaSubject>>({
    queryKey: [SCHEMAS_VERSION_QUERY_KEY, clusterName, subject],
    queryFn: () =>
      schemasApiClient.getAllVersionsBySubject({
        clusterName,
        subject,
      }),
  });
}

export function useGetGlobalCompatibilityLayer(clusterName: ClusterName) {
  return useSuspenseQuery<CompatibilityLevel>({
    queryKey: [GLOBAL_COMPATIBILITY_SCHEMAS_QUERY_KEY, clusterName],
    queryFn: () =>
      schemasApiClient.getGlobalSchemaCompatibilityLevel({
        clusterName,
      }),
  });
}

export function useCreateSchema(clusterName: ClusterName) {
  const queryClient = useQueryClient();
  return useMutation<SchemaSubject, void, NewSchemaSubject>({
    mutationFn: ({ subject, schema, schemaType }) =>
      schemasApiClient.createNewSchema({
        clusterName,
        newSchemaSubject: { subject, schema, schemaType },
      }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[0] === SCHEMA_QUERY_KEY &&
          query.queryKey[1] === clusterName,
      });
    },
  });
}

export function useUpdateSchemaCompatibilityLayer({
  clusterName,
  subject,
}: {
  clusterName: ClusterName;
  subject: string;
}) {
  const queryClient = useQueryClient();
  return useMutation<
    void,
    void,
    Omit<UpdateSchemaCompatibilityLevelRequest, 'clusterName' | 'subject'>
  >({
    mutationFn: ({ compatibilityLevel }) =>
      schemasApiClient.updateSchemaCompatibilityLevel({
        clusterName,
        subject,
        compatibilityLevel,
      }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[0] === SCHEMA_QUERY_KEY &&
          query.queryKey[1] === clusterName,
      });
    },
  });
}

export function useUpdateGlobalSchemaCompatibilityLevel(
  clusterName: ClusterName
) {
  const queryClient = useQueryClient();

  return useMutation<
    void,
    void,
    Omit<UpdateGlobalSchemaCompatibilityLevelRequest, 'clusterName'>
  >({
    mutationFn: ({ compatibilityLevel }) =>
      schemasApiClient.updateGlobalSchemaCompatibilityLevel({
        clusterName,
        compatibilityLevel,
      }),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: [GLOBAL_COMPATIBILITY_SCHEMAS_QUERY_KEY, clusterName],
        }),
        queryClient.invalidateQueries({
          predicate: (query) =>
            query.queryKey[0] === SCHEMA_QUERY_KEY &&
            query.queryKey[1] === clusterName,
        }),
      ]);
    },
  });
}

export function useDeleteSchema({
  clusterName,
  subject,
}: {
  clusterName: ClusterName;
  subject: string;
}) {
  const queryClient = useQueryClient();

  return useMutation<void, unknown>({
    mutationFn: () =>
      schemasApiClient.deleteSchema({
        clusterName,
        subject,
      }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[0] === SCHEMA_QUERY_KEY &&
          query.queryKey[1] === clusterName,
      });
    },
  });
}
