import React, {
  ComponentRef,
  ReactNode,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useDebouncedCallback } from 'use-debounce';
import Input from 'components/common/Input/Input';
import { useSearchParams } from 'react-router-dom';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';

import * as S from './Search.styled';

interface SearchProps {
  placeholder?: string;
  disabled?: boolean;
  onChange?: (value: string) => void;
  value?: string;
  extraActions?: ReactNode;
  debounceMs?: number;
}

const Search: React.FC<SearchProps> = ({
  placeholder = 'Search',
  disabled = false,
  value,
  onChange,
  extraActions,
  debounceMs = 500,
}) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const ref = useRef<ComponentRef<'input'>>(null);
  const [showIcon, setShowIcon] = useState(
    typeof value !== 'undefined' ? !!value : !!searchParams.get('q')
  );

  useEffect(() => {
    if (ref.current !== null && typeof value !== 'undefined') {
      ref.current.value = value;
    }
    setShowIcon(
      typeof value !== 'undefined' ? !!value : !!searchParams.get('q')
    );
  }, [searchParams, value]);

  const handleChange = useDebouncedCallback((nextValue: string) => {
    setShowIcon(!!nextValue);
    if (ref.current != null) {
      ref.current.value = nextValue;
    }
    if (onChange) {
      onChange(nextValue);
    } else {
      searchParams.set('q', nextValue);
      if (searchParams.get('page')) {
        searchParams.set('page', '1');
      }
      setSearchParams(searchParams);
    }
  }, debounceMs);

  const clearSearchValue = () => {
    if (onChange) {
      onChange('');
    } else if (searchParams.get('q')) {
      searchParams.set('q', '');
      setSearchParams(searchParams);
    }

    if (ref.current != null) {
      ref.current.value = '';
    }
    setShowIcon(false);
  };

  return (
    <Input
      type="text"
      placeholder={placeholder}
      onChange={({ target: { value: nextValue } }) => handleChange(nextValue)}
      defaultValue={value || searchParams.get('q') || ''}
      inputSize="M"
      disabled={disabled}
      ref={ref}
      search
      actions={
        <S.Actions>
          {showIcon && (
            <S.IconButtonWrapper
              onClick={clearSearchValue}
              data-testid="search-clear-button"
            >
              <CloseCircleIcon />
            </S.IconButtonWrapper>
          )}

          {extraActions}
        </S.Actions>
      }
    />
  );
};

export default Search;
