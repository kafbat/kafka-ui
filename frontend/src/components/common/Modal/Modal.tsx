import React from 'react';

import * as S from './Modal.styled';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidth?: string;
  maxHeight?: string;
}

const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  footer,
  maxWidth = '65vw',
  maxHeight = '80vh',
}) => {
  if (!isOpen) return null;

  return (
    <S.ModalOverlay onClick={onClose} role="dialog" aria-label="Modal">
      <S.ModalContent
        onClick={(e: React.MouseEvent) => e.stopPropagation()}
        maxWidth={maxWidth}
        maxHeight={maxHeight}
      >
        {title && (
          <S.ModalHeader>
            <S.ModalTitle>{title}</S.ModalTitle>
          </S.ModalHeader>
        )}

        <S.ModalBody>{children}</S.ModalBody>

        {footer && <S.ModalFooter>{footer}</S.ModalFooter>}
      </S.ModalContent>
    </S.ModalOverlay>
  );
};

export default Modal;
