import styled from 'styled-components';

export const Container = styled.div`
  display: flex;
  height: 24px;
  align-items: center;
  position: relative;
  padding-right: 8px;
`;

export const Positioner = styled.div`
  position: absolute;
  z-index: 30;
`;

export const Count = styled.span`
  padding-left: 4px;
  color: ${({ theme }) => theme.table.filter.multiSelect.value.color};
`;

export const FilterIcon = styled.div`
  height: 12px;
  margin: 2px;
`;

export const ResetIcon = styled.div`
  margin: 3px;
`;
