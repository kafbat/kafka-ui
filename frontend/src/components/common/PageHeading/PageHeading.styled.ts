import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

export const Breadcrumbs = styled.div`
  display: flex;
  align-items: baseline;
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
  padding: 16px;
`;

export const Content = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  & > div {
    display: flex;
    gap: 16px;
  }

  & > ${Breadcrumbs} {
    gap: 20px;
  }
`;

export const Title = styled.div`
  color: ${({ theme }) => theme.pageHeading.title.color};
  font-weight: 500;
  line-height: 8px;
  & + ${Content} h1 {
    padding-top: 8px;
    line-height: 24px;
  }
`;
