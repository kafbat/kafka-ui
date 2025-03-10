import * as React from 'react';
import * as S from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { useFieldArray, useFormContext } from 'react-hook-form';
import PlusIcon from 'components/common/Icons/PlusIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import {
  FieldContainer,
  FieldWrapper,
  FlexGrow1,
  FlexRow,
} from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import SectionHeader from 'widgets/ClusterConfigForm/common/SectionHeader';
import { MASKING_OPTIONS } from 'lib/constants';
import ControlledSelect from 'components/common/Select/ControlledSelect';
import { FormError } from 'components/common/Input/Input.styled';
import { ErrorMessage } from '@hookform/error-message';

const Fields = ({ nestedIdx }: { nestedIdx: number }) => {
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: `masking.${nestedIdx}.fields`,
  });

  const handleAppend = () => append({ value: '' });

  return (
    <FlexGrow1>
      <FieldWrapper>
        <FieldWrapper>
          {fields.map((item, index) => (
            <FieldContainer key={item.id}>
              <Input
                label="Field"
                name={`masking.${nestedIdx}.fields.${index}.value`}
                placeholder="Field"
                type="text"
                withError
              />

              {fields.length > 1 && (
                <S.RemoveButton
                  style={{ marginTop: '18px' }}
                  onClick={() => remove(index)}
                >
                  <IconButtonWrapper aria-label="deleteProperty">
                    <CloseCircleIcon aria-hidden />
                  </IconButtonWrapper>
                </S.RemoveButton>
              )}
            </FieldContainer>
          ))}
        </FieldWrapper>

        <Button
          style={{ marginTop: '20px' }}
          type="button"
          buttonSize="M"
          buttonType="secondary"
          onClick={handleAppend}
        >
          <PlusIcon />
          Add Field
        </Button>
      </FieldWrapper>

      <FormError>
        <ErrorMessage name={`masking.${nestedIdx}.fields`} />
      </FormError>
    </FlexGrow1>
  );
};

const MaskingCharReplacement = ({ nestedIdx }: { nestedIdx: number }) => {
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: `masking.${nestedIdx}.maskingCharsReplacement`,
  });

  const handleAppend = () => append({ value: '' });

  return (
    <FlexGrow1>
      <FieldWrapper>
        <FieldWrapper>
          {fields.map((item, index) => (
            <FieldContainer key={item.id}>
              <Input
                label="Field"
                name={`masking.${nestedIdx}.maskingCharsReplacement.${index}.value`}
                placeholder="Field"
                type="text"
                withError
              />

              {fields.length > 1 && (
                <S.RemoveButton
                  style={{ marginTop: '18px' }}
                  onClick={() => remove(index)}
                >
                  <IconButtonWrapper aria-label="deleteProperty">
                    <CloseCircleIcon aria-hidden />
                  </IconButtonWrapper>
                </S.RemoveButton>
              )}
            </FieldContainer>
          ))}
        </FieldWrapper>

        <Button
          style={{ marginTop: '20px' }}
          type="button"
          buttonSize="M"
          buttonType="secondary"
          onClick={handleAppend}
        >
          <PlusIcon />
          Add Masking Chars Replacement
        </Button>
      </FieldWrapper>

      <FormError>
        <ErrorMessage name={`masking.${nestedIdx}.maskingCharsReplacement`} />
      </FormError>
    </FlexGrow1>
  );
};

const Masking = () => {
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'masking',
  });
  const handleAppend = () =>
    append({
      type: undefined,
      fields: [{ value: '' }],
      fieldsNamePattern: '',
      maskingCharsReplacement: [{ value: '' }],
      replacement: '',
      topicKeysPattern: '',
      topicValuesPattern: '',
    });
  const toggleConfig = () => (fields.length === 0 ? handleAppend() : remove());

  const hasFields = fields.length > 0;

  return (
    <>
      <SectionHeader
        title="Masking"
        addButtonText="Configure Masking"
        adding={!hasFields}
        onClick={toggleConfig}
      />
      {hasFields && (
        <S.GroupFieldWrapper>
          {fields.map((item, index) => (
            <div key={item.id}>
              <FlexRow>
                <FlexGrow1>
                  <ControlledSelect
                    name={`masking.${index}.type`}
                    label="Masking Type *"
                    placeholder="Choose masking type"
                    options={MASKING_OPTIONS}
                  />
                  <Fields nestedIdx={index} />
                  <Input
                    label="Fields name pattern"
                    name={`masking.${index}.fieldsNamePattern`}
                    placeholder="Pattern"
                    type="text"
                    withError
                  />
                  <MaskingCharReplacement nestedIdx={index} />
                  <Input
                    label="Replacement"
                    name={`masking.${index}.replacement`}
                    placeholder="Replacement"
                    type="text"
                  />
                  <Input
                    label="Topic Keys Pattern"
                    name={`masking.${index}.topicKeysPattern`}
                    placeholder="Keys pattern"
                    type="text"
                  />
                  <Input
                    label="Topic Values Pattern"
                    name={`masking.${index}.topicValuesPattern`}
                    placeholder="Values pattern"
                    type="text"
                  />
                </FlexGrow1>
                <S.RemoveButton onClick={() => remove(index)}>
                  <IconButtonWrapper aria-label="deleteProperty">
                    <CloseCircleIcon aria-hidden />
                  </IconButtonWrapper>
                </S.RemoveButton>
              </FlexRow>

              <hr />
            </div>
          ))}
          <Button
            type="button"
            buttonSize="M"
            buttonType="secondary"
            onClick={handleAppend}
          >
            <PlusIcon />
            Add Masking
          </Button>
        </S.GroupFieldWrapper>
      )}
    </>
  );
};
export default Masking;
