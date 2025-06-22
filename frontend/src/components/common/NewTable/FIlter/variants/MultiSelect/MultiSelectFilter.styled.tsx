import styled from 'styled-components';
import { MultiSelect as ReactMultiSelect } from 'react-multi-select-component';

export const MultiSelect = styled(ReactMultiSelect)<{
  minWidth?: string;
  height?: string;
}>`
  padding-right: 12px;
  & > .dropdown-container {
    border: none !important;
    background-color: ${({ theme }) =>
      theme.input.backgroundColor.normal} !important;
  }

  & > .dropdown-container > .dropdown-content {
    width: fit-content;
    min-width: 120px;
    right: 0px;
  }

  & > .dropdown-container > .dropdown-content > .panel-content {
    padding: 8px;
  }
`;
