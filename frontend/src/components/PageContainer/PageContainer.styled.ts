import styled, { css } from 'styled-components';

export const Container = styled.main<{ $isSidebarVisible: boolean }>(
  ({ theme, $isSidebarVisible }) => css`
    margin-top: ${theme.layout.navBarHeight};
    margin-left: ${$isSidebarVisible ? theme.layout.navBarWidth : 0};
    position: relative;
    padding-bottom: 30px;
    z-index: 20;
    max-width: calc(
      100vw - ${$isSidebarVisible ? theme.layout.navBarWidth : 0}
    );
    transition: ${
      $isSidebarVisible
        ? 'max-width 0.75s ease-in-out'
        : 'margin-left 0.25s ease-in-out'
    }
  }

  @media screen and (max-width: 1024px) {
    margin-left: initial;
    max-width: 100vw;
  }
  `
);

export const Sidebar = styled.div<{ $visible: boolean }>(
  ({ theme, $visible }) => css`
    width: ${theme.layout.navBarWidth};
    display: flex;
    flex-direction: column;
    border-right: 1px solid ${theme.layout.stuffBorderColor};
    position: fixed;
    top: ${theme.layout.navBarHeight};
    left: 0;
    bottom: 0;
    padding: 16px;
    scrollbar-gutter: stable;
    scrollbar-width: thin;
    overflow-y: auto;
    transition:
      width 0.25s,
      opacity 0.25s,
      transform 0.25s,
      -webkit-transform 0.25s;
    background: ${theme.default.backgroundColor};
    transform: ${$visible ? 'translateX(0)' : 'translateX(-100%)'};

    @media screen and (max-width: 1024px) {
      ${$visible &&
      css`
        transform: translate3d(${theme.layout.navBarWidth}, 0, 0);
      `};
      left: -${theme.layout.navBarWidth};
      z-index: 1000;
    }

    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background-color: ${theme.scrollbar.trackColor.normal};
    }

    &::-webkit-scrollbar-thumb {
      width: 8px;
      background-color: ${theme.scrollbar.thumbColor.normal};
      border-radius: 4px;
    }

    &:hover::-webkit-scrollbar-thumb {
      background: ${theme.scrollbar.thumbColor.active};
    }

    &:hover::-webkit-scrollbar-track {
      background-color: ${theme.scrollbar.trackColor.active};
    }
  `
);

export const Overlay = styled.div<{ $visible: boolean }>(
  ({ theme, $visible }) => css`
    height: calc(100vh - ${theme.layout.navBarHeight});
    z-index: 99;
    visibility: hidden;
    opacity: 0;
    -webkit-transition: all 0.5s ease;
    transition: all 0.5s ease;
    left: 0;
    position: absolute;
    top: 0;
    ${$visible &&
    css`
      @media screen and (max-width: 1024px) {
        bottom: 0;
        right: 0;
        visibility: visible;
        opacity: 0.7;
        background-color: ${theme.layout.overlay.backgroundColor};
      }
    `}
  `
);
