import {
  expectQueryWorks,
  renderQueryHook,
  TestQueryClientProvider,
} from 'lib/testHelpers';
import fetchMock from 'fetch-mock';
import * as hooks from 'lib/hooks/api/schemas';
import { QUERY_REFETCH_LIMITED_OPTIONS } from 'lib/constants';
import { act } from 'react-dom/test-utils';
import { renderHook, waitFor } from '@testing-library/react';
import { CompatibilityLevelCompatibilityEnum } from 'generated-sources';
import { schemaVersion } from 'components/Schemas/Edit/__tests__/fixtures';
import {
  jsonSchema,
  versionPayload,
} from 'components/Schemas/Details/__test__/fixtures';
import { schemasPayload } from 'components/Schemas/List/__test__/fixtures';

const clusterName = 'test-cluster';
const { subject } = schemaVersion;

const schemasAPIUrl = `/api/clusters/${clusterName}/schemas`;
const schemasWithSubjectAPIUrl = `${schemasAPIUrl}/${subject}`;
const schemasAPILatestUrl = `${schemasWithSubjectAPIUrl}/latest`;
const schemasAPIVersionsUrl = `${schemasWithSubjectAPIUrl}/versions`;
const schemaCompatibilityUrl = `${schemasAPIUrl}/compatibility`;
const schemaCompatibilityWithSubjectUrl = `${schemasWithSubjectAPIUrl}/compatibility`;

describe('Schema hooks', () => {
  beforeEach(() => fetchMock.restore());

  describe('Get Queries', () => {
    describe('useGetSchemas', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.getOnce(schemasAPIUrl, schemasPayload);
        const { result } = renderQueryHook(() =>
          hooks.useGetSchemas({ clusterName })
        );
        await expectQueryWorks(mock, result);
      });
    });

    describe('useGetLatestSchema', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.getOnce(schemasAPILatestUrl, schemaVersion);
        const { result } = renderQueryHook(() =>
          hooks.useGetLatestSchema({ clusterName, subject })
        );
        await expectQueryWorks(mock, result);
      });
      it('returns the correct data with queryOptions', async () => {
        const mock = fetchMock.getOnce(schemasAPILatestUrl, schemaVersion);
        const { result } = renderQueryHook(() =>
          hooks.useGetLatestSchema(
            { clusterName, subject },
            QUERY_REFETCH_LIMITED_OPTIONS
          )
        );
        await expectQueryWorks(mock, result);
      });
    });

    describe('useGetSchemasVersions', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.getOnce(schemasAPIVersionsUrl, versionPayload);
        const { result } = renderQueryHook(() =>
          hooks.useGetSchemasVersions({ clusterName, subject })
        );
        await expectQueryWorks(mock, result);
      });
    });

    describe('useGetGlobalCompatibilityLayer', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.getOnce(schemaCompatibilityUrl, {
          compatibility: CompatibilityLevelCompatibilityEnum.FULL,
        });
        const { result } = renderQueryHook(() =>
          hooks.useGetGlobalCompatibilityLayer(clusterName)
        );
        await expectQueryWorks(mock, result);
      });
    });
  });

  describe('Mutations', () => {
    describe('useCreateSchema', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.postOnce(schemasAPIUrl, jsonSchema);
        const { result } = renderHook(
          () => hooks.useCreateSchema(clusterName),
          { wrapper: TestQueryClientProvider }
        );
        await act(async () => {
          await result.current.mutateAsync(jsonSchema);
        });
        await waitFor(() => expect(result.current.isSuccess).toBeTruthy());
        expect(mock.calls()).toHaveLength(1);
      });
    });

    describe('useUpdateSchemaCompatibilityLayer', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.putOnce(schemaCompatibilityWithSubjectUrl, 200);
        const { result } = renderHook(
          () =>
            hooks.useUpdateSchemaCompatibilityLayer({ clusterName, subject }),
          { wrapper: TestQueryClientProvider }
        );
        await act(async () => {
          await result.current.mutateAsync({
            compatibilityLevel: {
              compatibility: CompatibilityLevelCompatibilityEnum.BACKWARD,
            },
          });
        });
        await waitFor(() => expect(result.current.isSuccess).toBeTruthy());
        expect(mock.calls()).toHaveLength(1);
      });
    });

    describe('useUpdateGlobalSchemaCompatibilityLevel', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.putOnce(schemaCompatibilityUrl, {
          body: {
            compatibility: CompatibilityLevelCompatibilityEnum.BACKWARD,
          },
        });
        const { result } = renderHook(
          () => hooks.useUpdateGlobalSchemaCompatibilityLevel(clusterName),
          { wrapper: TestQueryClientProvider }
        );

        await act(() =>
          result.current.mutateAsync({
            compatibilityLevel: {
              compatibility: CompatibilityLevelCompatibilityEnum.BACKWARD,
            },
          })
        );
        await waitFor(() => expect(result.current.isSuccess).toBeTruthy());
        expect(mock.calls()).toHaveLength(1);
      });
    });

    describe('useDeleteSchema', () => {
      it('returns the correct data', async () => {
        const mock = fetchMock.deleteOnce(schemasWithSubjectAPIUrl, 200);
        const { result } = renderHook(
          () => hooks.useDeleteSchema({ clusterName, subject }),
          { wrapper: TestQueryClientProvider }
        );
        await act(() => result.current.mutateAsync());
        await waitFor(() => expect(result.current.isSuccess).toBeTruthy());
        expect(mock.calls()).toHaveLength(1);
      });
    });
  });
});
