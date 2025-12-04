import styled from 'styled-components';
import { Button } from 'components/common/Button/Button';

export const Wrapper = styled.div`
  display: block;
  border-radius: 6px;
`;

export const Columns = styled.div`
  margin: -0.75rem;
  margin-bottom: 0.75rem;
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  gap: 8px;

  @media screen and (min-width: 769px) {
    display: flex;
  }
`;

export const Flex = styled.div`
  display: flex;
  flex-direction: row;
  gap: 8px;
  @media screen and (max-width: 1200px) {
    flex-direction: column;
  }
`;

export const FlexItem = styled.div`
  width: 18rem;
  @media screen and (max-width: 1450px) {
    width: 50%;
  }
  @media screen and (max-width: 1200px) {
    width: 100%;
  }
`;

// New styled components for JSON formatting functionality
export const ValidationSection = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
`;

export const FieldGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const FieldHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
`;

export const FormatButton = styled(Button)`
  font-size: 12px;
  padding: 4px 8px;
  height: 24px;
  min-width: auto;
  border-radius: 3px;
  
  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.button.primary.backgroundColor.normal};
    outline-offset: 2px;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  // Ensure proper contrast and accessibility
  &[aria-pressed="true"] {
    background: ${({ theme }) => theme.button.primary.backgroundColor.active};
    color: ${({ theme }) => theme.button.primary.color.active};
  }
`;

export const ResizableEditorWrapper = styled.div`
  .ace_editor {
    resize: vertical !important;
    min-height: 40px !important;
    max-height: 200px !important;
    overflow: auto;
    border: 1px solid ${({ theme }) => theme.input?.borderColor?.normal || '#ddd'};
    border-radius: 4px;
    
    &:focus-within {
      border-color: ${({ theme }) => theme.input?.borderColor?.focus || theme.button.primary.backgroundColor.normal};
      box-shadow: 0 0 0 2px ${({ theme }) => theme.button.primary.backgroundColor.normal}33;
    }
  }
  
  .ace_content {
    cursor: text;
  }
  
  .ace_scrollbar-v {
    right: 0 !important;
  }
  
  .ace_scrollbar-h {
    bottom: 0 !important;
  }
  
  // Enhanced resize handle visibility
  .ace_editor::after {
    content: '';
    position: absolute;
    bottom: 0;
    right: 0;
    width: 12px;
    height: 12px;
    background: linear-gradient(
      135deg,
      transparent 0%,
      transparent 46%,
      ${({ theme }) => theme.input?.borderColor?.normal || '#ddd'} 46%,
      ${({ theme }) => theme.input?.borderColor?.normal || '#ddd'} 50%,
      transparent 50%,
      transparent 56%,
      ${({ theme }) => theme.input?.borderColor?.normal || '#ddd'} 56%,
      ${({ theme }) => theme.input?.borderColor?.normal || '#ddd'} 60%,
      transparent 60%
    );
    cursor: nw-resize;
    z-index: 10;
  }
`;

// Error state styling for validation feedback
export const ValidationError = styled.div`
  color: ${({ theme }) => theme.button.danger.backgroundColor.normal};
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  
  &::before {
    content: '⚠';
    font-size: 14px;
  }
`;

// Success state styling for formatting feedback
export const FormatSuccess = styled.div`
  color: ${({ theme }) => theme.button.primary.backgroundColor.normal};
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  
  &::before {
    content: '✓';
    font-size: 14px;
    font-weight: bold;
  }
`;

// Accessibility improvements for screen readers
export const ScreenReaderOnly = styled.span`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
`;