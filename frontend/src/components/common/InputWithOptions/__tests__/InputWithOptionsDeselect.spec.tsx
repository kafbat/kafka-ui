import React from 'react';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InputWithOptions from 'components/common/InputWithOptions/InputWithOptions';
import { SelectOption } from 'components/common/Select/Select';

// Mirrors what SendMessage builds for an optional serde parameter: a
// "(default)" entry followed by the serde's allowedValues.
const options: Array<SelectOption<string>> = [
  { label: '(default)', value: '' },
  {
    label: 'test.events.OrderPlacedEvent',
    value: 'test.events.OrderPlacedEvent',
  },
  {
    label: 'test.events.AccountUpdatedEvent',
    value: 'test.events.AccountUpdatedEvent',
  },
];

const Harness = () => {
  const [formValue, setFormValue] = React.useState('');
  return (
    <>
      <span data-testid="form-value">
        {formValue === '' ? '(unset)' : formValue}
      </span>
      <InputWithOptions
        name="messageName"
        options={options}
        value={formValue}
        onChange={setFormValue}
      />
    </>
  );
};

describe('serde parameter deselection', () => {
  const getInputBox = () => screen.getByRole('listitem');
  const getListbox = () => screen.getByRole('listbox');
  const getFormValue = () => screen.getByTestId('form-value');

  beforeEach(() => {
    render(<Harness />);
  });

  it('offers an option to clear the selection', async () => {
    await userEvent.click(getInputBox());
    expect(
      screen.getAllByRole('option').map((o) => o.getAttribute('value'))
    ).toContain('');
  });

  it('clears a chosen value when "(default)" is selected', async () => {
    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), [
      'test.events.AccountUpdatedEvent',
    ]);
    expect(getFormValue()).toHaveTextContent('test.events.AccountUpdatedEvent');

    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), ['(default)']);

    expect(getFormValue()).toHaveTextContent('(unset)');
    expect(getInputBox()).toHaveValue('');
  });

  it('keeps the parameter unset after clicking outside', async () => {
    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), [
      'test.events.AccountUpdatedEvent',
    ]);
    await userEvent.click(getInputBox());
    await userEvent.selectOptions(getListbox(), ['(default)']);

    await userEvent.click(getInputBox());
    await userEvent.click(document.body);

    expect(getFormValue()).toHaveTextContent('(unset)');
  });
});
