import { ConfirmContext } from 'components/contexts/ConfirmContext';
import { type ReactNode, useContext } from 'react';

export const useConfirm = (danger = false) => {
  const context = useContext(ConfirmContext);

  return (message: ReactNode, callback: () => void | Promise<unknown>) => {
    context?.setDangerButton(danger);
    context?.setContent(message);
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
