import { Button } from 'components/common/Button/Button';
import ChevronDownIcon from 'components/common/Icons/ChevronDownIcon';
import { Dropdown, DropdownItem } from 'components/common/Dropdown';
import React, { useMemo, useState } from 'react';
import Input from 'components/common/Input/Input';
import { TIMEZONES, useTimezone } from 'lib/hooks/useTimezones';

import * as S from './UserTimezone.styled';

export const UserTimezone = () => {
  const { currentTimezone, availableTimezones, setTimezone } = useTimezone();

  const [searchValue, setSearchValue] = useState('');

  const filteredTimezones = useMemo(() => {
    if (!searchValue.trim()) return availableTimezones;

    const searchLower = searchValue.toLowerCase();
    return TIMEZONES.filter(
      (timezone) =>
        timezone.value.toLowerCase().includes(searchLower) ||
        timezone.offset.toLowerCase().includes(searchLower) ||
        timezone.label.toLowerCase().includes(searchLower)
    );
  }, [searchValue]);

  const handleTimezoneSelect = (timezone: typeof currentTimezone) => {
    setTimezone(timezone);
    setSearchValue('');
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
  };

  return (
    <Dropdown
      onClose={() => setSearchValue('')}
      align="center"
      aria-label="user-timezone-dropdown"
      openBtnEl={
        <Button buttonType="text" buttonSize="L">
          <S.SelectedTimezoneContainer>
            <p>{currentTimezone.UTCOffset}</p>
            <ChevronDownIcon fill="currentColor" width="16" height="16" />
          </S.SelectedTimezoneContainer>
        </Button>
      }
    >
      <S.ContentContainer>
        <S.InputContainer>
          <Input
            id="user-timezone-search"
            type="text"
            placeholder="Search timezone..."
            value={searchValue}
            onChange={handleSearchChange}
            inputSize="M"
            search
          />
        </S.InputContainer>

        <S.ItemsContainer>
          {filteredTimezones.map((timezone) => (
            <DropdownItem
              key={timezone.value}
              onClick={() => handleTimezoneSelect(timezone)}
            >
              {timezone.label}
            </DropdownItem>
          ))}
        </S.ItemsContainer>
      </S.ContentContainer>
    </Dropdown>
  );
};
