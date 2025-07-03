import React from 'react';
import KafbatFilterIcon from 'components/common/Icons/FilterIcon';
import styled, { css } from 'styled-components';

const StyledFilterIcon = styled.div<{ $active: boolean }>`
  ${({ $active, theme: { table } }) => css`
    color: ${$active
      ? table.filter.multiSelect.filterIcon.fill.active
      : table.filter.multiSelect.filterIcon.fill.normal};

    &:hover {
      color: ${table.filter.multiSelect.filterIcon.fill.hover};
    }
  `}
`;

function FilterIcon({ active }: { active: boolean }) {
  return (
    <StyledFilterIcon $active={active}>
      <KafbatFilterIcon />
    </StyledFilterIcon>
  );
}

export default styled(FilterIcon)``;
