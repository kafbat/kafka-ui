import React, { FC } from 'react';
import { Controller, useFormContext } from 'react-hook-form';

import { ControlledRadioProps } from './types';
import { Radio } from './Radio';

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
            onChange={onChange}
            value={value ?? options[0].value}
          />
        );
      }}
    />
  );
};

export default ControlledRadio;
