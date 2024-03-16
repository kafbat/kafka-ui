import React, { PropsWithChildren } from 'react';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';

import * as S from './SlidingSidebar.styled';

interface SlidingSidebarProps extends PropsWithChildren<unknown> {
  open?: boolean;
  title: string;
  onClose?: () => void;
}

const SlidingSidebar: React.FC<SlidingSidebarProps> = ({
  open,
  title,
  children,
  onClose,
}) => {
  return (
    <S.Wrapper $open={open}>
      <S.Header>
        <S.HeaderText>{title}</S.HeaderText>
        <S.CloseIconButtonWrapper onClick={onClose} aria-label="edit">
          <CloseCircleIcon />
        </S.CloseIconButtonWrapper>
      </S.Header>
      <S.Content>{children}</S.Content>
    </S.Wrapper>
  );
};

export default SlidingSidebar;
