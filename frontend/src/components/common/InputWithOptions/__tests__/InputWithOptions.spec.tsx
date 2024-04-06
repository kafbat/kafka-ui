import React from 'react';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InputWithOptions, {
  InputWithOptionsProps,
  SelectOption,
} from 'components/common/InputWithOptions/InputWithOptions';

jest.mock('react-hook-form', () => ({
  useFormContext: () => ({
    register: jest.fn(),
  }),
}));

const options: Array<SelectOption> = [
  { label: 'test-label1', value: 'test-value1', disabled: false },
  { label: 'test-label2', value: 'test-value2', disabled: false },
  { label: 'test-label3', value: 'test-value3', disabled: false },
  { label: 'test-label4', value: 'test-value4', disabled: true },
];

const renderComponent = (props?: Partial<InputWithOptionsProps>) => {
  render(
    <InputWithOptions value="test" name="test" options={options} {...props} />
  );
};

describe('InputWithOptions component', () => {
  beforeEach(() => {
    renderComponent({ onChange: () => {} });
  });

  const getInputBox = () => screen.getByRole('textbox');
  const getListbox = () => screen.getByRole('listbox');

  it('renders component', () => {
    expect(getInputBox()).toBeInTheDocument();
  });

  it('shows select options when select is being clicked', async () => {
    expect(getInputBox()).toBeInTheDocument();
    await userEvent.click(getInputBox());
    expect(getListbox()).toBeInTheDocument();
    expect(screen.getAllByRole('option')).toHaveLength(4);
  });

  it('checking select option change', async () => {
    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), ['test-label1']);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(getInputBox()).toHaveValue('test-value1');
  });

  it('trying to select disabled option does not trigger change', async () => {
    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), ['test-label4']);
    expect(screen.queryByRole('listbox')).toBeInTheDocument();
    expect(getInputBox()).toHaveValue('');
  });

  it('allows user to type', async () => {
    await userEvent.click(getInputBox());
    await userEvent.type(getInputBox(), 'test');
    expect(getInputBox()).toHaveValue('test');
  });

  it('options list depends on the input value', async () => {
    await userEvent.click(getInputBox());
    await userEvent.type(getInputBox(), 'test-label1');
    expect(screen.getByRole('option')).toBeInTheDocument();
  });

  it('click outside closes the list', async () => {
    await userEvent.click(getInputBox());
    await userEvent.click(document.body);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('click outside select option', async () => {
    await userEvent.click(getInputBox());
    await userEvent.type(getInputBox(), 'test');
    await userEvent.click(document.body);
    expect(getInputBox()).toHaveValue('test');
  });
});
