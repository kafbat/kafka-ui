import React, { ChangeEvent, useRef } from 'react';
import { RegisterOptions, useFormContext } from 'react-hook-form';
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

export interface InputWithOptionsProps {
  options: SelectOption[];
  onChange?: (option: string) => void;
  onValidChange?: (isValid: boolean) => void;
  name?: string;
  hookFormOptions?: RegisterOptions;
  inputSize?: 'S' | 'M' | 'L';
  placeholder?: string;
  minWidth?: string;
}

const InputWithOptions = ({
  options = [],
  onChange,
  onValidChange,
  name,
  hookFormOptions,
  inputSize = 'L',
  placeholder = '',
  ...rest
}: InputWithOptionsProps) => {
  const [query, setQuery] = React.useState('');
  const [showOptions, setShowOptions] = React.useState(false);

  const methods = useFormContext();
  const isHookFormField = !!name && !!methods.register;

  let filteredOptions = options.filter((option) =>
    option.value.includes(query.toLowerCase())
  );

  if (!filteredOptions.length && query) {
    filteredOptions = [{ value: query, label: query }];
  }

  const updateSelectedOption = (option: SelectOption) => {
    if (!option.disabled) {
      onChange?.(option.value);
      setQuery(option.value);
      setShowOptions(false);
    }
  };

  const selectContainerRef = useRef(null);
  const clickOutsideHandler = () => {
    const isDisabledOption = (optionText: string) =>
      options.some((option) => option.value === optionText && option.disabled);

    if (!isDisabledOption(query) && showOptions) {
      onChange?.(query);
    }

    setShowOptions(false);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  let inputOptions = { ...rest };
  if (isHookFormField) {
    // extend input options with react-hook-form options
    // if the field is a part of react-hook-form form
    inputOptions = {
      ...rest,
      ...methods.register(name, {
        ...hookFormOptions,
      }),
    };
  }

  return (
    <S.Wrapper inputSize={inputSize} ref={selectContainerRef}>
      <S.Input
        value={query}
        onFocus={() => setShowOptions(true)}
        onInput={(e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
        autoComplete="off"
        placeholder={placeholder}
        inputSize={inputSize}
        {...inputOptions}
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

InputWithOptions.displayName = 'InputWithOptions';

export default InputWithOptions;
