import styled, { css } from 'styled-components';
import GitHubIcon from 'components/common/Icons/GitHubIcon';

export const AuthCardStyled = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    width: 400px;
    border: 1px solid black;
    border: 1px solid ${theme.auth_page.signIn.authCard.borderColor};
    border-radius: ${theme.auth_page.signIn.authCard.borderRadius};
    background-color: ${theme.auth_page.signIn.authCard.backgroundColor};
  `
);

export const ServiceData = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: 8px;
    align-items: center;

    svg,
    img {
      margin: 8px;
      width: 48px;
      height: 48px;
    }

    ${GitHubIcon} {
      fill: ${theme.auth_page.icons.githubColor};
    }
  `
);

export const ServiceDataTextContainer = styled.div`
  display: flex;
  flex-direction: column;
`;

export const ServiceNameStyled = styled.span(
  ({ theme }) => css`
    color: ${theme.auth_page.signIn.authCard.serviceNamecolor};
    font-size: 16px;
    font-weight: 500;
    line-height: 24px;
  `
);

export const ServiceTextStyled = styled.span(
  ({ theme }) => css`
    color: ${theme.auth_page.signIn.authCard.serviceTextColor};
    font-size: 12px;
    font-weight: 500;
    line-height: 16px;
  `
);
