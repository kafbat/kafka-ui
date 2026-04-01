import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { RouteParamsClusterTopic } from 'lib/paths';
import { Button } from 'components/common/Button/Button';
import useAppParams from 'lib/hooks/useAppParams';
import { useDataUnmask } from 'lib/hooks/api/topics';
import { InputLabel } from 'components/common/Input/InputLabel.styled';
import { useSerdes } from 'lib/hooks/api/topicMessages';
import { SerdeUsage } from 'generated-sources';
import Editor from 'components/common/Editor/Editor';

import * as S from './DataUnmask.styled';
import { getDefaultValues } from './utils';

interface FormType {
  justification: string; // Added field to interface
}

const DataUnmask: React.FC<{ closeSidebar: () => void }> = () => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  // const { data: topic } = useTopicDetails({ clusterName, topicName });
  const { data: serdes = {} } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.SERIALIZE,
  });
  const dataUnmasking = useDataUnmask({ clusterName, topicName });

  const defaultValues = React.useMemo(() => getDefaultValues(), [serdes]);
  const {
    handleSubmit,
    formState: { isSubmitting },
    control,
  } = useForm<FormType>({
    mode: 'onChange',
    defaultValues: {
      ...defaultValues,
    },
  });

  const submit = async ({
    justification, // Destructured ticketId
  }: FormType) => {
    try {
      await dataUnmasking.mutateAsync({
        justification,
      });
    } catch (e) {
      // do nothing
    }
  };

  return (
    <S.Wrapper>
      <form onSubmit={handleSubmit(submit)}>
        <S.Columns>
          <S.FlexItem>
            <InputLabel>Justification *</InputLabel>
            <Controller
              control={control}
              name="justification"
              rules={{
                required: 'Justification is required',
                minLength: {
                  value: 20,
                  message:
                    'Justification is too short. Less than 20 characters',
                },
              }}
              render={({ field: { name, onChange, value } }) => (
                <Editor
                  readOnly={isSubmitting}
                  name={name}
                  onChange={onChange}
                  value={value}
                  height="280px"
                />
              )}
            />
          </S.FlexItem>
        </S.Columns>
        <Button
          buttonSize="M"
          buttonType="primary"
          type="submit"
          disabled={isSubmitting}
        >
          Unmask Messages
        </Button>
      </form>
    </S.Wrapper>
  );
};

export default DataUnmask;
