import React, { FC, useEffect, useState } from 'react';

import * as S from './Radio.styled';
import { RadioProps } from './types';

export const Radio: FC<RadioProps> = ({ options, onChange, value }) => {
  const [selectedValue, setSelectedValue] = useState(value);

  useEffect(() => {
    onChange(value);
  });

  const handleChange = (v: string) => {
    setSelectedValue(v);
    onChange(v);
  };

  return (
    <S.Container>
      {options.map((tab) => (
        <S.Item
          key={tab.value}
          onClick={() => handleChange(tab.value)}
          $isActive={selectedValue === tab.value}
          $activeState={tab.activeState}
        >
          {tab.value}
        </S.Item>
      ))}
    </S.Container>
  );
};

export default Radio;
