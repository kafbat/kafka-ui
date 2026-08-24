import React from 'react';
import List from 'components/Schemas/List/List';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterSchemaPath, clusterSchemasPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import {
  schemaVersion1,
  schemaVersion2,
} from 'components/Schemas/Edit/__tests__/fixtures';
import ClusterContext, {
  ContextProps,
  initialValue as contextInitialValue,
} from 'components/contexts/ClusterContext';
import userEvent from '@testing-library/user-event';
import { useGetSchemas } from 'lib/hooks/api/schemas';

import { schemasPayload, schemasEmptyPayload } from './fixtures';

const mockedUsedNavigate = jest.fn();

const GlobalSchemaSelectorText = 'GlobalSchemaSelectorText';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));

jest.mock('lib/hooks/api/schemas', () => ({
  useGetSchemas: jest.fn(),
}));

jest.mock(
  'components/Schemas/List/GlobalSchemaSelector/GlobalSchemaSelector',
  () => () => <div>{GlobalSchemaSelectorText}</div>
);

const clusterName = 'testClusterName';
const renderComponent = (context: ContextProps = contextInitialValue) =>
  render(
    <WithRoute path={clusterSchemasPath()}>
      <ClusterContext.Provider value={context}>
        <List />
      </ClusterContext.Provider>
    </WithRoute>,
    {
      initialEntries: [clusterSchemasPath(clusterName)],
    }
  );

describe('List', () => {
  describe('fetch error', () => {
    it('shows progressbar', async () => {
      (useGetSchemas as jest.Mock).mockImplementation(() => ({
        data: {},
        isLoading: true,
        isSuccess: false,
      }));
      renderComponent();
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('fetch success', () => {
    describe('responded without schemas', () => {
      beforeEach(async () => {
        (useGetSchemas as jest.Mock).mockImplementation(() => ({
          data: schemasEmptyPayload,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        renderComponent();
      });
      it('renders empty table', () => {
        expect(screen.getByText('No schemas found')).toBeInTheDocument();
      });
    });
    describe('responded with schemas', () => {
      beforeEach(async () => {
        (useGetSchemas as jest.Mock).mockImplementation(() => ({
          data: schemasPayload,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        renderComponent();
      });
      it('renders list', () => {
        expect(screen.getByText(schemaVersion1.subject)).toBeInTheDocument();
        expect(screen.getByText(schemaVersion2.subject)).toBeInTheDocument();
      });
      it('handles onRowClick', async () => {
        const { id, schemaType, subject, version, compatibilityLevel } =
          schemaVersion2;
        const row = screen.getByRole('row', {
          name: `${subject} ${id} ${schemaType} ${version} ${compatibilityLevel}`,
        });
        expect(row).toBeInTheDocument();
        await userEvent.click(row);
        expect(mockedUsedNavigate).toHaveBeenCalledWith(
          clusterSchemaPath(clusterName, subject)
        );
      });
    });

    describe('responded with readonly cluster schemas', () => {
      beforeEach(async () => {
        (useGetSchemas as jest.Mock).mockImplementation(() => ({
          data: schemasPayload,
          isFetching: false,
          isError: false,
          isSuccess: true,
        }));
        renderComponent({
          ...contextInitialValue,
          isReadOnly: true,
        });
      });
      it('does not render Create Schema button', () => {
        expect(screen.queryByText('Create Schema')).not.toBeInTheDocument();
      });
    });
  });

  describe('check the compatibility layer', () => {
    it('should check if the compatibility layer component is being shown', () => {
      (useGetSchemas as jest.Mock).mockImplementation(() => ({
        data: {},
        isError: false,
        isFetching: false,
        isSuccess: true,
      }));
      renderComponent();
      expect(screen.getByText(GlobalSchemaSelectorText)).toBeInTheDocument();
    });
  });
});
