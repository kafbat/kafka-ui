import styled, { css } from 'styled-components';
import * as SEditorViewer from 'components/common/EditorViewer/EditorViewer.styled';
import { Link } from 'react-router-dom';

export const Wrapper = styled.tr`
  background-color: ${({ theme }) => theme.topicMetaData.backgroundColor};
  & > td {
    padding: 16px;
    &:first-child {
      padding-right: 1px;
    }
    &:last-child {
      padding-left: 1px;
    }
  }
`;

export const Section = styled.div<{ $bounded?: boolean }>`
  padding: 0 16px;
  display: flex;
  gap: 1px;
  align-items: stretch;
  ${({ $bounded }) =>
    $bounded &&
    css`
      @media (max-width: 960px) {
        max-width: calc(100vw - 64px);
        flex-direction: column;
        gap: 16px;

        > div {
          min-width: 0;
          padding: 16px;
          border-radius: 4px;
        }
      }
    `}
`;

export const ContentBox = styled.div`
  background-color: ${({ theme }) => theme.topicMetaData.backgroundColor};
  padding: 24px;
  border-radius: 8px 0 0 8px;
  flex-grow: 3;
  min-width: 0;
  display: flex;
  flex-direction: column;
  & nav {
    padding-bottom: 16px;
  }
  ${SEditorViewer.Wrapper} {
    flex-grow: 1;
  }
`;
export const DataCell = styled.td`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 350px;
  min-width: 350px;
`;
export const ClickableRow = styled.tr<{ $truncated?: boolean }>`
  cursor: pointer;
  ${({ $truncated, theme }) =>
    $truncated &&
    css`
      background: ${theme.alert.color.warning};

      &&:hover {
        background: ${theme.alert.color.warning};
      }

      && > td {
        color: ${theme.alert.textColor.warning};
      }
    `}
`;

export const RowSummary = styled.div`
  min-width: 280px;
  white-space: normal;
  color: ${({ theme }) => theme.alert.textColor.warning};
`;

export const RowTitle = styled.div`
  font-weight: 600;
  line-height: 20px;
`;

export const RowDescription = styled.div`
  font-size: 12px;
  line-height: 18px;
  margin-top: 2px;
`;
export const MetadataWrapper = styled.div`
  background-color: ${({ theme }) => theme.topicMetaData.backgroundColor};
  padding: 24px;
  border-radius: 0 8px 8px 0;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 400px;
`;

export const Metadata = styled.span`
  display: flex;
  gap: 35px;
`;

export const MetadataLabel = styled.p`
  color: ${({ theme }) => theme.topicMetaData.color.label};
  font-size: 14px;
  width: 80px;
`;

export const MetadataValue = styled.div<{ $truncated?: boolean }>`
  color: ${({ theme, $truncated }) =>
    $truncated
      ? theme.alert.textColor.warning
      : theme.topicMetaData.color.value};
  font-size: 14px;
`;

export const MetadataMeta = styled.p`
  color: ${({ theme }) => theme.topicMetaData.color.meta};
  font-size: 12px;
`;

export const Tab = styled.button<{ $active?: boolean }>(
  ({ theme, $active }) => css`
    background-color: ${theme.secondaryTab.backgroundColor[
      $active ? 'active' : 'normal'
    ]};
    color: ${theme.secondaryTab.color[$active ? 'active' : 'normal']};
    padding: 6px 16px;
    height: 32px;
    border: 1px solid ${theme.layout.stuffBorderColor};
    cursor: pointer;
    &:hover {
      background-color: ${theme.secondaryTab.backgroundColor.hover};
      color: ${theme.secondaryTab.color.hover};
    }
    &:first-child {
      border-radius: 4px 0 0 4px;
    }
    &:last-child {
      border-radius: 0 4px 4px 0;
    }
    &:not(:last-child) {
      border-right: 0;
    }
  `
);

export const SchemaLink = styled(Link)`
  cursor: pointer;
  color: ${({ theme }) => theme.link.color};

  &:hover {
    color: ${({ theme }) => theme.link.hoverColor};
  }
`;

export const Tabs = styled.nav`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
`;

export const PlainPreview = styled.pre`
  background-color: ${({ theme }) => theme.viewer.wrapper.backgroundColor};
  color: ${({ theme }) => theme.viewer.wrapper.color};
  padding: 8px 16px;
  width: 100%;
  max-width: 100%;
  max-height: 532px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  min-width: 0;
`;
