import * as React from 'react';
import * as S from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { useFieldArray, useFormContext } from 'react-hook-form';
import PlusIcon from 'components/common/Icons/PlusIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import Heading from 'components/common/heading/Heading.styled';
import {
  FlexGrow1,
  FlexRow,
} from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import SectionHeader from 'widgets/ClusterConfigForm/common/SectionHeader';
import { Serde } from 'widgets/ClusterConfigForm/types';

const Serdes = () => {
  const { control, reset, getValues } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'serde',
  });
  const {
    fields: propsFields,
    append: appendProps,
    remove: removeProps,
  } = useFieldArray({
    control,
    name: 'properties',
  });

  React.useEffect(() => {
    reset();
    getValues().serde?.forEach((item: Serde, index: number) => {
      item.properties?.forEach((itemProp) => {
        appendProps({
          key: itemProp.key,
          value: itemProp.value,
          serdeIndex: index,
        });
      });
    });
  }, []);

  const handleAppend = () =>
    append({
      name: '',
      className: '',
      filePath: '',
      topicKeysPattern: '%s-key',
      topicValuesPattern: '%s-value',
    });
  const toggleConfig = () => (fields.length === 0 ? handleAppend() : remove());

  const hasFields = fields.length > 0;

  return (
    <>
      <SectionHeader
        title="Serdes"
        addButtonText="Configure Serdes"
        adding={!hasFields}
        onClick={toggleConfig}
      />
      {hasFields && (
        <S.GroupFieldWrapper>
          {fields.map((item, index) => (
            <div key={item.id}>
              <FlexRow>
                <FlexGrow1>
                  <Input
                    label="Name *"
                    name={`serde.${index}.name`}
                    placeholder="Name"
                    type="text"
                    hint="Serde name"
                    withError
                  />
                  <Input
                    label="Class Name *"
                    name={`serde.${index}.className`}
                    placeholder="className"
                    type="text"
                    hint="Serde class name"
                    withError
                  />
                  <Input
                    label="File Path *"
                    name={`serde.${index}.filePath`}
                    placeholder="serde file path"
                    type="text"
                    hint="Serde file path"
                    withError
                  />
                  <Input
                    label="Topic Keys Pattern *"
                    name={`serde.${index}.topicKeysPattern`}
                    placeholder="topicKeysPattern"
                    type="text"
                    hint="Serde topic keys pattern"
                    withError
                  />
                  <Input
                    label="Topic Values Pattern *"
                    name={`serde.${index}.topicValuesPattern`}
                    placeholder="topicValuesPattern"
                    type="text"
                    hint="Serde topic values pattern"
                    withError
                  />
                  <hr />
                  <S.GroupFieldWrapper>
                    <Heading level={4}>Serde properties</Heading>
                    {propsFields
                      .filter(
                        (propItem) =>
                          (propItem as unknown as { serdeIndex: number })
                            .serdeIndex === index
                      )
                      .map((propsField, propsIndex) => (
                        <S.SerdeProperties key={propsField.id}>
                          <div>
                            <Input
                              name={`serde.${index}.properties.${propsIndex}.key`}
                              placeholder="Key"
                              type="text"
                              withError
                            />
                          </div>
                          <div>
                            <Input
                              name={`serde.${index}.properties.${propsIndex}.value`}
                              placeholder="Value"
                              type="text"
                              withError
                            />
                          </div>
                          <S.SerdePropertiesActions
                            aria-label="deleteProperty"
                            onClick={() => removeProps(index)}
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
                        onClick={() =>
                          appendProps({ key: '', value: '', serdeIndex: index })
                        }
                      >
                        <PlusIcon />
                        Add Property
                      </Button>
                    </div>
                  </S.GroupFieldWrapper>
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
            Add Serde
          </Button>
        </S.GroupFieldWrapper>
      )}
    </>
  );
};
export default Serdes;
