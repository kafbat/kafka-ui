import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import Dropdown from 'components/common/Dropdown/Dropdown';
import DropdownItem from 'components/common/Dropdown/DropdownItem';
import React from 'react';

describe('Dropdown', () => {
  it.each([
    ['default trigger', false, 'Open menu'],
    ['custom trigger', true, 'Open custom menu'],
  ])(
    'returns focus to the %s after selecting an item',
    async (_, useCustomTrigger, label) => {
      const onClose = jest.fn();
      const openBtnEl = useCustomTrigger ? (
        <button type="button">Open custom menu</button>
      ) : undefined;
      render(
        <Dropdown
          label={label}
          openBtnEl={openBtnEl}
          onClose={onClose}
          aria-label={label}
        >
          <DropdownItem onClick={jest.fn()}>Close menu</DropdownItem>
        </Dropdown>
      );

      const trigger = screen.getByRole('button', { name: label });
      await userEvent.click(trigger);
      await userEvent.click(
        screen.getByRole('menuitem', { name: 'Close menu' })
      );

      expect(trigger).toHaveFocus();
      expect(onClose).toHaveBeenCalled();
    }
  );
});
