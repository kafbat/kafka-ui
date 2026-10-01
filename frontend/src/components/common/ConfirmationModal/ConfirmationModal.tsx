import React from 'react';
import { Button } from 'components/common/Button/Button';
import { ConfirmContext } from 'components/contexts/ConfirmContext';

import * as S from './ConfirmationModal.styled';

/** Renders the active confirmation prompt with keyboard and modal semantics. */
const ConfirmationModal: React.FC = () => {
  const context = React.useContext(ConfirmContext);
  const isOpen = context?.content && context?.confirm;

  React.useEffect(() => {
    if (!isOpen || !context) return undefined;

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') context.cancel();
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [context, isOpen]);

  if (!isOpen) return null;

  return (
    <S.Wrapper
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirmation-modal-title"
    >
      <S.Overlay onClick={context.cancel} aria-hidden="true" />
      <S.Modal>
        <S.Header id="confirmation-modal-title">{context.title}</S.Header>
        <S.Content>{context.content}</S.Content>
        <S.Footer>
          <Button
            buttonType="secondary"
            buttonSize="M"
            onClick={context.cancel}
            type="button"
            autoFocus
          >
            Cancel
          </Button>
          <Button
            buttonType={context.dangerButton ? 'danger' : 'primary'}
            buttonSize="M"
            onClick={context.confirm}
            type="button"
            inProgress={context?.isConfirming}
          >
            {context.confirmLabel}
          </Button>
        </S.Footer>
      </S.Modal>
    </S.Wrapper>
  );
};

export default ConfirmationModal;
