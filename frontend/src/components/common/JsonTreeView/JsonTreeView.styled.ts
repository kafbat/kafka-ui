import styled from 'styled-components';

export const Wrapper = styled.div`
  font-family: Inconsolata, monospace;
  font-size: 14px;
  line-height: 19px;
  overflow: auto;
`;

export const Row = styled.div`
  display: flex;
  align-items: flex-start;
  white-space: pre;

  &:hover {
    background-color: ${({ theme }) => theme.viewer.tree.rowHover};
  }
`;

export const Indent = styled.span<{ $depth: number }>`
  flex-shrink: 0;
  width: ${({ $depth }) => $depth * 16 + 16}px;
`;

export const ToggleRow = styled.button`
  display: flex;
  align-items: flex-start;
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  cursor: pointer;
  font: inherit;
  color: inherit;
  text-align: left;

  &:focus-visible {
    outline: 1px solid ${({ theme }) => theme.viewer.tree.key};
  }
`;

export const Arrow = styled.span<{ $isOpen: boolean }>`
  flex-shrink: 0;
  width: 16px;
  display: inline-block;
  color: ${({ theme }) => theme.viewer.tree.muted};
  transform: rotate(${({ $isOpen }) => ($isOpen ? 90 : 0)}deg);
  transition: transform 0.1s ease-in-out;
`;

export const Key = styled.span`
  color: ${({ theme }) => theme.viewer.tree.key};
`;

export const Punctuation = styled.span`
  color: ${({ theme }) => theme.viewer.tree.punctuation};
`;

export const Muted = styled.span`
  color: ${({ theme }) => theme.viewer.tree.muted};
  font-style: italic;
`;

export const StringValue = styled.span`
  color: ${({ theme }) => theme.viewer.tree.string};
`;

export const NumberValue = styled.span`
  color: ${({ theme }) => theme.viewer.tree.number};
`;
