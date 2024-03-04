import { PropsWithChildren } from 'react';
import { ThemeType } from 'theme/theme';

export type ActiveState = {
  background: string;
  color: string;
};
export type RadioItemType = keyof ThemeType['acl']['create']['radioButtons'];

export type RadioOption = {
  value: string;
  itemType?: RadioItemType;
};

export interface RadioProps extends PropsWithChildren {
  value: string;
  options: ReadonlyArray<RadioOption>;
  onChange: (value: string) => void;
}

export interface ControlledRadioProps {
  name: string;
  options: ReadonlyArray<RadioOption>;
}
