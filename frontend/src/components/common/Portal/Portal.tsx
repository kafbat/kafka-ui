import React, { FC, memo, PropsWithChildren } from 'react';
import { createPortal } from 'react-dom';

import * as S from './Portal.styled';

type PortalProps = {
  isOpen: boolean;
};

const Portal: FC<PropsWithChildren<PortalProps>> = memo(
  ({ isOpen, children }) => {
    const target = document.getElementById('portal-root');

    if (target == null) {
      return null;
    }

    return isOpen
      ? createPortal(<S.Backdrop>{children}</S.Backdrop>, target)
      : null;
  }
);

export default Portal;
