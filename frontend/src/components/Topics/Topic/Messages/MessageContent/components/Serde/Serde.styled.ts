import styled from 'styled-components';
import { Link } from 'react-router-dom';

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

export const SchemaLink = styled(Link)`
  cursor: pointer;
  color: ${({ theme }) => theme.link.color};

  &:hover {
    color: ${({ theme }) => theme.link.hoverColor};
  }
`;
