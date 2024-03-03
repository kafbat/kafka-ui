import React, { FC } from 'react';
import { Controller, useFormContext } from 'react-hook-form';

import { RadioOption } from './types';
import { Radio } from './Radio';

type ControlledRadioProps = {
  name: string;
  options: ReadonlyArray<RadioOption>;
};

const ControlledRadio: FC<ControlledRadioProps> = ({ name, options }) => {
  const { control } = useFormContext();

  return (
    <Controller
      name={name}
      control={control}
      render={({ field: { onChange, value } }) => {
        return (
          <Radio
            options={options}
            onChange={(t) => {
              onChange(t);
            }}
            value={value ?? options[0].value}
          />
        );
      }}
    />
  );
};

export default ControlledRadio;
