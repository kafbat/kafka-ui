import React from 'react';
import FtsIcon from 'components/common/Icons/FtsIcon';
import styled from 'styled-components';
import Tooltip from 'components/common/Tooltip/Tooltip';
import ClusterContext from 'components/contexts/ClusterContext';

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
  const { ftsEnabled: isFtsFeatureEnabled } = React.useContext(ClusterContext);
  const { handleSwitch, isFtsEnabled } = useFts(resourceName);

  if (!isFtsFeatureEnabled) {
    return null;
  }

  return (
    <Tooltip
      value={
        <IconWrapper onClick={handleSwitch} active={isFtsEnabled}>
          <FtsIcon />
        </IconWrapper>
      }
      content="Apply full text search"
      placement="bottom"
      showTooltip
      fullWidth
    />
  );
};

export default Fts;
