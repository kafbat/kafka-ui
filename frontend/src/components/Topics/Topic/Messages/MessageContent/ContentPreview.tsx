import React from 'react';
import EditorViewer from 'components/common/EditorViewer/EditorViewer';
import { Button } from 'components/common/Button/Button';
import {
  CONTENT_PREVIEW_LENGTH,
  previewEnd,
} from 'components/Topics/Topic/Messages/preview';

import * as S from './MessageContent.styled';

interface Props {
  data: string;
  schemaType: string;
  truncated?: boolean;
}

const ContentPreview: React.FC<Props> = ({
  data,
  schemaType,
  truncated = false,
}) => {
  const [position, setPosition] = React.useState({ data, starts: [0] });
  const starts = position.data === data ? position.starts : [0];
  const start = starts[starts.length - 1];
  const end = previewEnd(
    data,
    Math.min(start + CONTENT_PREVIEW_LENGTH, data.length)
  );

  if (!truncated && data.length <= CONTENT_PREVIEW_LENGTH) {
    return <EditorViewer data={data} maxLines={28} schemaType={schemaType} />;
  }

  return (
    <>
      <p role="status">
        Large content is shown as a bounded plain-text preview without
        formatting.
        {truncated
          ? ' Headers preview truncated. Download full content to inspect all headers.'
          : ` Showing characters ${start + 1}-${end} of ${data.length}.`}
      </p>
      <S.PlainPreview aria-label="Large content preview">
        {data.slice(start, end)}
      </S.PlainPreview>
      {!truncated && (
        <S.PreviewActions>
          <Button
            buttonType="secondary"
            buttonSize="M"
            disabled={starts.length === 1}
            onClick={() => setPosition({ data, starts: starts.slice(0, -1) })}
          >
            Previous preview
          </Button>
          <Button
            buttonType="secondary"
            buttonSize="M"
            disabled={end >= data.length}
            onClick={() => setPosition({ data, starts: [...starts, end] })}
          >
            Show more
          </Button>
        </S.PreviewActions>
      )}
    </>
  );
};

export default ContentPreview;
