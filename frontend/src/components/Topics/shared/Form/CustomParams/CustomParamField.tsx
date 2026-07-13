import React, { useRef } from 'react';
import { ErrorMessage } from '@hookform/error-message';
import { TOPIC_CUSTOM_PARAMS } from 'lib/constants';
import { FieldArrayWithId, useFormContext, Controller } from 'react-hook-form';
import { InputLabel } from 'components/common/Input/InputLabel.styled';
import { FormError } from 'components/common/Input/Input.styled';
import Input from 'components/common/Input/Input';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import * as C from 'components/Topics/shared/Form/TopicForm.styled';
import { ConfigSource } from 'generated-sources';
import InputWithOptions from 'components/common/InputWithOptions/InputWithOptions';
import { TopicConfigParams, TopicFormData } from 'lib/interfaces/topic';

import * as S from './CustomParams.styled';

export interface Props {
  config?: TopicConfigParams;
  isDisabled: boolean;
  index: number;
  existingFields: string[];
  field: FieldArrayWithId<TopicFormData, 'customParams', 'id'>;
  remove: (index: number) => void;
  setExistingFields: React.Dispatch<React.SetStateAction<string[]>>;
}

const CustomParamField: React.FC<Props> = ({
  field,
  isDisabled,
  index,
  remove,
  config,
  existingFields,
  setExistingFields,
}) => {
  const {
    formState: { errors },
    setValue,
    watch,
    trigger,
    control,
  } = useFormContext<TopicFormData>();
  const nameValue = watch(`customParams.${index}.name`);
  const prevName = useRef(nameValue);

  const options = Object.keys(TOPIC_CUSTOM_PARAMS)
    .sort()
    .map((option) => ({
      value: option,
      label: option,
      disabled:
        (config &&
          config[option]?.source !== ConfigSource.DYNAMIC_TOPIC_CONFIG) ||
        existingFields.includes(option),
    }));

  React.useEffect(() => {
    if (nameValue !== prevName.current) {
      let newExistingFields = [...existingFields];
      if (prevName.current) {
        newExistingFields = newExistingFields.filter(
          (name) => name !== prevName.current
        );
      }
      prevName.current = nameValue;
      newExistingFields.push(nameValue);
      setExistingFields(newExistingFields);
      setValue(`customParams.${index}.value`, TOPIC_CUSTOM_PARAMS[nameValue], {
        shouldValidate: !!TOPIC_CUSTOM_PARAMS[nameValue],
      });
    }
  }, [existingFields, index, nameValue, setExistingFields, setValue]);

  const onRemove = () => {
    // We need to purge existingFields from the current value
    const itemIndex = existingFields.indexOf(nameValue);
    if (itemIndex !== -1) {
      const newExistingFields = [...existingFields];
      newExistingFields.splice(itemIndex, 1);
      setExistingFields(newExistingFields);
    }
    remove(index);
    trigger('customParams');
  };

  return (
    <C.Column>
      <div>
        <InputLabel>Custom Parameter *</InputLabel>
        <Controller
          control={control}
          name={`customParams.${index}.name`}
          render={({ field: { name, onChange, value } }) => (
            <InputWithOptions
              value={value}
              options={options}
              name={name}
              onChange={(s) => {
                onChange(s);
                trigger('customParams');
              }}
              minWidth="270px"
              placeholder="Select"
            />
          )}
        />
        <FormError>
          <ErrorMessage
            errors={errors}
            name={`customParams.${index}.name` as const}
          />
        </FormError>
      </div>
      <div>
        <InputLabel>Value *</InputLabel>
        <Input
          name={`customParams.${index}.value` as const}
          placeholder="Value"
          defaultValue={field.value}
          autoComplete="off"
          disabled={isDisabled}
        />
        <FormError>
          <ErrorMessage
            errors={errors}
            name={`customParams.${index}.value` as const}
          />
        </FormError>
      </div>

      <S.DeleteButtonWrapper>
        <IconButtonWrapper
          onClick={onRemove}
          onKeyDown={(e: React.KeyboardEvent) =>
            e.code === 'Space' && onRemove()
          }
          title={`Delete customParam field ${index}`}
        >
          <CloseCircleIcon aria-hidden />
        </IconButtonWrapper>
      </S.DeleteButtonWrapper>
    </C.Column>
  );
};

export default React.memo(CustomParamField);
