import styled from 'styled-components';

export const Wrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 5px;

  svg {
    position: relative;
  }
`;

export const Text = styled.div`
  color: ${({ theme }) => theme.user.color};

  &:hover {
    color: ${({ theme }) => theme.user.hoverColor};
  }
`;
