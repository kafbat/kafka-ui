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
  inputSize = 'L',
  placeholder = '',
  minWidth,
  ...rest
}: InputWithOptionsProps) => {
  const [selectedOption, setSelectedOption] = React.useState(value);
  const [showOptions, setShowOptions] = React.useState(false);

  // A blank-valued option means "clear the selection". It never matches the
  // filter (''.includes(anything) is false), so keep it pinned to the top
  // instead of letting it disappear as soon as something is selected.
  const clearOptions = options.filter((option) => option.value === '');

  let matchedOptions = options.filter(
    (option) =>
      option.value !== '' && option.value.includes(selectedOption.toLowerCase())
  );

  if (!matchedOptions.length && selectedOption) {
    matchedOptions = [{ value: selectedOption, label: selectedOption }];
  }

  const filteredOptions = [...clearOptions, ...matchedOptions];

  const updateSelectedOption = (option: SelectOption<string>) => {
    if (!option.disabled) {
      setSelectedOption(option.value);
      onChange?.(option.value);
      setShowOptions(false);
    }
  };

  const selectContainerRef = useRef(null);
  const clickOutsideHandler = () => {
    const isDisabledOption = (optionText: string) =>
      options.some((option) => option.value === optionText && option.disabled);

    if (!isDisabledOption(value) && showOptions) {
      onChange?.(selectedOption);
    }

    setShowOptions(false);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  return (
    <S.Wrapper inputSize={inputSize} ref={selectContainerRef}>
      <S.Input
        {...rest}
        role="listitem"
        value={selectedOption}
        onFocus={() => setShowOptions(true)}
        autoComplete="off"
        placeholder={placeholder}
        inputSize={inputSize}
        onChange={(e) => {
          onChange?.(e.target.value);
          setSelectedOption(e.target.value);
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
