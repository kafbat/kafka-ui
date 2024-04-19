import React from 'react';
import { useFieldArray, useFormContext, useWatch } from 'react-hook-form';
import { Button } from 'components/common/Button/Button';
import { TOPIC_CUSTOM_PARAMS_PREFIX } from 'lib/constants';
import PlusIcon from 'components/common/Icons/PlusIcon';
import { ErrorMessage } from '@hookform/error-message';
import { FormError } from 'components/common/Input/Input.styled';
import { TopicConfigParams, TopicFormData } from 'lib/interfaces/topic';

import CustomParamField from './CustomParamField';
import * as S from './CustomParams.styled';

export interface CustomParamsProps {
  config?: TopicConfigParams;
  isSubmitting: boolean;
  isEditing?: boolean;
}

const CustomParams: React.FC<CustomParamsProps> = ({
  isSubmitting,
  config,
}) => {
  const {
    trigger,
    control,
    formState: { errors },
  } = useFormContext<TopicFormData>();

  const { fields, append, remove } = useFieldArray({
    control,
    name: TOPIC_CUSTOM_PARAMS_PREFIX,
  });
  const watchFieldArray = useWatch({
    control,
    name: TOPIC_CUSTOM_PARAMS_PREFIX,
    defaultValue: fields,
  });
  const controlledFields = fields.map((field, index) => {
    return {
      ...field,
      ...watchFieldArray[index],
    };
  });

  const [existingFields, setExistingFields] = React.useState<string[]>([]);
  const removeField = (index: number): void => {
    const itemIndex = existingFields.indexOf(controlledFields[index].name);
    if (itemIndex !== -1) {
      existingFields.splice(itemIndex, 1);
      setExistingFields(existingFields);
      remove(index);
      trigger('customParams');
    }
  };

  return (
    <S.ParamsWrapper>
      {controlledFields?.map((field, idx) => (
        <CustomParamField
          key={field.id}
          config={config}
          field={field}
          remove={removeField}
          index={idx}
          isDisabled={isSubmitting}
          existingFields={existingFields}
          setExistingFields={setExistingFields}
        />
      ))}
      <FormError>
        <ErrorMessage errors={errors} name={`customParams` as const} />
      </FormError>
      <div>
        <Button
          type="button"
          buttonSize="M"
          buttonType="secondary"
          onClick={() =>
            append({ name: '', value: '' }, { shouldFocus: false })
          }
        >
          <PlusIcon />
          Add Custom Parameter
        </Button>
      </div>
    </S.ParamsWrapper>
  );
};

export default CustomParams;
