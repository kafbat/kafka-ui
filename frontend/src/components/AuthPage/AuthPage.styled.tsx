import styled, { css } from 'styled-components';

export const AuthPageStyled = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    min-height: 100vh;
    background-color: ${theme.auth_page.backgroundColor};
    font-family: ${theme.auth_page.fontFamily};
    overflow-x: hidden;
  `
);
