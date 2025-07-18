import styled from 'styled-components';

export const SelectedTimezoneContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
`;

export const ContentContainer = styled.div`
  display: flex;
  width: 320px;
  flex-direction: column;
  max-height: 640px;
  gap: 8px;
`;

export const InputContainer = styled.div`
  position: sticky;
  top: 0;
  background-color: white;
  z-index: 1;
`;

export const ItemsContainer = styled.div`
  overflow-y: auto;
`;
