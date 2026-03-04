import React from 'react';
import Details from 'components/Schemas/Details/Details';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterSchemaPath } from 'lib/paths';
import { screen } from '@testing-library/dom';
import {
  schemaVersion,
  schemaVersionWithNonAsciiChars,
} from 'components/Schemas/Edit/__tests__/fixtures';
import ClusterContext, {
  ContextProps,
  initialValue as contextInitialValue,
} from 'components/contexts/ClusterContext';
import {
  useDeleteSchema,
  useGetLatestSchema,
  useGetSchemasVersions,
} from 'lib/hooks/api/schemas';

import { versionPayload, versionEmptyPayload } from './fixtures';

const clusterName = 'testClusterName';

const mockHistoryPush = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockHistoryPush,
}));

jest.mock('lib/hooks/api/schemas', () => ({
  useGetSchemasVersions: jest.fn(),
  useGetLatestSchema: jest.fn(),
  useDeleteSchema: jest.fn(),
}));

const renderComponent = (context: ContextProps = contextInitialValue) =>
  render(
    <WithRoute path={clusterSchemaPath()}>
      <ClusterContext.Provider value={context}>
        <Details />
      </ClusterContext.Provider>
    </WithRoute>,
    {
      initialEntries: [clusterSchemaPath(clusterName, schemaVersion.subject)],
    }
  );

describe('Details', () => {
  const deleteMockfn = jest.fn();
  beforeEach(() => {
    deleteMockfn.mockClear();

    // TODO test case should be added for this
    (useDeleteSchema as jest.Mock).mockImplementation(() => ({
      mutateAsync: deleteMockfn,
    }));
  });

  describe('fetch success', () => {
    describe('has schema versions', () => {
      it('renders component with schema info', async () => {
        (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
          data: versionPayload,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
          data: useGetSchemasVersions,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        renderComponent();
        expect(screen.getByText('Edit Schema')).toBeInTheDocument();
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
        expect(screen.getByRole('table')).toBeInTheDocument();
      });
    });

    describe('fetch success schema with non ascii characters', () => {
      describe('has schema versions', () => {
        it('renders component with schema info', async () => {
          (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
            data: versionPayload,
            isFetching: false,
            isError: false,
            isSuccess: true,
          }));
          (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
            data: schemaVersionWithNonAsciiChars,
            isFetching: false,
            isError: false,
            isSuccess: true,
          }));
          renderComponent();
          expect(screen.getByText('Edit Schema')).toBeInTheDocument();
          expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
          expect(screen.getByRole('table')).toBeInTheDocument();
        });
      });
    });

    describe('empty schema versions', () => {
      beforeEach(async () => {
        (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
          data: versionEmptyPayload,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
          data: schemaVersionWithNonAsciiChars,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        renderComponent();
      });

      // seems like incorrect behaviour
      it('renders versions table with 0 items', () => {
        expect(screen.getByRole('table')).toBeInTheDocument();
      });
    });
  });
});
