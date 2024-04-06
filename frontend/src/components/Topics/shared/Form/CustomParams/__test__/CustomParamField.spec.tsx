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

  const setupComponent = (props: Props) => {
    const Wrapper: React.FC<PropsWithChildren<unknown>> = ({ children }) => {
      const methods = useForm();
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

  const getInputBox = () => screen.getByRole('listitem');
  const getListbox = () => screen.getByRole('listbox');
  const getTextBox = () => screen.getByRole('textbox');
  const getButton = () => screen.getByRole('button');

  it('renders the component with its view correctly', async () => {
    setupComponent({
      field,
      isDisabled,
      index,
      remove,
      existingFields,
      setExistingFields,
    });
    expect(getInputBox()).toBeInTheDocument();
    expect(getTextBox()).toBeInTheDocument();
    expect(getButton()).toBeInTheDocument();
    await userEvent.click(getInputBox());
    expect(getListbox()).toBeInTheDocument();
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
      await userEvent.click(getButton());
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
      await userEvent.type(getButton(), SPACE_KEY);
      // userEvent.type triggers remove two times as at first it clicks on element and then presses space
      expect(remove).toHaveBeenCalledTimes(2);
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
      await userEvent.click(getInputBox());
      await selectOption(getListbox(), 'compression.type');
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
      expect(getInputBox()).toHaveValue('compression.type');
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
      await userEvent.click(getInputBox());
      await selectOption(getListbox(), 'compression.type');

      expect(getTextBox()).toHaveValue(TOPIC_CUSTOM_PARAMS['compression.type']);
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
      await userEvent.click(getInputBox());
      await selectOption(getListbox(), 'compression.type');

      expect(setExistingFields).toHaveBeenCalledTimes(1);
    });
  });
});
