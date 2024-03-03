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
      {options.map((option) => (
        <S.Item
          key={option.value}
          onClick={() => handleChange(option.value)}
          $isActive={selectedValue === option.value}
          $activeState={option.activeState}
        >
          {option.value}
        </S.Item>
      ))}
    </S.Container>
  );
};

export default Radio;
