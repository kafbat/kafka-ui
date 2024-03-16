import React from 'react';
import FiltersSideBar, {
  FilterModalProps,
} from 'components/Topics/Topic/Messages/Filters/FiltersSideBar';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import { ADD_FILTER_ID } from 'components/Topics/Topic/Messages/Filters/utils';
import userEvent from '@testing-library/user-event';

const AddEditComponentMock = 'AddEditComponentMock';
const SavedFilterComponentMock = 'SavedFilterComponentMock';

jest.mock(
  'components/Topics/Topic/Messages/Filters/AddEditFilterContainer',
  () => () => <div>{AddEditComponentMock}</div>
);

jest.mock('components/Topics/Topic/Messages/Filters/SavedFilters', () => () => (
  <div>{SavedFilterComponentMock}</div>
));

const renderComponent = (props?: Partial<FilterModalProps>) =>
  render(
    <FiltersSideBar
      filterName={props?.filterName || ADD_FILTER_ID}
      setClose={jest.fn()}
      setFilterName={jest.fn()}
      setSmartFilter={jest.fn()}
      {...props}
    />
  );

describe('FiltersSidebar component', () => {
  it('renders component with add filter modal', () => {
    renderComponent();
    expect(screen.getByText(/add filter/i)).toBeInTheDocument();
    expect(screen.getByText(AddEditComponentMock)).toBeInTheDocument();
    expect(screen.getByText(SavedFilterComponentMock)).toBeInTheDocument();
  });

  it('renders component with edit filter modal', () => {
    renderComponent({ filterName: 'filterName' });

    expect(screen.getByText(/edit filter/i)).toBeInTheDocument();
    expect(screen.getByText(AddEditComponentMock)).toBeInTheDocument();
    expect(
      screen.queryByText(SavedFilterComponentMock)
    ).not.toBeInTheDocument();
  });

  it('renders component with edit filter modal', async () => {
    const cancelMock = jest.fn();
    renderComponent({ setClose: cancelMock });
    await userEvent.click(
      screen.getByRole('button', {
        name: /edit/i,
      })
    );

    expect(cancelMock).toHaveBeenCalled();
  });
});
