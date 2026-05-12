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
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 30;
    background-color: ${theme.menu.header.backgroundColor};
    min-height: 3.25rem;
  `
);

export const NavbarBrand = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center !important;
  flex-shrink: 0;
  min-height: 3.25rem;
  padding-left: 8px;
`;

export const SocialLink = styled.a(
  ({ theme: { icons } }) => css`
    display: block;
    margin-top: 5px;
    cursor: pointer;
    fill: ${icons.discord.normal};

    &:hover {
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
  gap: 10px;
  margin: 5px 10px 5px;
`;

export const NavbarItem = styled.div`
  display: flex;
  position: relative;
  flex-grow: 0;
  flex-shrink: 0;
  align-items: center;
  line-height: 1.5;
  padding: 0.5rem 0.75rem;
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
    padding: 0.5rem 0.6rem;

    font-family: Inter, sans-serif;
    font-style: normal;
    font-weight: bold;
    font-size: 22px;
    line-height: 16px;
    color: ${theme.default.color.normal};

    &:hover {
      color: ${theme.default.color.normal};
    }

    text-decoration: none;
    word-break: break-word;
    cursor: pointer;
  `
);
