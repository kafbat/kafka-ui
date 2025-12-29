import React from 'react';
import AddEditFilterContainer, {
  AddEditFilterContainerProps,
} from 'components/Topics/Topic/Messages/Filters/AddEditFilterContainer';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdvancedFilter } from 'lib/hooks/useMessageFiltersStore';
import { useRegisterSmartFilter } from 'lib/hooks/api/topicMessages';

jest.mock('lib/hooks/api/topicMessages', () => ({
  useRegisterSmartFilter: jest.fn(),
}));

describe('AddEditFilterContainer component', () => {
  const mockData: AdvancedFilter = {
    id: 'id',
    value: 'mockCode',
    filterCode: 'record.partition == 1',
  };

  const renderComponent = (
    props: Partial<AddEditFilterContainerProps> = {}
  ) => {
    return render(
      <AddEditFilterContainer
        setSmartFilter={props.setSmartFilter || jest.fn()}
        closeSideBar={props.closeSideBar || jest.fn()}
        {...props}
      />
    );
  };

  beforeEach(() => {
    (useRegisterSmartFilter as jest.Mock).mockImplementation(() => ({
      mutateAsync: () =>
        new Promise((res) => {
          res({ id: 'id1' });
        }),
    }));
  });

  describe('Add current Filter Configured', () => {
    beforeEach(() => {
      renderComponent();
    });

    it('should check the Button text during add', async () => {
      expect(screen.getByText(/add filter/i)).toBeInTheDocument();
    });

    it('should check whether the submit Button is disabled when the form is pristine and disabled if dirty', async () => {
      const submitButtonElem = screen.getByText(/add filter/i);
      expect(submitButtonElem).toBeDisabled();

      const inputs = screen.getAllByRole('textbox');

      const textAreaElement = inputs[0] as HTMLTextAreaElement;
      textAreaElement.focus();
      await userEvent.paste('Hello World With TextArea');

      const inputNameElement = inputs[1] as HTMLInputElement;
      await userEvent.type(inputNameElement, 'Hello World!');

      expect(submitButtonElem).toBeEnabled();
    });

    it('should display the CEL syntax help icon', async () => {
      const celHelpIcon = screen.queryByLabelText('info');
      expect(celHelpIcon).toBeVisible();
      expect(celHelpIcon).toBeEnabled();
    });

    // TODO needs a rethink
    // it('should view the error message after typing and clearing the input', async () => {
    //   const inputs = screen.getAllByRole('textbox');
    //   const textAreaElement = inputs[0] as HTMLTextAreaElement;
    //   const inputNameElement = inputs[1];
    //
    //   textAreaElement.focus();
    //   await userEvent.paste('Hello World With TextArea');
    //   await userEvent.type(inputNameElement, 'Hello World!');
    //
    //   await userEvent.tab();
    //
    //   act(() => {
    //     fireEvent.change(textAreaElement, { target: { value: '' } });
    //     textAreaElement.focus();
    //   });
    //
    //   expect(screen.getByText(/required field/i)).toBeInTheDocument();
    // });
  });

  describe('Edit current Filter Configured', () => {
    beforeEach(() => {
      renderComponent({ currentFilter: mockData });
    });

    it('should check the Button text during edit', async () => {
      expect(screen.getByText(/edit filter/i)).toBeInTheDocument();
    });

    it('should render the input with default data if they are passed', () => {
      const inputs = screen.getAllByRole('textbox');
      const textAreaElement = inputs[0] as HTMLTextAreaElement;
      const inputNameElement = inputs[1];
      expect(inputNameElement).toHaveValue(mockData.id);
      expect(textAreaElement).toHaveValue('');
    });

    it('should display the checkbox is not shown during the edit mode', async () => {
      const checkbox = screen.queryByRole('checkbox');
      expect(checkbox).not.toBeInTheDocument();
    });

    it('should display the CEL syntax help icon', async () => {
      const celHelpIcon = screen.queryByLabelText('info');
      expect(celHelpIcon).toBeVisible();
      expect(celHelpIcon).toBeEnabled();
    });
  });

  describe('Cancel and Submit button functionality', () => {
    it('should test whether the cancel callback is being called', async () => {
      const cancelCallback = jest.fn();
      renderComponent({ closeSideBar: cancelCallback });

      const cancelBtnElement = screen.getByText(/cancel/i);

      await userEvent.click(cancelBtnElement);
      expect(cancelCallback).toBeCalled();
    });

    it('should test whether the submit Callback is being called', async () => {
      const submitCallback = jest.fn();
      renderComponent({ setSmartFilter: submitCallback });

      const inputs = screen.getAllByRole('textbox');

      const textAreaElement = inputs[0] as HTMLTextAreaElement;
      textAreaElement.focus();
      await userEvent.paste('Hello World With TextArea');

      const inputNameElement = inputs[1];
      await userEvent.type(inputNameElement, 'Hello World!');

      const submitButtonElem = screen.getByText(/add filter/i);
      expect(submitButtonElem).toBeEnabled();

      await userEvent.click(submitButtonElem);
      expect(submitCallback).toBeCalled();
    });
  });
});
