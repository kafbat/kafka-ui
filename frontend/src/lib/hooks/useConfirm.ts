import { ConfirmContext } from 'components/contexts/ConfirmContext';
import { type ReactNode, useContext } from 'react';

/** Creates a dialog callback for confirming synchronous or asynchronous actions. */
export const useConfirm = (danger = false) => {
  const context = useContext(ConfirmContext);

  return (
    message: ReactNode,
    callback: () => void | Promise<unknown>,
    options?: {
      title?: string;
      confirmLabel?: string;
    }
  ) => {
    context?.setDangerButton(danger);
    context?.setContent(message);
    context?.setTitle(options?.title || 'Confirm the action');
    context?.setConfirmLabel(options?.confirmLabel || 'Confirm');
    context?.setIsConfirming(false);
    context?.setConfirm(() => async () => {
      context?.setIsConfirming(true);

      try {
        await callback();
      } finally {
        context?.setIsConfirming(false);
        context?.cancel();
      }
    });
  };
};
