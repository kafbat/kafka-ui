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
  width: 18rem;
  @media screen and (max-width: 1450px) {
    width: 50%;
  }
  @media screen and (max-width: 1200px) {
    width: 100%;
  }
`;
export const Field = styled.div`
  ${({ theme }) => theme.input.label};
  display: flex;
  justify-content: space-between;

  & ul {
    width: 100%;
  }
`;

export const Label = styled.label`
  line-height: 32px;
`;

export const Input = styled.input`
  display: flex;
  justify-content: space-between;

  & ul {
    width: 100%;
  }
`;

export const ErrorMessage = styled.p`
  color: ${({ theme }) => theme.notification.variant.error.borderColor};
  font-size: 12px;
  margin-top: 4px;
  font-weight: 500;
`;
