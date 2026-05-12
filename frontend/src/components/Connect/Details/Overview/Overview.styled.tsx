import styled, { css } from 'styled-components';

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
