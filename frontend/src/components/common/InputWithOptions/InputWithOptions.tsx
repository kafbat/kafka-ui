import React, { useRef } from 'react';
import useClickOutside from 'lib/hooks/useClickOutside';
import DropdownArrowIcon from 'components/common/Icons/DropdownArrowIcon';
import { SelectOption } from 'components/common/Select/Select';

import * as S from './InputWithOptions.styled';

export interface InputWithOptionsProps extends Omit<
  S.StyledInputProps,
  'onChange'
> {
  options: SelectOption<string>[];
  value?: string;
  onChange?: (option: string) => void;
  inputSize?: 'S' | 'M' | 'L';
  minWidth?: string;
}

const InputWithOptions = ({
  options = [],
  value = '',
  onChange,
  onBlur,
  onKeyDown,
  inputSize = 'L',
  placeholder = '',
  minWidth,
  ...rest
}: InputWithOptionsProps) => {
  const [selectedOption, setSelectedOption] = React.useState(value);
  const [showOptions, setShowOptions] = React.useState(false);
  const editStartValue = useRef(value);
  const isInputEditing = useRef(false);

  let filteredOptions = options.filter((option) =>
    option.value.includes(selectedOption.toLowerCase())
  );

  if (!filteredOptions.length && selectedOption) {
    filteredOptions = [{ value: selectedOption, label: selectedOption }];
  }

  const isDisabledOption = (optionText: string) =>
    options.some((option) => option.value === optionText && option.disabled);

  const commitInput = () => {
    if (!isInputEditing.current) return;

    isInputEditing.current = false;
    if (isDisabledOption(selectedOption)) {
      setSelectedOption(editStartValue.current);
      onChange?.(editStartValue.current);
    } else if (!isDisabledOption(value) && showOptions) {
      onChange?.(selectedOption);
      editStartValue.current = selectedOption;
    }
  };

  const updateSelectedOption = (option: SelectOption<string>) => {
    if (!option.disabled) {
      setSelectedOption(option.value);
      onChange?.(option.value);
      editStartValue.current = option.value;
      isInputEditing.current = false;
      setShowOptions(false);
    }
  };

  const selectContainerRef = useRef(null);
  const clickOutsideHandler = () => {
    commitInput();
    setShowOptions(false);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  return (
    <S.Wrapper inputSize={inputSize} ref={selectContainerRef}>
      <S.Input
        {...rest}
        role="listitem"
        value={selectedOption}
        onFocus={() => {
          if (!showOptions) {
            editStartValue.current = value;
          }
          isInputEditing.current = true;
          setShowOptions(true);
        }}
        autoComplete="off"
        placeholder={placeholder}
        inputSize={inputSize}
        onBlur={(event) => {
          if (
            !event.currentTarget.parentElement?.contains(
              event.relatedTarget as Node | null
            )
          ) {
            commitInput();
          }
          onBlur?.(event);
        }}
        onKeyDown={(event) => {
          onKeyDown?.(event);
          if (event.key === 'Enter') {
            commitInput();
            setShowOptions(false);
          }
        }}
        onChange={(e) => {
          const nextValue = e.target.value;
          setSelectedOption(nextValue);
          if (!isDisabledOption(nextValue)) {
            onChange?.(nextValue);
          }
        }}
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
              {option.label}
            </S.Option>
          ))}
        </S.OptionList>
      )}
    </S.Wrapper>
  );
};

export default InputWithOptions;
