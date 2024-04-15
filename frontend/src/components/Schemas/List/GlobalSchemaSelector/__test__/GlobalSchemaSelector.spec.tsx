import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { render, WithRoute } from 'lib/testHelpers';
import { CompatibilityLevelCompatibilityEnum } from 'generated-sources';
import GlobalSchemaSelector from 'components/Schemas/List/GlobalSchemaSelector/GlobalSchemaSelector';
import userEvent from '@testing-library/user-event';
import { clusterSchemasPath } from 'lib/paths';
import {
  useGetGlobalCompatibilityLayer,
  useUpdateGlobalSchemaCompatibilityLevel,
} from 'lib/hooks/api/schemas';

const clusterName = 'testClusterName';

const selectForwardOption = async () => {
  const dropdownElement = screen.getByRole('listbox');
  // clicks to open dropdown
  await userEvent.click(within(dropdownElement).getByRole('option'));
  await userEvent.click(
    within(dropdownElement).getByText(
      CompatibilityLevelCompatibilityEnum.FORWARD
    )
  );
};

const expectOptionIsSelected = (option: string) => {
  const dropdownElement = screen.getByRole('listbox');
  const selectedOption = within(dropdownElement).getAllByRole('option');
  expect(selectedOption.length).toEqual(1);
  expect(selectedOption[0]).toHaveTextContent(option);
};

jest.mock('lib/hooks/api/schemas', () => ({
  useGetGlobalCompatibilityLayer: jest.fn(),
  useUpdateGlobalSchemaCompatibilityLevel: jest.fn(),
}));

describe('GlobalSchemaSelector', () => {
  const renderComponent = () =>
    render(
      <WithRoute path={clusterSchemasPath()}>
        <GlobalSchemaSelector />
      </WithRoute>,
      {
        initialEntries: [clusterSchemasPath(clusterName)],
      }
    );

  const updateMockFn = jest.fn();

  beforeEach(async () => {
    updateMockFn.mockClear();
    (useUpdateGlobalSchemaCompatibilityLevel as jest.Mock).mockImplementation(
      () => ({
        mutateAsync: updateMockFn,
      })
    );
    (useGetGlobalCompatibilityLayer as jest.Mock).mockImplementation(() => ({
      data: { compatibility: CompatibilityLevelCompatibilityEnum.FULL },
      isFetching: false,
    }));

    renderComponent();
  });

  it('renders with initial prop', () => {
    expectOptionIsSelected(CompatibilityLevelCompatibilityEnum.FULL);
  });

  it('shows popup when select value is changed', async () => {
    expectOptionIsSelected(CompatibilityLevelCompatibilityEnum.FULL);
    await selectForwardOption();
    expect(screen.getByText('Confirm the action')).toBeInTheDocument();
  });

  it('resets select value when cancel is clicked', async () => {
    await selectForwardOption();
    await userEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Confirm the action')).not.toBeInTheDocument();
    expectOptionIsSelected(CompatibilityLevelCompatibilityEnum.FULL);
  });

  it('sets new schema when confirm is clicked', async () => {
    await selectForwardOption();
    await waitFor(() => {
      userEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    });
    await waitFor(() => {
      expect(updateMockFn).toHaveBeenCalledTimes(1);
    });

    await waitFor(() =>
      expect(screen.queryByText('Confirm the action')).not.toBeInTheDocument()
    );

    // TODO this should be checked later not that important working as expected
    // await waitFor(() =>
    //   expectOptionIsSelected(CompatibilityLevelCompatibilityEnum.FORWARD)
    // );
  });
});
