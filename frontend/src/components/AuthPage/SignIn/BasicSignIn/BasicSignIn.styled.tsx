import styled from 'styled-components';

export const Form = styled.form`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 40px;
  width: 100%;

  div {
    width: 100%;
  }
`;

export const Fieldset = styled.fieldset`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  border: none;
  width: 100%;
`;

export const Field = styled.div`
  ${({ theme }) => theme.auth_page.signIn.label};
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 4px;
`;

export const Label = styled.label`
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
`;

export const ErrorMessage = styled.div`
  display: flex;
  column-gap: 2px;
  align-items: center;
  justify-content: center;
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;
`;

export const ErrorMessageText = styled.span`
  ${({ theme }) => theme.auth_page.signIn.errorMessage};
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;
`;
