import {
  useMutation,
  useQuery,
  useQueryClient,
  UseQueryOptions,
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
import { apiFetch, ServerResponse } from 'lib/errorHandling';

export function useGetLatestSchema(
  param: GetLatestSchemaRequest,
  options?: Omit<UseQueryOptions<SchemaSubject, ServerResponse>, 'queryKey' | 'queryFn'>
) {
  return useQuery<SchemaSubject, ServerResponse>({
    queryKey: [
      SCHEMA_QUERY_KEY,
      LATEST_SCHEMA_QUERY_KEY,
      param.clusterName,
      param.subject,
    ],
    queryFn: () => apiFetch(() => schemasApiClient.getLatestSchema(param)),
    ...options,
  });
}

export function useGetSchemas(
  params: GetSchemasRequest,
  options?: Omit<UseQueryOptions<SchemaSubjectsResponse, ServerResponse>, 'queryKey' | 'queryFn'>
) {
  const { clusterName, page, perPage, search, orderBy, sortOrder, fts } = params;
  return useQuery<SchemaSubjectsResponse, ServerResponse>({
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
    queryFn: () => apiFetch(() => schemasApiClient.getSchemas({
      clusterName,
      page,
      perPage,
      search: search || undefined,
      sortOrder,
      orderBy,
      fts,
    })),
    ...options,
  });
}

export function useGetSchemasVersions(
  params: GetAllVersionsBySubjectRequest,
  options?: Omit<UseQueryOptions<Array<SchemaSubject>, ServerResponse>, 'queryKey' | 'queryFn'>
) {
  const { clusterName, subject } = params;
  return useQuery<Array<SchemaSubject>, ServerResponse>({
    queryKey: [SCHEMAS_VERSION_QUERY_KEY, clusterName, subject],
    queryFn: () =>
      apiFetch(() => schemasApiClient.getAllVersionsBySubject({
        clusterName,
        subject,
      })),
    ...options,
  });
}

export function useGetGlobalCompatibilityLayer(
  clusterName: ClusterName,
  options?: Omit<UseQueryOptions<CompatibilityLevel, ServerResponse>, 'queryKey' | 'queryFn'>
) {
  return useQuery<CompatibilityLevel, ServerResponse>({
    queryKey: [GLOBAL_COMPATIBILITY_SCHEMAS_QUERY_KEY, clusterName],
    queryFn: () =>
      apiFetch(() => schemasApiClient.getGlobalSchemaCompatibilityLevel({
        clusterName,
      })),
    ...options,
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
