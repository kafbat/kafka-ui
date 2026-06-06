import styled, { css } from 'styled-components';

export const SignInStyled = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: min(380px, calc(100vw - 32px));
  gap: 28px;
  flex-grow: 1;
  margin: 24px 0 56px;
  padding: 28px;
  border: 1px solid ${({ theme }) => theme.surface.border};
  border-radius: 8px;
  background: ${({ theme }) => theme.surface.panel};
  box-shadow: ${({ theme }) => theme.surface.shadowLg};
`;

export const SignInTitle = styled.span(
  ({ theme }) => css`
    color: ${theme.auth_page.signIn.titleColor};
    font-size: 28px;
    line-height: 36px;
    font-weight: 700;
    letter-spacing: 0;
  `
);
