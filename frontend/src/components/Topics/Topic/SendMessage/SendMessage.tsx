import React from 'react';
import { useForm, Controller, useWatch } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';
import { RouteParamsClusterTopic } from 'lib/paths';
import { Button } from 'components/common/Button/Button';
import Editor from 'components/common/Editor/Editor';
import InputWithOptions from 'components/common/InputWithOptions/InputWithOptions';
import Select from 'components/common/Select/Select';
import Switch from 'components/common/Switch/Switch';
import Tooltip from 'components/common/Tooltip/Tooltip';
import InfoIcon from 'components/common/Icons/InfoIcon';
import useAppParams from 'lib/hooks/useAppParams';
import { showAlert } from 'lib/errorHandling';
import { useSendMessage, useTopicDetails } from 'lib/hooks/api/topics';
import { InputLabel } from 'components/common/Input/InputLabel.styled';
import { useSerdes } from 'lib/hooks/api/topicMessages';
import { SerdeUsage } from 'generated-sources';
import { MessageFormData } from 'lib/interfaces/message';

import * as S from './SendMessage.styled';
import {
  getDefaultValues,
  getPartitionOptions,
  getSerdeOptions,
  validateBySchema,
} from './utils';

interface SendMessageProps {
  closeSidebar: () => void;
  messageData?: Partial<MessageFormData> | null;
}

const SCHEMA_REGISTRY_SERDE_NAME = 'SchemaRegistry';

const SendMessage: React.FC<SendMessageProps> = ({
  closeSidebar,
  messageData = null,
}) => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const [searchParams] = useSearchParams();
  const urlKeySerde = searchParams.get('keySerde');
  const urlValueSerde = searchParams.get('valueSerde');
  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const { data: serdes = {} } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.SERIALIZE,
  });
  const sendMessage = useSendMessage({ clusterName, topicName });
  const defaultValues = React.useMemo(() => getDefaultValues(serdes), [serdes]);
  const partitionOptions = React.useMemo(
    () => getPartitionOptions(topic?.partitions || []),
    [topic]
  );

  // Get subjects from the SchemaRegistry serde for key and value
  const keySubjectOptions = React.useMemo(() => {
    const srSerde = serdes.key?.find(
      (s) => s.name === SCHEMA_REGISTRY_SERDE_NAME
    );
    return (srSerde?.subjects || []).map((subject) => ({
      label: subject,
      value: subject,
    }));
  }, [serdes.key]);

  const valueSubjectOptions = React.useMemo(() => {
    const srSerde = serdes.value?.find(
      (s) => s.name === SCHEMA_REGISTRY_SERDE_NAME
    );
    return (srSerde?.subjects || []).map((subject) => ({
      label: subject,
      value: subject,
    }));
  }, [serdes.value]);

  const formDefaults = React.useMemo(
    () => ({
      ...defaultValues,
      keySerde: urlKeySerde || defaultValues.keySerde,
      valueSerde: urlValueSerde || defaultValues.valueSerde,
      partition: Number(partitionOptions[0]?.value || 0),
      keepContents: false,
      ...messageData,
    }),
    [defaultValues, partitionOptions, messageData, urlKeySerde, urlValueSerde]
  );

  const {
    handleSubmit,
    formState: { isSubmitting },
    control,
    setValue,
  } = useForm<MessageFormData>({
    mode: 'onChange',
    defaultValues: formDefaults,
  });

  // Update serde values when URL params change
  React.useEffect(() => {
    if (urlKeySerde) {
      setValue('keySerde', urlKeySerde);
    }
    if (urlValueSerde) {
      setValue('valueSerde', urlValueSerde);
    }
  }, [urlKeySerde, urlValueSerde, setValue]);

  const keySerde = useWatch({ control, name: 'keySerde' });
  const valueSerde = useWatch({ control, name: 'valueSerde' });

  const showKeySubject = keySerde === SCHEMA_REGISTRY_SERDE_NAME;
  const showValueSubject = valueSerde === SCHEMA_REGISTRY_SERDE_NAME;

  const submit = async ({
    keySerde: formKeySerde,
    valueSerde: formValueSerde,
    key,
    content,
    headers,
    partition,
    keySubject,
    valueSubject,
    keepContents,
  }: MessageFormData) => {
    let errors: string[] = [];

    if (formKeySerde) {
      const selectedKeySerde = serdes.key?.find((k) => k.name === formKeySerde);
      errors = validateBySchema(key, selectedKeySerde?.schema, 'key');
    }

    if (formValueSerde) {
      const selectedValue = serdes.value?.find(
        (v) => v.name === formValueSerde
      );
      errors = [
        ...errors,
        ...validateBySchema(content, selectedValue?.schema, 'content'),
      ];
    }

    let parsedHeaders;
    if (headers) {
      try {
        parsedHeaders = JSON.parse(headers);
      } catch (error) {
        errors.push('Wrong header format');
      }
    }

    if (errors.length > 0) {
      showAlert('error', {
        id: `${clusterName}-${topicName}-createTopicMessageError`,
        title: 'Validation Error',
        message: (
          <ul>
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        ),
      });
      return;
    }
    try {
      await sendMessage.mutateAsync({
        key: key || null,
        value: content || null,
        headers: parsedHeaders,
        partition: partition || 0,
        keySerde: formKeySerde,
        valueSerde: formValueSerde,
        keySubject: keySubject || undefined,
        valueSubject: valueSubject || undefined,
      });
      if (!keepContents) {
        setValue('key', defaultValues.key || '');
        setValue('content', defaultValues.content || '');
        closeSidebar();
      }
    } catch (e) {
      // do nothing
    }
  };

  return (
    <S.Wrapper>
      <form onSubmit={handleSubmit(submit)}>
        <S.Columns>
          <S.FlexItem>
            <InputLabel id="partitionOptionsLabel">Partition</InputLabel>
            <Controller
              control={control}
              name="partition"
              render={({ field: { name, onChange, value } }) => (
                <Select
                  id="selectPartitionOptions"
                  aria-labelledby="partitionOptionsLabel"
                  name={name}
                  onChange={onChange}
                  minWidth="100%"
                  options={partitionOptions}
                  value={value}
                />
              )}
            />
          </S.FlexItem>
          <S.Flex>
            <S.FlexItem>
              <div>
                <InputLabel id="keySerdeOptionsLabel">Key Serde</InputLabel>
                <Controller
                  control={control}
                  name="keySerde"
                  render={({ field: { name, onChange, value } }) => (
                    <Select
                      id="selectKeySerdeOptions"
                      aria-labelledby="keySerdeOptionsLabel"
                      name={name}
                      onChange={onChange}
                      minWidth="100%"
                      options={getSerdeOptions(serdes.key || [])}
                      value={value}
                    />
                  )}
                />
              </div>
              {showKeySubject && (
                <div>
                  <InputLabel id="keySubjectOptionsLabel">
                    Key Subject
                  </InputLabel>
                  <Controller
                    control={control}
                    name="keySubject"
                    render={({ field: { name, onChange, value } }) => (
                      <InputWithOptions
                        name={name}
                        onChange={onChange}
                        minWidth="100%"
                        options={keySubjectOptions}
                        value={value}
                        placeholder="Search subjects..."
                        inputSize="M"
                      />
                    )}
                  />
                </div>
              )}
            </S.FlexItem>
            <S.FlexItem>
              <div>
                <InputLabel id="valueSerdeOptionsLabel">Value Serde</InputLabel>
                <Controller
                  control={control}
                  name="valueSerde"
                  render={({ field: { name, onChange, value } }) => (
                    <Select
                      id="selectValueSerdeOptions"
                      aria-labelledby="valueSerdeOptionsLabel"
                      name={name}
                      onChange={onChange}
                      minWidth="100%"
                      options={getSerdeOptions(serdes.value || [])}
                      value={value}
                    />
                  )}
                />
              </div>
              {showValueSubject && (
                <div>
                  <InputLabel id="valueSubjectOptionsLabel">
                    Value Subject
                  </InputLabel>
                  <Controller
                    control={control}
                    name="valueSubject"
                    render={({ field: { name, onChange, value } }) => (
                      <InputWithOptions
                        name={name}
                        onChange={onChange}
                        minWidth="100%"
                        options={valueSubjectOptions}
                        value={value}
                        placeholder="Search subjects..."
                        inputSize="M"
                      />
                    )}
                  />
                </div>
              )}
            </S.FlexItem>
          </S.Flex>
        </S.Columns>
        <S.Columns>
          <div>
            <InputLabel>Key</InputLabel>
            <Controller
              control={control}
              name="key"
              render={({ field: { name, onChange, value } }) => (
                <Editor
                  readOnly={isSubmitting}
                  name={name}
                  onChange={onChange}
                  value={value}
                  height="40px"
                />
              )}
            />
          </div>
          <div>
            <InputLabel>Value</InputLabel>
            <Controller
              control={control}
              name="content"
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
          </div>
        </S.Columns>
        <S.Columns>
          <div>
            <InputLabel>Headers</InputLabel>
            <Controller
              control={control}
              name="headers"
              render={({ field: { name, onChange, value } }) => (
                <Editor
                  readOnly={isSubmitting}
                  name={name}
                  onChange={onChange}
                  value={value || '{}'}
                  height="40px"
                />
              )}
            />
          </div>
        </S.Columns>
        <S.Columns>
          <S.Flex>
            <Controller
              control={control}
              name="keepContents"
              render={({ field: { name, onChange, value } }) => (
                <Switch name={name} onChange={onChange} checked={value} />
              )}
            />
            <InputLabel>Keep contents after producing a message</InputLabel>
            <Tooltip
              value={<InfoIcon />}
              content="When enabled, the form will remain populated after sending a message."
            />
          </S.Flex>
        </S.Columns>
        <Button
          buttonSize="M"
          buttonType="primary"
          type="submit"
          disabled={isSubmitting}
        >
          Produce Message
        </Button>
      </form>
    </S.Wrapper>
  );
};

export default SendMessage;
