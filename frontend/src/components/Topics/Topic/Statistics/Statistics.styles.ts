import { List } from 'components/common/PropertiesList/PropertiesList.styled';
import styled from 'styled-components';

export const ProgressContainer = styled.div`
  padding: 1.5rem 1rem;
  background: ${({ theme }) => theme.code.backgroundColor};
  justify-content: center;
  align-items: center;
  display: flex;
  flex-direction: column;
  height: 300px;
  text-align: center;

  ${List} {
    opacity: 0.5;
  }
`;

export const ActionsBar = styled.div`
  display: flex;
  justify-content: end;
  gap: 8px;
  padding: 10px 20px;
  align-items: center;
`;

export const CreatedAt = styled.div`
  font-size: 12px;
  line-height: 1.5;
  color: ${({ theme }) => theme.statistics.createdAtColor};
`;

export const PartitionInfo = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  column-gap: 24px;
`;

export const ProgressBarWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 280px;
`;

export const ProgressPct = styled.span`
  font-size: 15px;
  font-weight: bold;
  line-height: 1.5;
  color: ${({ theme }) => theme.statistics.progressPctColor};
`;

export const LabelValue = styled.span`
  color: ${({ theme }) => theme.statistics.createdAtColor};
`;
