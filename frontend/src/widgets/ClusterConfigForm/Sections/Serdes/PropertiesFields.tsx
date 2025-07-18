import * as React from 'react';
import * as S from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { useFieldArray, useFormContext } from 'react-hook-form';
import PlusIcon from 'components/common/Icons/PlusIcon';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import Heading from 'components/common/heading/Heading.styled';

const PropertiesFields = ({ nestedId }: { nestedId: number }) => {
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: `serde.${nestedId}.properties`,
  });

  return (
    <S.GroupFieldWrapper>
      <Heading level={4}>Serde properties</Heading>
      {fields.map((propsField, propsIndex) => (
        <S.SerdeProperties key={propsField.id}>
          <Input
            name={`serde.${nestedId}.properties.${propsIndex}.key`}
            placeholder="Key"
            type="text"
            withError
          />
          <Input
            name={`serde.${nestedId}.properties.${propsIndex}.value`}
            placeholder="Value"
            type="text"
            withError
          />
          <S.SerdePropertiesActions
            aria-label="deleteProperty"
            onClick={() => remove(propsIndex)}
          >
            <CloseCircleIcon aria-hidden />
          </S.SerdePropertiesActions>
        </S.SerdeProperties>
      ))}
      <div>
        <Button
          type="button"
          buttonSize="M"
          buttonType="secondary"
          onClick={() => append({ key: '', value: '' })}
        >
          <PlusIcon />
          Add Property
        </Button>
      </div>
    </S.GroupFieldWrapper>
  );
};

export default PropertiesFields;
