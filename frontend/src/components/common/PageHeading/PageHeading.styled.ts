import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

export const Breadcrumbs = styled.div`
  display: flex;
  align-items: baseline;
`;

export const ClusterTitle = styled.text`
  color: ${({ theme }) => theme.pageHeading.backLink.color.disabled};
  position: relative;
`;

export const BackLink = styled(NavLink)`
  color: ${({ theme }) => theme.pageHeading.backLink.color.normal};
  position: relative;

  &:hover {
    ${({ theme }) => theme.pageHeading.backLink.color.hover};
  }
`;

export const Slash = styled.text`
  color: ${({ theme }) => theme.pageHeading.backLink.color.disabled};
  position: relative;
  margin: 0 8px;
`;

export const Wrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;

  & > div {
    display: flex;
    gap: 16px;
  }

  & > ${Breadcrumbs} {
    gap: 20px;
  }
`;
