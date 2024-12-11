import styled from 'styled-components';

export const List = styled.ul.attrs({ role: 'menu' })`
  padding: 2px 4px 6px 4px;

  & > & {
    padding: 0 0 0 8px;
  }

  & * {
    margin-bottom: 2px;
  }
`;

export const AccordionContent = styled.div<{ isOpen: boolean }>`
  overflow: hidden;
  max-height: ${({ isOpen }) => (isOpen ? '500px' : '0')};
  opacity: ${({ isOpen }) => (isOpen ? '1' : '0')};
  transition:
    max-height 0.4s ease-out,
    opacity 0.3s ease-out;
`;
