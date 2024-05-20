import styled, { css } from 'styled-components';

export const HeaderStyled = styled.div`
  display: grid;
  grid-template-columns: repeat(47, 41.11px);
  grid-template-rows: repeat(4, 41.11px);
  justify-content: center;
  margin-bottom: 13.5px;
`;

export const HeaderCell = styled.div<{ $sections?: number }>(
  ({ theme, $sections }) => css`
    border: 1.23px solid ${theme.auth_page.header.cellBorderColor};
    border-radius: 75.98px;
    ${$sections && `grid-column: span ${$sections};`}
  `
);

export const StyledSVG = styled.svg`
  grid-column: span 3;
`;

export const StyledRect = styled.rect(
  ({ theme }) => css`
    fill: ${theme.auth_page.header.LogoBgColor};
  `
);

export const StyledPath = styled.path(
  ({ theme }) => css`
    fill: ${theme.auth_page.header.LogoTextColor};
  `
);
