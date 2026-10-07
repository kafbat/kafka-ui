import React from 'react';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import { useConfirm } from 'lib/hooks/useConfirm';
import { ConfirmContext } from 'components/contexts/ConfirmContext';

const callback = jest.fn();

const Prompts = () => {
  const confirm = useConfirm();
  const context = React.useContext(ConfirmContext);
  return (
    <>
      <button
        type="button"
        onClick={() =>
          confirm('Large content risk', callback, {
            title: 'Open large message?',
            confirmLabel: 'Open anyway',
          })
        }
      >
        Custom prompt
      </button>
      <button
        type="button"
        onClick={() => confirm('Ordinary action', callback)}
      >
        Default prompt
      </button>
      <output>
        {context?.title} / {context?.confirmLabel}
      </output>
    </>
  );
};

describe('ConfirmationModal customization', () => {
  beforeEach(() => callback.mockClear());

  it.each(['Cancel', 'Escape', 'overlay', 'confirm'])(
    'restores default labels after %s',
    async (action) => {
      render(<Prompts />);
      await userEvent.click(
        screen.getByRole('button', { name: 'Custom prompt' })
      );
      const dialog = screen.getByRole('dialog', {
        name: 'Open large message?',
      });
      expect(dialog).toHaveAttribute('aria-modal', 'true');
      expect(
        within(dialog).getByRole('button', { name: 'Cancel' })
      ).toHaveFocus();
      expect(
        within(dialog).getByRole('button', { name: 'Open anyway' })
      ).toBeInTheDocument();
      if (action === 'Escape') {
        await userEvent.keyboard('{Escape}');
      } else if (action === 'overlay') {
        const overlay = dialog.querySelector('[aria-hidden="true"]');
        expect(overlay).not.toBeNull();
        await userEvent.click(overlay!);
      } else {
        await userEvent.click(
          within(dialog).getByRole('button', {
            name: action === 'confirm' ? 'Open anyway' : 'Cancel',
          })
        );
      }
      expect(callback).toHaveBeenCalledTimes(action === 'confirm' ? 1 : 0);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.getByRole('status')).toHaveTextContent(
        'Confirm the action / Confirm'
      );
      await userEvent.click(
        screen.getByRole('button', { name: 'Default prompt' })
      );
      const ordinary = screen.getByRole('dialog', {
        name: 'Confirm the action',
      });
      expect(
        within(ordinary).getByRole('button', { name: 'Confirm' })
      ).toBeInTheDocument();
      expect(
        within(ordinary).queryByRole('button', { name: 'Open anyway' })
      ).not.toBeInTheDocument();
    }
  );
});
