import styled, { css } from 'styled-components';

export const AuthPageStyled = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    min-height: 100vh;
    background:
      linear-gradient(180deg, ${theme.surface.panelAlt} 0, transparent 360px),
      ${theme.surface.canvas};
    font-family: ${theme.auth_page.fontFamily};
    overflow-x: hidden;
    color: ${theme.surface.foreground};
  `
);
