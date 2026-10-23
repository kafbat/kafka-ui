import React from 'react';
import Editor from 'components/common/Editor/Editor';
import JsonTreeView from 'components/common/JsonTreeView/JsonTreeView';
import { SchemaType } from 'generated-sources';
import { parse, stringify } from 'lossless-json';

import * as S from './EditorViewer.styled';

export interface EditorViewerProps {
  data: string;
  schemaType?: string;
  maxLines?: number;
}

type ViewMode = 'code' | 'tree';

const isTreeableType = (schemaType?: string) =>
  schemaType === SchemaType.JSON || schemaType === SchemaType.AVRO;

const EditorViewer: React.FC<EditorViewerProps> = ({
  data,
  schemaType,
  maxLines,
}) => {
  const [viewMode, setViewMode] = React.useState<ViewMode>('code');

  try {
    const isTreeable = isTreeableType(schemaType);
    const parsedData = isTreeable ? parse(data) : undefined;
    const codeValue = isTreeable
      ? stringify(parsedData, undefined, '\t')
      : data;
    const canShowTree = isTreeable && parsedData !== undefined;

    return (
      <S.Wrapper>
        {canShowTree && (
          <S.ViewToggle>
            <S.ViewToggleButton
              type="button"
              $active={viewMode === 'code'}
              onClick={() => setViewMode('code')}
            >
              Code
            </S.ViewToggleButton>
            <S.ViewToggleButton
              type="button"
              $active={viewMode === 'tree'}
              onClick={() => setViewMode('tree')}
            >
              Tree
            </S.ViewToggleButton>
          </S.ViewToggle>
        )}
        {canShowTree && viewMode === 'tree' ? (
          <JsonTreeView data={parsedData} />
        ) : (
          <Editor
            isFixedHeight
            schemaType={schemaType}
            name="schema"
            value={codeValue}
            setOptions={{
              showLineNumbers: false,
              maxLines,
              showGutter: false,
            }}
            readOnly
          />
        )}
      </S.Wrapper>
    );
  } catch {
    return (
      <S.Wrapper>
        <p>{data}</p>
      </S.Wrapper>
    );
  }
};

export default EditorViewer;
