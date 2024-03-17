import React from 'react';
import SavedFilters, {
  Props,
} from 'components/Topics/Topic/Messages/Filters/SavedFilters';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import { AdvancedFiltersType } from 'lib/hooks/useMessageFiltersStore';

const mockDeleteIcon = 'mock-DeleteIcon';
const mockEditIcon = 'mock-EditIcon';

jest.mock('components/common/Icons/EditIcon', () => () => (
  <div>{mockEditIcon}</div>
));

jest.mock('components/common/Icons/DeleteIcon', () => () => (
  <div>{mockDeleteIcon}</div>
));

describe('SavedFilter Component', () => {
  const id1 = 'id1';
  const id2 = 'id2';

  const mockData: AdvancedFiltersType = {
    [id1]: {
      id: id1,
      value: 'record.partition == 1',
      filterCode: 'code',
    },
    [id2]: {
      id: id2,
      value: 'record.partition == 1',
      filterCode: 'code1',
    },
  };

  const setUpComponent = (props: Partial<Props> = {}) =>
    render(
      <SavedFilters
        filters={mockData}
        onEdit={jest.fn()}
        closeSideBar={jest.fn()}
        setSmartFilter={jest.fn()}
        {...props}
      />
    );

  const getSavedFilters = () => screen.getAllByRole('savedFilter');

  describe('Empty Filters Rendering', () => {
    beforeEach(() => {
      setUpComponent({ filters: {} });
    });

    it('should check the rendering of the empty filter', () => {
      expect(screen.getByText('No saved filter(s)')).toBeInTheDocument();

      expect(screen.getByText(/clear all/i)).toBeDisabled();
    });
  });

  describe('Saved Filters Deleting Editing', () => {
    const onEditMock = jest.fn();
    const cancelMock = jest.fn();
    const setSmartFilterMock = jest.fn();

    beforeEach(() => {
      setUpComponent({
        onEdit: onEditMock,
        closeSideBar: cancelMock,
        setSmartFilter: setSmartFilterMock,
      });
    });

    afterEach(() => {
      onEditMock.mockClear();
      cancelMock.mockClear();
      setSmartFilterMock.mockClear();
    });

    it('should check the normal data rendering', () => {
      const mockFilters = Object.values(mockData);
      mockFilters.forEach(({ id }) => {
        expect(screen.getByText(id)).toBeInTheDocument();
      });
    });

    it('should check the Filter edit Button works', async () => {
      const filters = getSavedFilters();
      await userEvent.hover(filters[0]);
      await userEvent.click(within(filters[0]).getByText(mockEditIcon));

      await userEvent.hover(filters[1]);
      await userEvent.click(within(filters[1]).getByText(mockEditIcon));

      expect(onEditMock).toBeCalledTimes(2);
    });

    it('should check the select filter', async () => {
      const savedFilterElement = getSavedFilters();
      await userEvent.click(savedFilterElement[0]);

      expect(setSmartFilterMock).toHaveBeenCalled();
      expect(cancelMock).toHaveBeenCalled();
    });
  });

  describe('Saved Filters Deletion', () => {
    const setSmartFilterMock = jest.fn();

    beforeEach(async () => {
      setUpComponent({ setSmartFilter: setSmartFilterMock });
      const filters = getSavedFilters();
      await userEvent.hover(filters[0]);
      await userEvent.click(within(filters[0]).getByText(mockDeleteIcon));
    });

    afterEach(() => {
      setSmartFilterMock.mockClear();
    });

    it('Open Confirmation for the deletion modal', async () => {
      const modelDialog = screen.getByRole('dialog');
      expect(modelDialog).toBeInTheDocument();
      expect(
        within(modelDialog).getByText(`Are you sure want to remove ${id1}?`)
      ).toBeInTheDocument();
    });

    it('Close Confirmations deletion modal with button', async () => {
      const modelDialog = screen.getByRole('dialog');
      const cancelButton = within(modelDialog).getByRole('button', {
        name: /Cancel/i,
      });
      await waitFor(() => userEvent.click(cancelButton));
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('Delete the saved filter', async () => {
      await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));
      expect(setSmartFilterMock).toHaveBeenCalledTimes(1);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
