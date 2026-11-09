import React from 'react';
import { isLosslessNumber } from 'lossless-json';

import * as S from './JsonTreeView.styled';

export interface JsonTreeViewProps {
  data: unknown;
  /**
   * Nodes at a depth below this value start expanded, everything at or
   * beyond it starts collapsed. Root is depth 0.
   */
  defaultExpandDepth?: number;
}

type Entry = [key: string | number, value: unknown];

const isContainer = (value: unknown): value is object | unknown[] =>
  typeof value === 'object' && value !== null && !isLosslessNumber(value);

const getEntries = (value: object | unknown[]): Entry[] =>
  Array.isArray(value)
    ? value.map((item, index): Entry => [index, item])
    : Object.entries(value);

const formatPrimitive = (value: unknown) => {
  if (isLosslessNumber(value)) {
    return { type: 'number', text: value.toString() } as const;
  }
  if (value === null) {
    return { type: 'null', text: 'null' } as const;
  }
  switch (typeof value) {
    case 'string':
      return { type: 'string', text: JSON.stringify(value) } as const;
    case 'number':
      return { type: 'number', text: String(value) } as const;
    case 'boolean':
      return { type: 'boolean', text: value ? 'true' : 'false' } as const;
    default:
      return { type: 'null', text: String(value) } as const;
  }
};

interface JsonTreeNodeProps {
  nodeKey?: string | number;
  value: unknown;
  depth: number;
  path: string;
  isLast: boolean;
  defaultExpandDepth: number;
}

const NodeKey: React.FC<{ nodeKey?: string | number }> = ({ nodeKey }) => {
  if (nodeKey === undefined) {
    return null;
  }
  return (
    <>
      <S.Key>{typeof nodeKey === 'number' ? nodeKey : `"${nodeKey}"`}</S.Key>
      <S.Punctuation>: </S.Punctuation>
    </>
  );
};

const JsonTreeNode: React.FC<JsonTreeNodeProps> = ({
  nodeKey,
  value,
  depth,
  path,
  isLast,
  defaultExpandDepth,
}) => {
  const [isOpen, setIsOpen] = React.useState(depth < defaultExpandDepth);

  if (!isContainer(value)) {
    const { type, text } = formatPrimitive(value);
    const ValueComponent =
      // eslint-disable-next-line no-nested-ternary
      type === 'string'
        ? S.StringValue
        : type === 'number'
          ? S.NumberValue
          : S.Muted;

    return (
      <S.Row>
        <S.Indent $depth={depth} />
        <NodeKey nodeKey={nodeKey} />
        <ValueComponent>{text}</ValueComponent>
        {!isLast && <S.Punctuation>,</S.Punctuation>}
      </S.Row>
    );
  }

  const entries = getEntries(value);
  const isArray = Array.isArray(value);
  const openBracket = isArray ? '[' : '{';
  const closeBracket = isArray ? ']' : '}';
  const itemLabel = entries.length === 1 ? 'item' : 'items';

  if (entries.length === 0) {
    return (
      <S.Row>
        <S.Indent $depth={depth} />
        <NodeKey nodeKey={nodeKey} />
        <S.Punctuation>
          {openBracket}
          {closeBracket}
        </S.Punctuation>
        {!isLast && <S.Punctuation>,</S.Punctuation>}
      </S.Row>
    );
  }

  return (
    <>
      <S.Row>
        <S.ToggleRow
          type="button"
          aria-expanded={isOpen}
          aria-label={`${isOpen ? 'Collapse' : 'Expand'} ${nodeKey ?? 'root'}`}
          onClick={() => setIsOpen((prev) => !prev)}
        >
          <S.Indent $depth={depth}>
            <S.Arrow $isOpen={isOpen}>▶</S.Arrow>
          </S.Indent>
          <NodeKey nodeKey={nodeKey} />
          <S.Punctuation>{openBracket}</S.Punctuation>
          {!isOpen && (
            <S.Muted>
              {' '}
              … {entries.length} {itemLabel}{' '}
            </S.Muted>
          )}
          {!isOpen && <S.Punctuation>{closeBracket}</S.Punctuation>}
          {!isOpen && !isLast && <S.Punctuation>,</S.Punctuation>}
        </S.ToggleRow>
      </S.Row>
      {isOpen && (
        <>
          {entries.map(([entryKey, entryValue], index) => (
            <JsonTreeNode
              key={`${path}.${entryKey}`}
              nodeKey={entryKey}
              value={entryValue}
              depth={depth + 1}
              path={`${path}.${entryKey}`}
              isLast={index === entries.length - 1}
              defaultExpandDepth={defaultExpandDepth}
            />
          ))}
          <S.Row>
            <S.Indent $depth={depth} />
            <S.Punctuation>{closeBracket}</S.Punctuation>
            {!isLast && <S.Punctuation>,</S.Punctuation>}
          </S.Row>
        </>
      )}
    </>
  );
};

const JsonTreeView: React.FC<JsonTreeViewProps> = ({
  data,
  defaultExpandDepth = 2,
}) => {
  return (
    <S.Wrapper>
      <JsonTreeNode
        value={data}
        depth={0}
        path="root"
        isLast
        defaultExpandDepth={defaultExpandDepth}
      />
    </S.Wrapper>
  );
};

export default JsonTreeView;
