import React from 'react';
import FtsIcon from 'components/common/Icons/FtsIcon';
import styled from 'styled-components';

import useFts, { FtsAvailableResource } from './useFts';

export const IconWrapper = styled.span.attrs<{ active: boolean }>(() => ({
  role: 'button',
  tabIndex: 1,
}))`
  display: inline-block;
  &:hover {
    cursor: pointer;
  }
  color: ${(props) =>
    props.active
      ? props.theme.icons.ftsIcon.active
      : props.theme.icons.ftsIcon.normal};
`;

const Fts = ({ resourceName }: { resourceName: FtsAvailableResource }) => {
  const { handleSwitch, isFtsEnabled } = useFts(resourceName);

  return (
    <IconWrapper onClick={handleSwitch} active={isFtsEnabled}>
      <FtsIcon />
    </IconWrapper>
  );
};

export default Fts;
