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
}

const Search: React.FC<SearchProps> = ({
  placeholder = 'Search',
  disabled = false,
  value,
  onChange,
  extraActions,
}) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const ref = useRef<ComponentRef<'input'>>(null);
  const [showIcon, setShowIcon] = useState(!!value || !!searchParams.get('q'));

  useEffect(() => {
    // Only update input if uncontrolled and searchParams 'q' is different
    if (onChange && value !== undefined) return;

    const qParam = searchParams.get('q') || '';
    if (ref.current !== null && ref.current.value !== qParam) {
      ref.current.value = qParam;
    }
  }, [searchParams, onChange, value]);

  const handleChange = useDebouncedCallback((e) => {
    setShowIcon(!!e.target.value);
    if (ref.current != null) {
      ref.current.value = e.target.value;
    }
    if (onChange) {
      onChange(e.target.value);
    } else {
      searchParams.set('q', e.target.value);
      if (searchParams.get('page')) {
        searchParams.set('page', '1');
      }
      setSearchParams(searchParams);
    }
  }, 500);

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
    if (onChange) {
      onChange('');
    }
    setShowIcon(false);
  };

  return (
    <Input
      type="text"
      placeholder={placeholder}
      onChange={handleChange}
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
