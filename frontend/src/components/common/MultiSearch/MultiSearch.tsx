import React, { useRef, useState } from 'react';
import useClickOutside from 'lib/hooks/useClickOutside';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import SearchIcon from 'components/common/Icons/SearchIcon';

import * as S from './MultiSearch.styled';

export interface MultiSearchProps extends Omit<S.InputProps, 'onChange'> {
  name: string;
  value?: string;
  values?: string[];
  onChange?: (value: string, values: string[]) => void;
  inputSize?: 'S' | 'M' | 'L';
  placeholder?: string;
  searchIcon?: boolean;
}

const MAX_VISIBLE_TAGS = 1;

const MultiSearch: React.FC<MultiSearchProps> = ({
  name,
  value = '',
  values = [],
  onChange,
  inputSize = 'S',
  placeholder = '',
  searchIcon = true,
  ...rest
}) => {
  const [inputValue, setInputValue] = useState(value);
  const [showAllTags, setShowAllTags] = useState(false);

  const selectContainerRef = useRef(null);

  const handleKeyEnter = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && inputValue.trim()) {
      const trimmedValue = inputValue.trim();
      if (!values.includes(trimmedValue)) {
        const newValues = [...values, trimmedValue];
        if (onChange) {
          onChange('', newValues);
        }
      }
      setInputValue('');
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setInputValue(newValue);
    if (onChange) {
      onChange(newValue, values);
    }
  };

  const handleRemove = (valueToRemove: string) => {
    const newValues = values.filter((tagValue) => tagValue !== valueToRemove);
    if (onChange) {
      onChange(inputValue, newValues);
    }
  };

  const clearAll = () => {
    if (onChange) {
      onChange('', []);
    }
  };

  const handleFocus = () => {
    setShowAllTags(true);
  };

  const clickOutsideHandler = () => {
    setShowAllTags(false);
  };
  useClickOutside(selectContainerRef, clickOutsideHandler);

  const visibleTags = showAllTags ? values : values.slice(0, MAX_VISIBLE_TAGS);
  const remainingTagsCount = values.length - MAX_VISIBLE_TAGS;

  return (
    <S.Wrapper ref={selectContainerRef}>
      {searchIcon && <SearchIcon />}
      <S.ValuesContainer>
        {visibleTags.map((tagValue) => (
          <S.Tag key={tagValue}>
            {tagValue}
            <S.RemoveButton onClick={() => handleRemove(tagValue)}>
              <CloseCircleIcon />
            </S.RemoveButton>
          </S.Tag>
        ))}
        {!showAllTags && remainingTagsCount > 0 && (
          <S.RemainingTagCount>+{remainingTagsCount}</S.RemainingTagCount>
        )}
        <S.Input
          {...rest}
          value={inputValue}
          values={values}
          placeholder={values.length <= MAX_VISIBLE_TAGS ? placeholder : name}
          inputSize={inputSize}
          onChange={handleInputChange}
          onKeyDown={handleKeyEnter}
          onFocus={handleFocus}
          isFocused={showAllTags}
        />
      </S.ValuesContainer>
      <S.IconButtonWrapper onClick={clearAll}>
        <CloseCircleIcon />
      </S.IconButtonWrapper>
    </S.Wrapper>
  );
};

export default MultiSearch;
