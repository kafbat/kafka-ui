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

const getCustomParamInput = () => screen.getByRole('listitem');
const getAllCustomParamInputs = () => screen.getAllByRole('listitem');
const getCustomParamsList = () => screen.getByRole('listbox');
const getValueInput = () => screen.getByRole('textbox');
const getAllValueInputs = () => screen.getAllByRole('textbox');
const getAddNewFieldButton = () => screen.getByText('Add Custom Parameter');

const topicCustomParam1 = Object.keys(TOPIC_CUSTOM_PARAMS)[0];
const topicCustomParam2 = Object.keys(TOPIC_CUSTOM_PARAMS)[1];
const topicCustomParam3 = Object.keys(TOPIC_CUSTOM_PARAMS)[2];

describe('CustomParams', () => {
  it('renders with props', () => {
    renderComponent({ isSubmitting: false });

    expect(getAddNewFieldButton()).toBeInTheDocument();
    expect(getAddNewFieldButton()).toHaveTextContent('Add Custom Parameter');
  });

  it('has defaultValues when they are set', () => {
    renderComponent({ isSubmitting: false }, defaultValues);

    expect(getCustomParamInput()).toHaveValue(
      defaultValues.customParams[0].name
    );
    expect(getValueInput()).toHaveValue(defaultValues.customParams[0].value);
  });

  describe('works with user inputs correctly', () => {
    beforeEach(async () => {
      renderComponent({ isSubmitting: false });
      await userEvent.click(getAddNewFieldButton());
    });

    it('button click creates custom param fieldset', async () => {
      await userEvent.click(getCustomParamInput());
      expect(getCustomParamsList()).toBeInTheDocument();
      expect(getValueInput()).toBeInTheDocument();
    });

    it('can select option', async () => {
      await selectOption(getCustomParamInput(), topicCustomParam1);
      expect(getCustomParamInput()).toHaveValue(topicCustomParam1);

      await expectOptionAvailability(
        getCustomParamInput,
        getCustomParamsList,
        topicCustomParam1,
        true
      );

      expect(getValueInput()).toHaveValue(
        TOPIC_CUSTOM_PARAMS[topicCustomParam1]
      );
    });

    it('when selected option changes disabled options update correctly', async () => {
      await selectOption(getCustomParamInput(), topicCustomParam1);
      expect(getCustomParamInput()).toHaveValue(topicCustomParam1);

      await expectOptionAvailability(
        getCustomParamInput,
        getCustomParamsList,
        topicCustomParam1,
        true
      );

      await selectOption(getCustomParamInput(), topicCustomParam2);
      await expectOptionAvailability(
        getCustomParamInput,
        getCustomParamsList,
        topicCustomParam2,
        true
      );

      await userEvent.click(getAddNewFieldButton());
      await expectOptionAvailability(
        () => getAllCustomParamInputs()[1],
        getCustomParamsList,
        topicCustomParam1,
        false
      );
    });

    it('multiple button clicks create multiple fieldsets', async () => {
      await userEvent.click(getAddNewFieldButton());
      await userEvent.click(getAddNewFieldButton());

      expect(getAllCustomParamInputs().length).toBe(3);

      expect(getAllValueInputs().length).toBe(3);
    });

    it("can't select already selected option", async () => {
      await userEvent.click(getAddNewFieldButton());

      await selectOption(getAllCustomParamInputs()[0], topicCustomParam1);
      await expectOptionAvailability(
        () => getAllCustomParamInputs()[0],
        getCustomParamsList,
        topicCustomParam1,
        true
      );

      await expectOptionAvailability(
        () => getAllCustomParamInputs()[1],
        getCustomParamsList,
        topicCustomParam1,
        true
      );
    });

    it('when fieldset with selected custom property type is deleted disabled options update correctly', async () => {
      await userEvent.click(getAddNewFieldButton());
      await userEvent.click(getAddNewFieldButton());

      const [firstListBox, secondListbox, thirdListbox] =
        getAllCustomParamInputs();
      await selectOption(firstListBox, topicCustomParam1);
      await expectOptionAvailability(
        () => firstListBox,
        getCustomParamsList,
        topicCustomParam1,
        true
      );

      await selectOption(secondListbox, topicCustomParam2);
      await expectOptionAvailability(
        () => secondListbox,
        getCustomParamsList,
        topicCustomParam2,
        true
      );

      await selectOption(thirdListbox, topicCustomParam3);
      await expectOptionAvailability(
        () => thirdListbox,
        getCustomParamsList,
        topicCustomParam3,
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
        getCustomParamsList,
        topicCustomParam2,
        false
      );

      await userEvent.clear(thirdListbox);
      await expectOptionAvailability(
        () => thirdListbox,
        getCustomParamsList,
        topicCustomParam2,
        false
      );
    });
  });
});
