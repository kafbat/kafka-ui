import React, { PropsWithChildren } from 'react';
import { screen, within } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import CustomParams, {
  CustomParamsProps,
} from 'components/Topics/shared/Form/CustomParams/CustomParams';
import { FormProvider, useForm } from 'react-hook-form';
import userEvent from '@testing-library/user-event';
import { TOPIC_CUSTOM_PARAMS } from 'lib/constants';

import { defaultValues } from './fixtures';

const selectOption = async (textinput: HTMLElement, option: string) => {
  await userEvent.click(textinput);
  await userEvent.clear(textinput);
  await userEvent.click(screen.getByText(option));
};

const expectOptionAvailability = async (
  textinput: () => HTMLElement,
  listbox: () => HTMLElement,
  option: string,
  disabled: boolean
) => {
  await userEvent.click(textinput());
  const selectedOptions = within(listbox()).getAllByText(option).reverse();
  // its either two or one nodes, we only need last one
  const selectedOption = selectedOptions[0];

  if (disabled) {
    expect(selectedOption).toHaveAttribute('disabled');
  } else {
    expect(selectedOption).toBeEnabled();
  }

  expect(selectedOption).toHaveStyleRule(
    'cursor',
    disabled ? 'not-allowed' : 'pointer'
  );
  await userEvent.click(document.body);
};

const renderComponent = (props: CustomParamsProps, defaults = {}) => {
  const Wrapper: React.FC<PropsWithChildren<unknown>> = ({ children }) => {
    const methods = useForm({ defaultValues: defaults });
    return <FormProvider {...methods}>{children}</FormProvider>;
  };

  return render(
    <Wrapper>
      <CustomParams {...props} />
    </Wrapper>
  );
};

const getInputBox = () => screen.getByRole('listitem');
const getAllInputBoxes = () => screen.getAllByRole('listitem');
const getListbox = () => screen.getByRole('listbox');
const getTextBox = () => screen.getByRole('textbox');
const getAllTextBoxes = () => screen.getAllByRole('textbox');
const getAddNewFieldButton = () => screen.getByText('Add Custom Parameter');

describe('CustomParams', () => {
  it('renders with props', () => {
    renderComponent({ isSubmitting: false });

    expect(getAddNewFieldButton()).toBeInTheDocument();
    expect(getAddNewFieldButton()).toHaveTextContent('Add Custom Parameter');
  });

  it('has defaultValues when they are set', () => {
    renderComponent({ isSubmitting: false }, defaultValues);

    expect(getInputBox()).toHaveValue(defaultValues.customParams[0].name);
    expect(getTextBox()).toHaveValue(defaultValues.customParams[0].value);
  });

  describe('works with user inputs correctly', () => {
    beforeEach(async () => {
      renderComponent({ isSubmitting: false });
      await userEvent.click(getAddNewFieldButton());
    });

    it('button click creates custom param fieldset', async () => {
      await userEvent.click(getInputBox());
      expect(getListbox()).toBeInTheDocument();
      expect(getTextBox()).toBeInTheDocument();
    });

    it('can select option', async () => {
      await selectOption(getInputBox(), 'compression.type');
      expect(getInputBox()).toHaveValue('compression.type');

      await expectOptionAvailability(
        getInputBox,
        getListbox,
        'compression.type',
        true
      );

      expect(getTextBox()).toHaveValue(TOPIC_CUSTOM_PARAMS['compression.type']);
    });

    it('when selected option changes disabled options update correctly', async () => {
      await selectOption(getInputBox(), 'compression.type');
      expect(getInputBox()).toHaveValue('compression.type');

      await expectOptionAvailability(
        getInputBox,
        getListbox,
        'compression.type',
        true
      );

      await selectOption(getInputBox(), 'delete.retention.ms');
      await expectOptionAvailability(
        getInputBox,
        getListbox,
        'delete.retention.ms',
        true
      );

      await userEvent.click(getAddNewFieldButton());
      await expectOptionAvailability(
        () => getAllInputBoxes()[1],
        getListbox,
        'compression.type',
        false
      );
    });

    it('multiple button clicks create multiple fieldsets', async () => {
      await userEvent.click(getAddNewFieldButton());
      await userEvent.click(getAddNewFieldButton());

      expect(getAllInputBoxes().length).toBe(3);

      expect(getAllTextBoxes().length).toBe(3);
    });

    it("can't select already selected option", async () => {
      await userEvent.click(getAddNewFieldButton());

      await selectOption(getAllInputBoxes()[0], 'compression.type');
      await expectOptionAvailability(
        () => getAllInputBoxes()[0],
        getListbox,
        'compression.type',
        true
      );

      await expectOptionAvailability(
        () => getAllInputBoxes()[1],
        getListbox,
        'compression.type',
        true
      );
    });

    it('when fieldset with selected custom property type is deleted disabled options update correctly', async () => {
      await userEvent.click(getAddNewFieldButton());
      await userEvent.click(getAddNewFieldButton());

      const [firstListBox, secondListbox, thirdListbox] = getAllInputBoxes();
      await selectOption(firstListBox, 'compression.type');
      await expectOptionAvailability(
        () => firstListBox,
        getListbox,
        'compression.type',
        true
      );

      await selectOption(secondListbox, 'delete.retention.ms');
      await expectOptionAvailability(
        () => secondListbox,
        getListbox,
        'delete.retention.ms',
        true
      );

      await selectOption(thirdListbox, 'file.delete.delay.ms');
      await expectOptionAvailability(
        () => thirdListbox,
        getListbox,
        'file.delete.delay.ms',
        true
      );

      const deleteSecondFieldsetButton = screen.getByTitle(
        'Delete customParam field 1'
      );
      await userEvent.click(deleteSecondFieldsetButton);
      expect(secondListbox).not.toBeInTheDocument();

      await userEvent.clear(firstListBox);
      await expectOptionAvailability(
        () => firstListBox,
        getListbox,
        'delete.retention.ms',
        false
      );

      await userEvent.clear(thirdListbox);
      await expectOptionAvailability(
        () => thirdListbox,
        getListbox,
        'delete.retention.ms',
        false
      );
    });
  });
});
