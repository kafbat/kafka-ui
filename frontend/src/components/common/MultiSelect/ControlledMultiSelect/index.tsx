import React, { FC } from 'react';
import MultiSelect from 'components/common/MultiSelect/MultiSelect.styled';
import { Controller, useFormContext } from 'react-hook-form';
import { Option } from 'react-multi-select-component';

type Props = {
  name: string;
  options: Option[];
  label?: string;
};
const ControlledMultiSelect: FC<Props> = ({ name, options, label }) => {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { value, onChange } }) => (
        <MultiSelect
          height="40px"
          options={options}
          value={value ?? []}
          onChange={onChange}
          labelledBy={label ?? ''}
        />
      )}
    />
  );
};

export default ControlledMultiSelect;
