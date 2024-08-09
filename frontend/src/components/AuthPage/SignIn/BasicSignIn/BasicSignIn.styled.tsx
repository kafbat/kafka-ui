import styled from 'styled-components';

export const Form = styled.form`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 40px;

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
