import React, { useRef } from 'react';
import useClickOutside from 'lib/hooks/useClickOutside';
import LiveIcon from 'components/common/Select/LiveIcon.styled';
import DropdownArrowIcon from 'components/common/Icons/DropdownArrowIcon';

import * as S from './InputWithOptions.styled';

export interface SelectOption {
  label: string | number | React.ReactElement;
  value: string;
  disabled?: boolean;
  isLive?: boolean;
}

export interface InputWithOptionsProps
  extends Omit<S.StyledInputProps, 'onChange'> {
  options: SelectOption[];
  onChange?: (option: string) => void;
  inputSize?: 'S' | 'M' | 'L';
  minWidth?: string;
  value: string;
}

const InputWithOptions = ({
  options = [],
  onChange,
  inputSize = 'L',
  placeholder = '',
  minWidth,
  value = '',
  ...rest
}: InputWithOptionsProps) => {
  // TODO maybe value should be debounced
  const [showOptions, setShowOptions] = React.useState(false);

  let filteredOptions = options.filter((option) =>
    option.value.includes(value.toLowerCase())
  );

  if (!filteredOptions.length && value) {
    filteredOptions = [{ value, label: value }];
  }

  const updateSelectedOption = (option: SelectOption) => {
    if (!option.disabled) {
      onChange?.(option.value);
      setShowOptions(false);
    }
  };

  const selectContainerRef = useRef(null);
  const clickOutsideHandler = () => {
    const isDisabledOption = (optionText: string) =>
      options.some((option) => option.value === optionText && option.disabled);

    if (!isDisabledOption(value) && showOptions) {
      onChange?.(value);
    }

    setShowOptions(false);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  return (
    <S.Wrapper inputSize={inputSize} ref={selectContainerRef}>
      <S.Input
        {...rest}
        value={value}
        onFocus={() => setShowOptions(true)}
        autoComplete="off"
        placeholder={placeholder}
        inputSize={inputSize}
        onChange={(e) => onChange?.(e.target.value)}
      />
      <DropdownArrowIcon isOpen={showOptions} />
      {showOptions && (
        <S.OptionList role="listbox" tabIndex={0}>
          {filteredOptions?.map((option) => (
            <S.Option
              role="option"
              value={option.value}
              key={option.value}
              disabled={option.disabled}
              onClick={() => updateSelectedOption(option)}
              tabIndex={0}
            >
              {option.isLive && <LiveIcon />}
              {option.label}
            </S.Option>
          ))}
        </S.OptionList>
      )}
    </S.Wrapper>
  );
};

export default InputWithOptions;
