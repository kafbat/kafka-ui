import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { RouteParamsClusterTopic } from 'lib/paths';
import { Button } from 'components/common/Button/Button';
import Editor from 'components/common/Editor/Editor';
import Select from 'components/common/Select/Select';
import Switch from 'components/common/Switch/Switch';
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

// JSON formatting utility with comprehensive error handling
const formatJsonString = (input: string): { formatted: string; error: string | null } => {
  if (!input || input.trim() === '') {
    return { formatted: input, error: null };
  }
  
  try {
    const parsed = JSON.parse(input);
    const formatted = JSON.stringify(parsed, null, 2);
    return { formatted, error: null };
  } catch (e) {
    const errorMessage = e instanceof Error ? e.message : 'Invalid JSON format';
    return { formatted: input, error: errorMessage };
  }
};

// JSON validation utility for optional validation
const validateJsonField = (value: string, fieldName: string, validateJson: boolean): boolean | string => {
  if (!validateJson || !value || value.trim() === '') return true;
  
  try {
    JSON.parse(value);
    return true;
  } catch (e) {
    return `Invalid JSON in ${fieldName} field: ${e instanceof Error ? e.message : 'Parse error'}`;
  }
};

const SendMessage: React.FC<SendMessageProps> = ({
  closeSidebar,
  messageData = null,
}) => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const { data: serdes = {} } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.SERIALIZE,
  });
  const sendMessage = useSendMessage({ clusterName, topicName });
  
  // Formatting state management
  const [formatKey, setFormatKey] = React.useState<boolean>(false);
  const [formatValue, setFormatValue] = React.useState<boolean>(false);
  const [formatHeaders, setFormatHeaders] = React.useState<boolean>(false);
  const [validateJson, setValidateJson] = React.useState<boolean>(false);

  const defaultValues = React.useMemo(() => getDefaultValues(serdes), [serdes]);
  const partitionOptions = React.useMemo(
    () => getPartitionOptions(topic?.partitions || []),
    [topic]
  );

  const formDefaults = React.useMemo(
    () => ({
      ...defaultValues,
      partition: Number(partitionOptions[0]?.value || 0),
      keepContents: false,
      ...messageData,
    }),
    [defaultValues, partitionOptions, messageData]
  );

  const {
    handleSubmit,
    formState: { isSubmitting },
    control,
    setValue,
    watch,
  } = useForm<MessageFormData>({
    mode: 'onChange',
    defaultValues: formDefaults,
  });

  // Format toggle handler with error handling and user feedback
  const handleFormatToggle = React.useCallback((field: 'key' | 'content' | 'headers') => {
    const currentValue = watch(field) || '';
    const { formatted, error } = formatJsonString(currentValue);
    
    if (error) {
      showAlert('error', {
        id: `format-error-${field}`,
        title: 'Format Error',
        message: `Cannot format ${field}: ${error}`,
      });
    } else {
      setValue(field, formatted);
      // Update formatting state
      switch (field) {
        case 'key': 
          setFormatKey(true); 
          break;
        case 'content': 
          setFormatValue(true); 
          break;
        case 'headers': 
          setFormatHeaders(true); 
          break;
      }
    }
  }, [watch, setValue]);

  const submit = async ({
    keySerde,
    valueSerde,
    key,
    content,
    headers,
    partition,
    keepContents,
  }: MessageFormData) => {
    let errors: string[] = [];

    // JSON validation if enabled
    if (validateJson) {
      const keyValidation = validateJsonField(key || '', 'key', validateJson);
      const contentValidation = validateJsonField(content || '', 'content', validateJson);
      const headersValidation = validateJsonField(headers || '', 'headers', validateJson);

      if (typeof keyValidation === 'string') errors.push(keyValidation);
      if (typeof contentValidation === 'string') errors.push(contentValidation);
      if (typeof headersValidation === 'string') errors.push(headersValidation);
    }

    // Existing schema validation
    if (keySerde) {
      const selectedKeySerde = serdes.key?.find((k) => k.name === keySerde);
      errors = [...errors, ...validateBySchema(key, selectedKeySerde?.schema, 'key')];
    }

    if (valueSerde) {
      const selectedValue = serdes.value?.find((v) => v.name === valueSerde);
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
        keySerde,
        valueSerde,
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
            </S.FlexItem>
            <S.FlexItem>
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
            </S.FlexItem>
          </S.Flex>
          <S.ValidationSection>
            <Switch 
              name="validateJson" 
              onChange={setValidateJson} 
              checked={validateJson} 
            />
            <InputLabel>Validate JSON before submission</InputLabel>
          </S.ValidationSection>
          <div>
            <Controller
              control={control}
              name="keepContents"
              render={({ field: { name, onChange, value } }) => (
                <Switch name={name} onChange={onChange} checked={value} />
              )}
            />
            <InputLabel>Keep contents</InputLabel>
          </div>
        </S.Columns>
        
        <S.Columns>
          <S.FieldGroup>
            <S.FieldHeader>
              <InputLabel>Key</InputLabel>
              <S.FormatButton
                buttonSize="S"
                buttonType={formatKey ? "primary" : "secondary"}
                onClick={() => handleFormatToggle('key')}
                aria-label="Format JSON for key field"
                type="button"
                disabled={isSubmitting}
              >
                Format JSON
              </S.FormatButton>
            </S.FieldHeader>
            <Controller
              control={control}
              name="key"
              render={({ field: { name, onChange, value } }) => (
                <Editor
                  readOnly={isSubmitting}
                  name={name}
                  onChange={(newValue) => {
                    onChange(newValue);
                    // Reset format state when user manually edits
                    if (formatKey && newValue !== value) {
                      setFormatKey(false);
                    }
                  }}
                  value={value}
                  height="40px"
                  mode={formatKey ? "json5" : undefined}
                  setOptions={{
                    showLineNumbers: formatKey,
                    tabSize: 2,
                    useWorker: false
                  }}
                />
              )}
            />
          </S.FieldGroup>
          
          <S.FieldGroup>
            <S.FieldHeader>
              <InputLabel>Value</InputLabel>
              <S.FormatButton
                buttonSize="S"
                buttonType={formatValue ? "primary" : "secondary"}
                onClick={() => handleFormatToggle('content')}
                aria-label="Format JSON for value field"
                type="button"
                disabled={isSubmitting}
              >
                Format JSON
              </S.FormatButton>
            </S.FieldHeader>
            <Controller
              control={control}
              name="content"
              render={({ field: { name, onChange, value } }) => (
                <Editor
                  readOnly={isSubmitting}
                  name={name}
                  onChange={(newValue) => {
                    onChange(newValue);
                    // Reset format state when user manually edits
                    if (formatValue && newValue !== value) {
                      setFormatValue(false);
                    }
                  }}
                  value={value}
                  height="280px"
                  mode={formatValue ? "json5" : undefined}
                  setOptions={{
                    showLineNumbers: formatValue,
                    tabSize: 2,
                    useWorker: false
                  }}
                />
              )}
            />
          </S.FieldGroup>
        </S.Columns>
        
        <S.Columns>
          <S.FieldGroup>
            <S.FieldHeader>
              <InputLabel>Headers</InputLabel>
              <S.FormatButton
                buttonSize="S"
                buttonType={formatHeaders ? "primary" : "secondary"}
                onClick={() => handleFormatToggle('headers')}
                aria-label="Format JSON for headers field"
                type="button"
                disabled={isSubmitting}
              >
                Format JSON
              </S.FormatButton>
            </S.FieldHeader>
            <S.ResizableEditorWrapper>
              <Controller
                control={control}
                name="headers"
                render={({ field: { name, onChange, value } }) => (
                  <Editor
                    readOnly={isSubmitting}
                    name={name}
                    onChange={(newValue) => {
                      onChange(newValue);
                      // Reset format state when user manually edits
                      if (formatHeaders && newValue !== value) {
                        setFormatHeaders(false);
                      }
                    }}
                    value={value || '{}'}
                    height="40px"
                    mode={formatHeaders ? "json5" : undefined}
                    setOptions={{
                      showLineNumbers: formatHeaders,
                      tabSize: 2,
                      useWorker: false
                    }}
                  />
                )}
              />
            </S.ResizableEditorWrapper>
          </S.FieldGroup>
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