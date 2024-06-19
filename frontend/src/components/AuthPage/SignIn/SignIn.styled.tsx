import styled, { css } from 'styled-components';

export const SignInStyled = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 320px;
  gap: 56px;
`;

export const SignInTitle = styled.span(
  ({ theme }) => css`
    color: ${theme.auth_page.signIn.titleColor};
    font-size: 24px;
    font-weight: 600;
  `
);
