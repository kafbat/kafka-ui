import React, { useRef, useEffect, useState } from 'react';
import useClickOutside from 'lib/hooks/useClickOutside';
import { SelectOption } from 'components/common/Select/Select';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import SearchIcon from 'components/common/Icons/SearchIcon';

import * as S from './SearchAutocomplete.styled';

export interface SearchAutocompleteProps
  extends Omit<S.StyledInputProps, 'onChange'> {
  options: SelectOption<string>[];
  value?: string;
  onChange?: (option: string) => void;
  inputSize?: 'S' | 'M' | 'L';
  minWidth?: string;
  placeholder?: string;
  searchIcon?: boolean;
}

const SearchAutocomplete: React.FC<SearchAutocompleteProps> = ({
  options = [],
  value = '',
  onChange,
  inputSize = 'M',
  placeholder = '',
  minWidth,
  searchIcon = true,
  ...rest
}) => {
  const [inputValue, setInputValue] = useState(value);
  const [showOptions, setShowOptions] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState<number>(0);

  const selectContainerRef = useRef(null);
  const optionListRef = useRef<HTMLUListElement>(null);

  const clickOutsideHandler = () => {
    const isDisabledOption = (optionText: string) =>
      options.some((option) => option.value === optionText && option.disabled);

    if (!isDisabledOption(value) && showOptions) {
      onChange?.(inputValue);
    }

    setShowOptions(false);
    setHighlightedIndex(0);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  const filteredOptions = options.filter((option) =>
    option?.label?.toString().toLowerCase().includes(inputValue.toLowerCase())
  );

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
    setShowOptions(true);
    if (onChange) {
      onChange(e.target.value);
    }
  };

  const handleOptionClick = (option: SelectOption<string>, index: number) => {
    setInputValue(option?.value);
    setShowOptions(false);
    if (onChange) {
      onChange(option.value);
    }
    setHighlightedIndex(index);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex((prev) =>
        Math.min(prev + 1, filteredOptions.length - 1)
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex((prev) => Math.max(prev - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (highlightedIndex >= 0 && highlightedIndex < filteredOptions.length) {
        handleOptionClick(filteredOptions[highlightedIndex], highlightedIndex);
      }
    }
  };

  useEffect(() => {
    if (optionListRef.current && highlightedIndex >= 0) {
      const option = optionListRef.current.children[
        highlightedIndex
      ] as HTMLElement;
      if (option) {
        const optionRect = option.getBoundingClientRect();
        const listRect = optionListRef.current.getBoundingClientRect();
        if (optionRect.bottom > listRect.bottom) {
          optionListRef.current.scrollTop +=
            optionRect.bottom - listRect.bottom;
        } else if (optionRect.top < listRect.top) {
          optionListRef.current.scrollTop -= listRect.top - optionRect.top;
        }
      }
    }
  }, [highlightedIndex]);

  const clearSearchValue = () => {
    setInputValue('');
    setShowOptions(false);
    setHighlightedIndex(0);
    if (onChange) {
      onChange('');
    }
  };

  return (
    <S.Wrapper ref={selectContainerRef}>
      {searchIcon && <SearchIcon />}
      <S.Input
        {...rest}
        role="listitem"
        value={inputValue}
        onFocus={() => setShowOptions(true)}
        autoComplete="off"
        placeholder={placeholder}
        inputSize={inputSize}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
      />
      {showOptions && (
        <S.OptionList role="listbox" ref={optionListRef}>
          {filteredOptions.length > 0 ? (
            filteredOptions.map((option, index) => (
              <S.Option
                role="option"
                key={option.value}
                disabled={option.disabled}
                isHighlighted={index === highlightedIndex}
                onClick={() => handleOptionClick(option, index)}
              >
                {option.label}
              </S.Option>
            ))
          ) : (
            <S.Option disabled>No results</S.Option>
          )}
        </S.OptionList>
      )}

      <S.IconButtonWrapper onClick={clearSearchValue}>
        <CloseCircleIcon />
      </S.IconButtonWrapper>
    </S.Wrapper>
  );
};

export default SearchAutocomplete;
