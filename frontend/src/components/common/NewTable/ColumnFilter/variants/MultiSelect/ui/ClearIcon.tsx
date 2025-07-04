import React from 'react';
import styled from 'styled-components';
import CloseIcon from 'components/common/Icons/CloseIcon';

const StyledCloseIcon = styled(CloseIcon)`
  height: 9px;
  width: 9px;

  &:hover {
    fill: ${({ theme }) => theme.icons.closeIcon.hover};
  }
`;

function ClearIcon() {
  return <StyledCloseIcon />;
}

export default styled(ClearIcon)``;
