import styled from 'styled-components';

export const Wrapper = styled.div<{ $offsetY: number }>`
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  gap: 24px;
  height: calc(100vh - ${({ $offsetY }) => $offsetY}px);
`;

export const Status = styled.div`
  font-size: 100px;
  color: ${({ theme }) => theme.default.color.normal};
  line-height: initial;
`;

export const TextContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8;
`;

export const Title = styled.div`
  font-size: 18px;
  line-height: 28px;
  font-weight: 600;
  color: ${({ theme }) => theme.default.color.normal};
`;

export const Text = styled.div`
  font-size: 14px;
  line-height: 20px;
  font-weight: 400;
  color: ${({ theme }) => theme.default.color.normal};
`;
