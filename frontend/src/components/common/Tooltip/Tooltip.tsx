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
  fullWidth?: boolean;
}

const Tooltip: React.FC<TooltipProps> = ({
  value,
  content,
  placement,
  showTooltip = true,
  fullWidth,
}) => {
  const [open, setOpen] = useState(false);
  const { x, y, refs, strategy, context } = useFloating({
    open,
    onOpenChange: setOpen,
    placement,
  });
  const hover = useHover(context);
  const { getReferenceProps, getFloatingProps } = useInteractions([hover]);
  const isOpened = showTooltip && open;
  return (
    <>
      <div ref={refs.setReference} {...getReferenceProps()}>
        <S.Wrapper>{value}</S.Wrapper>
      </div>
      {isOpened && (
        <S.MessageTooltip
          ref={refs.setFloating}
          style={{
            position: strategy,
            top: y ?? 0,
            left: x ?? 0,
            width: 'max-content',
            minWidth: fullWidth ? 'max-content' : 'initial',
            display: 'flex',
            alignItems: 'center',
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
