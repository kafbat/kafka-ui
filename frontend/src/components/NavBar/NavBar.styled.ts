import styled, { css } from 'styled-components';
import { Link } from 'react-router-dom';
import DiscordIcon from 'components/common/Icons/DiscordIcon';
import GitHubIcon from 'components/common/Icons/GitHubIcon';
import ProductHuntIcon from 'components/common/Icons/ProductHuntIcon';

export const Navbar = styled.nav(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid ${theme.layout.stuffBorderColor};
    box-shadow: ${theme.surface.shadow};
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 30;
    background-color: ${theme.surface.header};
    min-height: ${theme.layout.navBarHeight};
    padding: 0 18px;
    backdrop-filter: blur(18px);
  `
);

export const NavbarBrand = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  min-height: ${({ theme }) => theme.layout.navBarHeight};
`;

export const SocialLink = styled.a(
  ({ theme: { icons } }) => css`
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: 6px;
    cursor: pointer;
    fill: ${icons.discord.normal};
    transition:
      background-color 120ms ease,
      fill 120ms ease;

    &:hover {
      background-color: ${({ theme }) => theme.surface.muted};

      ${DiscordIcon} {
        fill: ${icons.discord.hover};
      }

      ${GitHubIcon} {
        fill: ${icons.github.hover};
      }

      ${ProductHuntIcon} {
        fill: ${icons.producthunt.hover};
      }
    }

    &:active {
      ${DiscordIcon} {
        fill: ${icons.discord.active};
      }

      ${GitHubIcon} {
        fill: ${icons.github.active};
      }

      ${ProductHuntIcon} {
        fill: ${icons.producthunt.active};
      }
    }
  `
);

export const NavbarSocial = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  @media screen and (max-width: 768px) {
    gap: 4px;

    & > a {
      display: none;
    }
  }
`;

export const NavbarItem = styled.div`
  display: flex;
  position: relative;
  flex-grow: 0;
  flex-shrink: 0;
  align-items: center;
  line-height: 1.5;
  padding: 0 0 0 4px;

  @media screen and (max-width: 900px) {
    display: none;
  }
`;

export const Hyperlink = styled(Link)(
  ({ theme }) => css`
    position: relative;

    display: flex;
    flex-grow: 0;
    flex-shrink: 0;
    align-items: center;
    gap: 8px;

    margin: 0;
    padding: 0.35rem 0.45rem;
    border-radius: 8px;

    font-family: Inter, sans-serif;
    font-style: normal;
    font-weight: 700;
    font-size: 18px;
    line-height: 20px;
    color: ${theme.surface.foreground};
    letter-spacing: 0;

    &:hover {
      color: ${theme.surface.foreground};
      background: ${theme.surface.muted};
    }

    text-decoration: none;
    word-break: break-word;
    cursor: pointer;
  `
);
