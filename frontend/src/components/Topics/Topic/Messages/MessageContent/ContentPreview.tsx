import React from 'react';
import EditorViewer from 'components/common/EditorViewer/EditorViewer';
import { Button } from 'components/common/Button/Button';
import {
  CONTENT_PREVIEW_LENGTH,
  previewEnd,
  previewChunkCount,
} from 'components/Topics/Topic/Messages/preview';
import * as Warning from 'components/Topics/Topic/Messages/Filters/Filters.styled';

import * as S from './MessageContent.styled';

interface Props {
  data: string;
  schemaType: string;
  truncated?: boolean;
  onDownload: () => void;
}

const ContentPreview: React.FC<Props> = ({
  data,
  schemaType,
  truncated = false,
  onDownload,
}) => {
  const [position, setPosition] = React.useState({ data, starts: [0] });
  const starts = position.data === data ? position.starts : [0];
  const start = starts[starts.length - 1];
  const end = previewEnd(
    data,
    Math.min(start + CONTENT_PREVIEW_LENGTH, data.length)
  );
  const chunkCount = React.useMemo(() => previewChunkCount(data), [data]);
  const formatNumber = (value: number) => value.toLocaleString('en-US');

  if (!truncated && data.length <= CONTENT_PREVIEW_LENGTH) {
    return <EditorViewer data={data} maxLines={28} schemaType={schemaType} />;
  }

  return (
    <>
      <Warning.Warning role="status">
        <Warning.WarningContent>
          <Warning.WarningTitle>Preview truncated</Warning.WarningTitle>
          <Warning.WarningDescription>
            {truncated
              ? 'Headers exceed the preview limit. Download to view them all.'
              : `Chunk ${starts.length} of ${chunkCount} visible. Preview range: ${formatNumber(start + 1)}–${formatNumber(end)} of ${formatNumber(data.length)}.`}
          </Warning.WarningDescription>
        </Warning.WarningContent>
        <Warning.WarningActions>
          {!truncated && (
            <>
              <Button
                buttonType="secondary"
                buttonSize="M"
                disabled={starts.length === 1}
                onClick={() =>
                  setPosition({ data, starts: starts.slice(0, -1) })
                }
              >
                Previous chunk
              </Button>
              <Button
                buttonType="primary"
                buttonSize="M"
                disabled={end >= data.length}
                onClick={() => setPosition({ data, starts: [...starts, end] })}
              >
                Show next chunk
              </Button>
            </>
          )}
          <Button buttonType="secondary" buttonSize="M" onClick={onDownload}>
            Download full message
          </Button>
        </Warning.WarningActions>
      </Warning.Warning>
      <S.PlainPreview aria-label="Large content preview">
        {data.slice(start, end)}
      </S.PlainPreview>
    </>
  );
};

export default ContentPreview;
