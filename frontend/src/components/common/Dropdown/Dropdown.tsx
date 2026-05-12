import { MenuProps } from '@szhsin/react-menu';
import React, { cloneElement, PropsWithChildren, useRef } from 'react';
import VerticalElipsisIcon from 'components/common/Icons/VerticalElipsisIcon';
import useBoolean from 'lib/hooks/useBoolean';

import * as S from './Dropdown.styled';

interface DropdownProps extends PropsWithChildren<Partial<MenuProps>> {
  label?: React.ReactNode;
  disabled?: boolean;
  openBtnEl?: React.ReactElement;
  onClose?: () => void;
}

const Dropdown: React.FC<DropdownProps> = ({
  label,
  disabled,
  children,
  offsetY,
  openBtnEl,
  onClose,
  ...props
}) => {
  const ref = useRef(null);
  const { value: isOpen, setFalse, setTrue } = useBoolean(false);

  const handleClick: React.MouseEventHandler<HTMLButtonElement> = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setTrue();
  };

  return (
    <S.Wrapper>
      {openBtnEl ? (
        <span ref={ref} style={{ display: 'inline-block' }}>
          {cloneElement(openBtnEl, {
            onClick: handleClick,
            disabled,
            'aria-label': props['aria-label'] || 'Dropdown Toggle',
          })}
        </span>
      ) : (
        <S.DropdownButton
          onClick={handleClick}
          ref={ref}
          aria-label={props['aria-label'] || 'Dropdown Toggle'}
          disabled={disabled}
        >
          {label || (
            <S.SmallButton>
              <VerticalElipsisIcon />
            </S.SmallButton>
          )}
        </S.DropdownButton>
      )}

      <S.Dropdown
        anchorRef={ref}
        state={isOpen ? 'open' : 'closed'}
        onMouseLeave={setFalse}
        onClose={() => {
          setFalse();
          onClose?.();
        }}
        align={props.align || 'end'}
        direction={props.direction || 'bottom'}
        offsetY={offsetY ?? 10}
        viewScroll="auto"
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
        }}
      >
        {children}
      </S.Dropdown>
    </S.Wrapper>
  );
};

export default Dropdown;
