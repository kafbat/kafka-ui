import styled from 'styled-components';

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
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 18rem;
  max-width: 18rem;
  min-width: 0;
  @media screen and (max-width: 1450px) {
    width: 50%;
    max-width: 50%;
  }
  @media screen and (max-width: 1200px) {
    width: 100%;
    max-width: 100%;
  }

  & ul[role='listbox'] {
    width: 100%;
    max-width: 100%;
  }

  & ul[role='listbox'] > div {
    overflow: hidden;
    flex: 1;
    min-width: 0;
  }

  & ul[role='listbox'] > svg {
    flex-shrink: 0;
  }
`;
