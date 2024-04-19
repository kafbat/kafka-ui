import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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

export function useGetLatestSchema(param: GetLatestSchemaRequest) {
  return useQuery<SchemaSubject>({
    queryKey: [
      SCHEMA_QUERY_KEY,
      LATEST_SCHEMA_QUERY_KEY,
      param.clusterName,
      param.subject,
    ],
    queryFn: () => schemasApiClient.getLatestSchema(param),
  });
}

export function useGetSchemas({
  clusterName,
  page,
  perPage,
  search,
}: GetSchemasRequest) {
  return useQuery<SchemaSubjectsResponse>({
    queryKey: [SCHEMA_QUERY_KEY, clusterName, page, perPage, search],
    queryFn: () =>
      schemasApiClient.getSchemas({
        clusterName,
        page,
        perPage,
        search: search || undefined,
      }),
  });
}

export function useGetSchemasVersions({
  clusterName,
  subject,
}: GetAllVersionsBySubjectRequest) {
  return useQuery<Array<SchemaSubject>>({
    queryKey: [SCHEMAS_VERSION_QUERY_KEY, clusterName, subject],
    queryFn: () =>
      schemasApiClient.getAllVersionsBySubject({
        clusterName,
        subject,
      }),
  });
}

export function useGetGlobalCompatibilityLayer(clusterName: ClusterName) {
  return useQuery<CompatibilityLevel>({
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
        queryClient.invalidateQueries([
          GLOBAL_COMPATIBILITY_SCHEMAS_QUERY_KEY,
          clusterName,
        ]),
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
