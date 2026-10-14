import React from 'react';
import { Button } from 'components/common/Button/Button';
import { ConfirmContext } from 'components/contexts/ConfirmContext';

import * as S from './ConfirmationModal.styled';

const ConfirmationModal: React.FC = () => {
  const context = React.useContext(ConfirmContext);
  const isOpen = Boolean(context?.content && context?.confirm);
  const modalRef = React.useRef<HTMLDivElement>(null);
  const footerRef = React.useRef<HTMLDivElement>(null);

  React.useLayoutEffect(() => {
    const modal = modalRef.current;
    if (!isOpen || !modal) return undefined;

    const previousFocus = document.activeElement;
    const focusCancel = () => {
      (footerRef.current?.querySelector('button') || modal).focus();
    };
    const containFocus = (event: FocusEvent) => {
      if (event.target instanceof Node && !modal.contains(event.target)) {
        focusCancel();
      }
    };
    const trapTab = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return;

      const focusable = Array.from(
        modal.querySelectorAll<HTMLElement>(
          'button, a[href], input, select, textarea, [tabindex]'
        )
      ).filter(
        (element) =>
          element.tabIndex >= 0 &&
          !element.matches(':disabled') &&
          !element.closest('[hidden], [inert]') &&
          getComputedStyle(element).display !== 'none' &&
          getComputedStyle(element).visibility !== 'hidden'
      );
      const first = focusable[0] || modal;
      const last = focusable[focusable.length - 1] || modal;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    focusCancel();
    document.addEventListener('focusin', containFocus);
    document.addEventListener('keydown', trapTab);
    return () => {
      document.removeEventListener('focusin', containFocus);
      document.removeEventListener('keydown', trapTab);
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, [isOpen]);

  React.useEffect(() => {
    if (!isOpen || !context) return undefined;

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') context.cancel();
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [context, isOpen]);

  if (!isOpen || !context) return null;

  return (
    <S.Wrapper
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirmation-modal-title"
    >
      <S.Overlay onClick={context.cancel} aria-hidden="true" />
      <S.Modal ref={modalRef} tabIndex={-1}>
        <S.Header id="confirmation-modal-title">{context.title}</S.Header>
        <S.Content>{context.content}</S.Content>
        <S.Footer ref={footerRef}>
          <Button
            buttonType="secondary"
            buttonSize="M"
            onClick={context.cancel}
            type="button"
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
