import React, { useState } from 'react';
import {
  Placement,
  useFloating,
  useHover,
  useInteractions,
} from '@floating-ui/react';

import * as S from './Tooltip.styled';

interface TooltipProps {
  value: React.ReactNode;
  content: string;
  placement?: Placement;
  showTooltip?: boolean;
}

const Tooltip: React.FC<TooltipProps> = ({
  value,
  content,
  placement,
  showTooltip = true,
}) => {
  const [open, setOpen] = useState(false);
  const { x, y, refs, strategy, context } = useFloating({
    open,
    onOpenChange: setOpen,
    placement,
  });
  const hover = useHover(context);
  const { getReferenceProps, getFloatingProps } = useInteractions([hover]);
  return (
    <>
      <div ref={refs.setReference} {...getReferenceProps()}>
        <S.Wrapper>{value}</S.Wrapper>
      </div>
      {showTooltip && open && (
        <S.MessageTooltip
          ref={refs.setFloating}
          style={{
            position: strategy,
            top: y ?? 0,
            left: x ?? 0,
            width: 'max-content',
          }}
          {...getFloatingProps()}
        >
          {content}
        </S.MessageTooltip>
      )}
    </>
  );
};

export default Tooltip;
