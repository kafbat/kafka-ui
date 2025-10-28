import styled, { css } from 'styled-components';

export const ModalOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: ${({ theme }) => theme.modal.overlay};
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

export const ModalContent = styled.div<{
  maxWidth: string;
  maxHeight: string;
}>(
  ({ theme: { modal }, maxWidth, maxHeight }) => css`
    background-color: ${modal.backgroundColor};
    color: ${modal.color};
    border-radius: 8px;
    padding: 24px;
    max-width: ${maxWidth};
    max-height: ${maxHeight};
    overflow: auto;
    position: relative;
    border: 1px solid ${modal.border.contrast};
    box-shadow: 0 4px 20px ${modal.shadow};
  `
);

export const ModalHeader = styled.div(
  ({ theme: { modal } }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    border-bottom: 1px solid ${modal.border.bottom};
    padding-bottom: 12px;
  `
);

export const ModalTitle = styled.h3`
  margin: 0;
  font-size: 18px;
  font-weight: 600;
`;

export const ModalBody = styled.div`
  margin-bottom: 16px;
`;

export const ModalFooter = styled.div(
  ({ theme: { modal } }) => css`
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid ${modal.border.top};
    text-align: center;
    display: flex;
    justify-content: center;
  `
);
