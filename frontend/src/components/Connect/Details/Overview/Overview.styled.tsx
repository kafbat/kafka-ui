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

export const ModalContent = styled.div(
  ({ theme: { modal } }) => css`
    background-color: ${modal.backgroundColor};
    color: ${modal.color};
    border-radius: 8px;
    padding: 24px;
    max-width: 65vw;
    max-height: 80vh;
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

export const WorkerInfo = styled.p(
  ({ theme: { modal } }) => css`
    margin: 4px 0 0 0;
    font-size: 14px;
    color: ${modal.contentColor};
  `
);

export const TraceContent = styled.div(
  ({ theme: { modal } }) => css`
    background-color: ${modal.border.contrast};
    padding: 16px;
    border-radius: 6px;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    color: ${modal.color};
    border: 1px solid ${modal.border.contrast};
    max-height: 400px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-word;
  `
);

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
