import * as React from 'react';
import * as S from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { useFieldArray, useFormContext } from 'react-hook-form';
import PlusIcon from 'components/common/Icons/PlusIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import CloseCircleIcon from 'components/common/Icons/CloseCircleIcon';
import {
  FlexGrow1,
  FlexRow,
} from 'widgets/ClusterConfigForm/ClusterConfigForm.styled';
import SectionHeader from 'widgets/ClusterConfigForm/common/SectionHeader';
import Checkbox from 'components/common/Checkbox/Checkbox';

const MessageFilters = () => {
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'messageFilters',
  });
  const handleAppend = () =>
    append({
      displayName: '',
      filterCode: '',
      enabledByDefault: false,
    });
  const toggleConfig = () => (fields.length === 0 ? handleAppend() : remove());

  const hasFields = fields.length > 0;

  return (
    <>
      <SectionHeader
        title="Message Filters"
        addButtonText="Configure Message Filters"
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
                    label="Display Name *"
                    name={`messageFilters.${index}.displayName`}
                    placeholder="Non-heartbeat"
                    type="text"
                    withError
                  />
                  <Input
                    label="Filter Code (CEL) *"
                    name={`messageFilters.${index}.filterCode`}
                    placeholder="has(record.value.after)"
                    type="text"
                    withError
                    hint="CEL expression evaluated for each message. nowMs is the current epoch millis."
                  />
                  <Checkbox
                    name={`messageFilters.${index}.enabledByDefault`}
                    label="Enabled by default on every topic"
                    hint="Users can still clear the filter in the UI. The choice is remembered per topic."
                  />
                </FlexGrow1>
                <S.RemoveButton onClick={() => remove(index)}>
                  <IconButtonWrapper aria-label="deleteMessageFilter">
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
            Add Filter
          </Button>
        </S.GroupFieldWrapper>
      )}
    </>
  );
};

export default MessageFilters;
