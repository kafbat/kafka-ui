import styled from 'styled-components';
import { LagTrend } from 'lib/consumerGroups';

export const LagContainer = styled.div<{ $lagTrend: LagTrend }>`
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme, $lagTrend }) => theme.lag[$lagTrend]};
`;
