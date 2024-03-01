import React from 'react';

import * as S from './Logo.styled';

const Logo: React.FC = () => {
  return (
    <S.Logo
      width="23"
      height="30"
      viewBox="0 0 23 30"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M1.9874 0.5C1.9874 0.5 7.7172 8.91974 17.9098 12.9622C17.9098 12.9622 24.7102 7.0943 18.1349 15.5305C11.5596 23.9666 1.54861 29.0758 1.9874 28.4481C3.29229 26.5813 7.59605 23.014 5.3963 20.8139C9.71432 16.495 0 13.6477 0 13.6477C2.51731 8.02801 1.9874 0.5 1.9874 0.5Z" />
    </S.Logo>
  );
};

export default Logo;
