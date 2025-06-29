import React from 'react';
import KafbatFilterIcon from 'components/common/Icons/FilterIcon';
import styled, { css } from 'styled-components';

const StyledFilterIcon = styled.div`
  ${({ theme: { table } }) => css`
    color: ${table.filter.multiSelect.filterIcon.fill.normal};

    &:hover {
      color: ${table.filter.multiSelect.filterIcon.fill.hover};
    }
  `}
`;

function FilterIcon({ expanded }: { expanded: boolean }) {
  return (
    <StyledFilterIcon>
      <KafbatFilterIcon isOpen={expanded} />
    </StyledFilterIcon>
  );
}

export default styled(FilterIcon)``;
