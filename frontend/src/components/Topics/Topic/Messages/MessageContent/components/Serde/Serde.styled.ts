import styled from 'styled-components';
import { Button } from 'components/common/Button/Button';

export const Metadata = styled.span`
  display: flex;
  gap: 35px;
`;

export const MetadataLabel = styled.p`
  color: ${({ theme }) => theme.topicMetaData.color.label};
  font-size: 14px;
  width: 80px;
`;

export const MetadataValue = styled.div`
  color: ${({ theme }) => theme.topicMetaData.color.value};
  font-size: 14px;
`;

export const MetadataMeta = styled.p`
  color: ${({ theme }) => theme.topicMetaData.color.meta};
  font-size: 12px;
`;

export const SchemaButton = styled(Button)`
  background-color: transparent;
  gap: 4px;
  padding: 0;
  &:hover {
    background-color: transparent;
  }
`;
