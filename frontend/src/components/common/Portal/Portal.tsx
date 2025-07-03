import React, { FC, memo, PropsWithChildren } from 'react';
import { createPortal } from 'react-dom';

import * as S from './Portal.styled';

type PortalProps = {
  isOpen: boolean;
};

const Portal: FC<PropsWithChildren<PortalProps>> = memo(
  ({ isOpen, children }) => {
    const content = isOpen ? <S.Backdrop>{children}</S.Backdrop> : null;

    return createPortal(content, document.getElementById('portal-root')!);
  }
);

export default Portal;
