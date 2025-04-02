import React from 'react';
import { Dropdown } from 'components/common/Dropdown';
import ColorPickerIcon from 'components/common/Icons/ColorPickerIcon';
import { ClusterColorKey } from 'theme/theme';

import * as S from './MenuColorPicker.styled';

export interface MenuColorPickerProps {
  setColorKey: (key: ClusterColorKey) => void;
}

const COLOR_KEYS: ClusterColorKey[] = [
  'transparent',
  'gray',
  'red',
  'orange',
  'lettuce',
  'green',
  'turquoise',
  'blue',
  'violet',
  'pink',
];

const MenuColorPicker = ({ setColorKey }: MenuColorPickerProps) => {
  const handleCircleCLick = (colorKey: ClusterColorKey) => () => {
    setColorKey(colorKey);
  };

  return (
    <Dropdown
      aria-label="Color Picker Dropdown"
      offsetY={5}
      label={<ColorPickerIcon />}
    >
      <S.Container>
        {COLOR_KEYS.map((key) => (
          <S.ColorCircle
            data-testid={`color-circle-${key}`}
            onClick={handleCircleCLick(key)}
            $colorKey={key}
            key={key}
          />
        ))}
      </S.Container>
    </Dropdown>
  );
};

export default MenuColorPicker;
