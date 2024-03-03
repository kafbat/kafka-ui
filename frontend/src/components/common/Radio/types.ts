import { PropsWithChildren } from 'react';

export type ActiveState = {
  background: string;
  color: string;
};

export type RadioOption = {
  value: string;
  activeState?: ActiveState;
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
