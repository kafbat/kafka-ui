import React from 'react';
import { Dropdown, DropdownItem } from 'components/common/Dropdown';
import UserIcon from 'components/common/Icons/UserIcon';
import DropdownArrowIcon from 'components/common/Icons/DropdownArrowIcon';
import { useUserInfo } from 'lib/hooks/useUserInfo';

import * as S from './UserInfo.styled';

const UserInfo = () => {
  const { username } = useUserInfo();

  return username ? (
    <Dropdown
      label={
        <S.Wrapper>
          <UserIcon />
          <S.Text>{username}</S.Text>
          <DropdownArrowIcon isOpen={false} />
        </S.Wrapper>
      }
    >
      <DropdownItem href="/logout">Log out</DropdownItem>
    </Dropdown>
  ) : null;
};

export default UserInfo;
