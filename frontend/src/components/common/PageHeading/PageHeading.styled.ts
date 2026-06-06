import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

export const Breadcrumbs = styled.div`
  display: flex;
  align-items: center;
  min-width: 0;
`;

export const BackLink = styled(NavLink)`
  color: ${({ theme }) => theme.pageHeading.backLink.color.normal};
  position: relative;

  &:hover {
    ${({ theme }) => theme.pageHeading.backLink.color.hover};
  }

  &::after {
    content: '';
    position: absolute;
    right: -11px;
    bottom: 2px;
    border-left: 1px solid ${({ theme }) => theme.pageHeading.dividerColor};
    height: 20px;
    transform: rotate(14deg);
  }
`;

export const Wrapper = styled.div`
  padding: 4px 0 18px;
`;

export const Content = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;

  & > div {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
  }

  & > ${Breadcrumbs} {
    gap: 20px;
  }

  h1 {
    font-size: 28px;
    line-height: 36px;
    font-weight: 700;
    letter-spacing: 0;
    color: ${({ theme }) => theme.surface.foreground};
  }

  @media screen and (max-width: 768px) {
    align-items: stretch;
    flex-direction: column;
  }
`;

export const Title = styled.div`
  color: ${({ theme }) => theme.pageHeading.title.color};
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  letter-spacing: 0;
  margin-bottom: 2px;

  & + ${Content} h1 {
    line-height: 34px;
  }
`;
