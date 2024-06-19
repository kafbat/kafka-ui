import styled, { css } from 'styled-components';
import GitHubIcon from 'components/common/Icons/GitHubIcon';

export const FooterStyledWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 19px 150px;
  font-size: 12px;
`;

export const AppVersionStyled = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    width: 200px;

    ${GitHubIcon} {
      width: 14px;
      height: 14px;
      fill: ${theme.auth_page.icons.githubColor};
    }
  `
);

export const AppVersionTextStyled = styled.span(
  ({ theme }) => css`
    font-weight: ${theme.auth_page.footer.span.fontWeight};
    line-height: 18px;
    color: ${theme.auth_page.footer.span.color};
  `
);

export const InformationTextStyled = styled.p(
  ({ theme }) => css`
    font-size: 12px;
    font-weight: ${theme.auth_page.footer.p.fontWeight};
    line-height: 16px;
    color: ${theme.auth_page.footer.p.color};
    text-align: center;
  `
);
