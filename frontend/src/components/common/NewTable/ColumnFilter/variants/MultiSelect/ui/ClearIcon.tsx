import React from 'react';
import styled from 'styled-components';
import CloseIcon from 'components/common/Icons/CloseIcon';

const StyledCloseIcon = styled(CloseIcon)`
  margin: 0px 4px;
  height: 12px;
  width: 12px;

  &:hover {
    fill: ${({ theme }) => theme.icons.closeIcon.hover};
  }
`;

function ClearIcon() {
  return <StyledCloseIcon />;
}

export default styled(ClearIcon)``;
