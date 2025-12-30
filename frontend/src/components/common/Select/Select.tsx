import React, { useState, useRef, useEffect } from 'react';
import useClickOutside from 'lib/hooks/useClickOutside';
import DropdownArrowIcon from 'components/common/Icons/DropdownArrowIcon';

import * as S from './Select.styled';

export interface SelectProps<T> {
  options?: SelectOption<T>[];
  id?: string;
  name?: string;
  selectSize?: 'M' | 'L';
  minWidth?: string;
  value?: T;
  defaultValue?: T;
  placeholder?: string;
  disabled?: boolean;
  onChange?: (option: T) => void;
  isThemeMode?: boolean;
  formatSelectedOption?: (option: SelectOption<T>) => string;
}

export interface SelectOption<T> {
  label: string | number | React.ReactElement;
  value: T;
  disabled?: boolean;
}

// Use the generic type T for forwardRef
const Select = <T extends object>(
  {
    options = [],
    value,
    defaultValue,
    selectSize = 'L',
    placeholder = '',
    disabled = false,
    onChange,
    isThemeMode,
    formatSelectedOption,
    ...props
  }: SelectProps<T>,
  ref?: React.Ref<HTMLUListElement>
) => {
  const [selectedOption, setSelectedOption] = useState(value);
  const [showOptions, setShowOptions] = useState(false);

  useEffect(() => {
    setSelectedOption(value);
  }, [value]);

  const showOptionsHandler = () => {
    if (!disabled) setShowOptions(!showOptions);
  };

  const selectContainerRef = useRef(null);
  const clickOutsideHandler = () => setShowOptions(false);
  useClickOutside(selectContainerRef, clickOutsideHandler);

  const updateSelectedOption = (option: SelectOption<T>) => {
    if (!option.disabled) {
      setSelectedOption(option.value);

      if (onChange) {
        onChange(option.value);
      }

      setShowOptions(false);
    }
  };

  const displayedOption = options.find(
    (option) => option.value === (defaultValue || selectedOption)
  );

  // eslint-disable-next-line no-nested-ternary
  const displayedOptionLabel = displayedOption
    ? formatSelectedOption
      ? formatSelectedOption(displayedOption)
      : displayedOption.label
    : placeholder;

  return (
    <div ref={selectContainerRef}>
      <S.Select
        role="listbox"
        selectSize={selectSize}
        disabled={disabled}
        onClick={showOptionsHandler}
        onKeyDown={showOptionsHandler}
        isThemeMode={isThemeMode}
        ref={ref}
        tabIndex={0}
        {...props}
      >
        <S.SelectedOptionWrapper>
          <S.SelectedOption
            role="option"
            tabIndex={0}
            isThemeMode={isThemeMode}
          >
            {displayedOptionLabel}
          </S.SelectedOption>
        </S.SelectedOptionWrapper>
        {showOptions && (
          <S.OptionList>
            {options?.map((option) => (
              <S.Option
                value={option.value.toString()}
                key={option.value.toString()}
                disabled={option.disabled}
                onClick={() => updateSelectedOption(option)}
                tabIndex={0}
                role="option"
              >
                {option.label}
              </S.Option>
            ))}
          </S.OptionList>
        )}
        <DropdownArrowIcon isOpen={showOptions} />
      </S.Select>
    </div>
  );
};

Select.displayName = 'Select';

export default React.forwardRef(Select) as <T>(
  props: SelectProps<T> & React.RefAttributes<HTMLUListElement>
) => React.ReactElement;
