import React, { PropsWithChildren } from 'react';
import { act, screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import CustomParamsField, {
  Props,
} from 'components/Topics/shared/Form/CustomParams/CustomParamField';
import { FormProvider, useForm } from 'react-hook-form';
import userEvent from '@testing-library/user-event';
import { TOPIC_CUSTOM_PARAMS } from 'lib/constants';

const isDisabled = false;
const index = 0;
const existingFields: string[] = [];
const field = { name: 'name', value: 'value', id: 'id' };

const SPACE_KEY = ' ';

const selectOption = async (listbox: HTMLElement, option: string) => {
  await act(() => userEvent.click(listbox));
  await act(() => userEvent.click(screen.getByText(option)));
};

describe('CustomParamsField', () => {
  const remove = jest.fn();
  const setExistingFields = jest.fn();

  const setupComponent = (props: Props, formDefaults: object = {}) => {
    const Wrapper: React.FC<PropsWithChildren<unknown>> = ({ children }) => {
      const methods = useForm({ defaultValues: formDefaults });
      return <FormProvider {...methods}>{children}</FormProvider>;
    };

    return render(
      <Wrapper>
        <CustomParamsField {...props} />
      </Wrapper>
    );
  };

  afterEach(() => {
    remove.mockClear();
    setExistingFields.mockClear();
  });

  const getCustomParamInput = () => screen.getByRole('listitem');
  const getCustomParamsList = () => screen.getByRole('listbox');
  const getValueInput = () => screen.getByRole('textbox');
  const getRemoveButton = () => screen.getByRole('button');

  const topicCustomParam1 = Object.keys(TOPIC_CUSTOM_PARAMS)[0];

  it('renders the component with its view correctly', async () => {
    setupComponent({
      field,
      isDisabled,
      index,
      remove,
      existingFields,
      setExistingFields,
    });
    expect(getCustomParamInput()).toBeInTheDocument();
    expect(getValueInput()).toBeInTheDocument();
    expect(getRemoveButton()).toBeInTheDocument();
    await userEvent.click(getCustomParamInput());
    expect(getCustomParamsList()).toBeInTheDocument();
  });

  describe('core functionality works', () => {
    it('click on button triggers remove', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields,
        setExistingFields,
      });
      await userEvent.click(getRemoveButton());
      expect(remove).toHaveBeenCalledTimes(1);
    });

    it('pressing space on button triggers remove', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields,
        setExistingFields,
      });
      await userEvent.type(getRemoveButton(), SPACE_KEY);
      // userEvent.type triggers remove two times as at first it clicks on element and then presses space
      expect(remove).toHaveBeenCalledTimes(2);
    });

    it('clicking delete removes the field name from existingFields', async () => {
      const selectedParam = Object.keys(TOPIC_CUSTOM_PARAMS)[0];
      const otherParam = 'some.other.param';
      // Initialize the form with the param name so nameValue is set in the component
      const formDefaults = {
        customParams: [{ name: selectedParam, value: '' }],
      };

      setupComponent(
        {
          field,
          isDisabled,
          index,
          remove,
          existingFields: [selectedParam, otherParam],
          setExistingFields,
        },
        formDefaults
      );

      await userEvent.click(getRemoveButton());

      // setExistingFields should have been called with the selected param removed
      expect(setExistingFields).toHaveBeenCalledWith([otherParam]);
      expect(remove).toHaveBeenCalledWith(index);
    });

    it('clicking delete calls remove even when field has no selected name', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields: [],
        setExistingFields,
      });

      await userEvent.click(getRemoveButton());
      expect(remove).toHaveBeenCalledWith(index);
    });

    it('can select option', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields,
        setExistingFields,
      });
      await userEvent.click(getCustomParamInput());
      await selectOption(getCustomParamsList(), topicCustomParam1);
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
      expect(getCustomParamInput()).toHaveValue(topicCustomParam1);
    });

    it('selecting option updates textbox value', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields,
        setExistingFields,
      });
      await userEvent.click(getCustomParamInput());
      await selectOption(getCustomParamsList(), topicCustomParam1);

      expect(getValueInput()).toHaveValue(
        TOPIC_CUSTOM_PARAMS[topicCustomParam1]
      );
    });

    it('selecting option updates triggers setExistingFields', async () => {
      setupComponent({
        field,
        isDisabled,
        index,
        remove,
        existingFields,
        setExistingFields,
      });
      await userEvent.click(getCustomParamInput());
      await selectOption(getCustomParamsList(), topicCustomParam1);

      expect(setExistingFields).toHaveBeenCalledTimes(1);
    });
  });
});
