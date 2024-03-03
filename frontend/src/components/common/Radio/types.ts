export type ActiveColor = {
  background: string;
  color: string;
};

export type RadioOption = {
  value: string;
  activeColor?: ActiveColor;
};

export type RadioProps = {
  value: string;
  options: ReadonlyArray<RadioOption>;
  onChange: (value: string) => void;
};

export interface ControlledRadioProps {
  name: string;
  options: ReadonlyArray<RadioOption>;
}
