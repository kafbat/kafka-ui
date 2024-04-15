import React from 'react';
import Edit from 'components/Schemas/Edit/Edit';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterSchemaEditPath } from 'lib/paths';
import { schemaVersion } from 'components/Schemas/Edit/__tests__/fixtures';
import { screen } from '@testing-library/dom';
import ClusterContext, {
  ContextProps,
  initialValue as contextInitialValue,
} from 'components/contexts/ClusterContext';
import { useGetLatestSchema } from 'lib/hooks/api/schemas';

const clusterName = 'testClusterName';

const renderComponent = (context: ContextProps = contextInitialValue) =>
  render(
    <WithRoute path={clusterSchemaEditPath()}>
      <ClusterContext.Provider value={context}>
        <Edit />
      </ClusterContext.Provider>
    </WithRoute>,
    {
      initialEntries: [
        clusterSchemaEditPath(clusterName, schemaVersion.subject),
      ],
    }
  );

jest.mock('lib/hooks/api/schemas', () => ({
  useGetLatestSchema: jest.fn(),
}));

const mockedUsedNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));

const FormCompText = 'FormCompText';

jest.mock('components/Schemas/Edit/Form', () => () => (
  <div>{FormCompText}</div>
));

describe('Edit', () => {
  beforeEach(() => {
    mockedUsedNavigate.mockClear();
  });

  describe('Fetch is Errored', () => {
    it('should navigate to the 404 page when fetch is not successful', () => {
      (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
        data: undefined,
        isFetching: false,
        isError: true,
      }));
      renderComponent();
      expect(mockedUsedNavigate).toHaveBeenCalledTimes(1);
    });
  });

  describe('fetch success', () => {
    it('renders component with schema info', async () => {
      (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
        data: schemaVersion,
        isFetching: false,
        isError: false,
      }));
      renderComponent();
      expect(screen.getByText(FormCompText)).toBeInTheDocument();
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });
  });

  describe('fetch is loading', () => {
    it('renders loader during', async () => {
      (useGetLatestSchema as jest.Mock).mockImplementation(() => ({
        data: undefined,
        isFetching: true,
        isError: false,
      }));
      renderComponent();
      expect(screen.queryByRole('progressbar')).toBeInTheDocument();
    });
  });
});
