/* eslint-disable @typescript-eslint/no-use-before-define */
import React, { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { SerdeUsage } from 'generated-sources';
import styled from 'styled-components';
import { Button } from 'components/common/Button/Button';
import FlexBox from 'components/common/FlexBox/FlexBox';
import Input from 'components/common/Input/Input';
import MultiSelect from 'components/common/MultiSelect/MultiSelect.styled';
import Select from 'components/common/Select/Select';
import {
  getPreferredDescription,
  getSerdeOptions,
} from 'components/Topics/Topic/SendMessage/utils';
import { useTopicDetails } from 'lib/hooks/api/topics';
import {
  UploadMessagesResult,
  useSerdes,
  useUploadMessages,
} from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';

interface Option<T> {
  label: string;
  value: T;
}

type PartitionOption = Option<number>;

const parseModeOptions: Option<string>[] = [
  { label: 'One message per file / ZIP entry', value: 'FILE_PER_MESSAGE' },
  { label: 'One message per text line', value: 'TEXT_LINES' },
  { label: 'NDJSON - validate each JSON line', value: 'NDJSON' },
  { label: 'JSON array - one message per item', value: 'JSON_ARRAY' },
];

const partitionStrategyOptions: Option<string>[] = [
  { label: 'Broker default / key decides', value: 'ANY' },
  { label: 'Selected partition', value: 'SELECTED' },
  { label: 'Random partition', value: 'RANDOM' },
  { label: 'Evenly round-robin', value: 'EVEN' },
];

const keyModeOptions: Option<string>[] = [
  { label: 'No key', value: 'NONE' },
  { label: 'Use original file name', value: 'FILE_NAME' },
  { label: 'Use ZIP entry / file path', value: 'ENTRY_NAME' },
];

const filterOptions = (options: PartitionOption[], filter: string) =>
  options.filter(({ label }) => label.toLowerCase().includes(filter.toLowerCase()));

const fileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

const Upload: React.FC = () => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const { data: serdes = {} } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.SERIALIZE,
  });
  const uploadMessages = useUploadMessages();

  const [files, setFiles] = useState<File[]>([]);
  const [parseMode, setParseMode] = useState('FILE_PER_MESSAGE');
  const [partitionStrategy, setPartitionStrategy] = useState('ANY');
  const [selectedPartition, setSelectedPartition] = useState<string>();
  const [targetPartitions, setTargetPartitions] = useState<PartitionOption[]>([]);
  const [keyMode, setKeyMode] = useState('NONE');
  const [keySerde, setKeySerde] = useState<string | undefined>();
  const [valueSerde, setValueSerde] = useState<string | undefined>();
  const [headersJson, setHeadersJson] = useState('');
  const [includeMetadataHeaders, setIncludeMetadataHeaders] = useState(true);
  const [messageLimit, setMessageLimit] = useState('1000');
  const [result, setResult] = useState<UploadMessagesResult>();

  const partitionOptions = useMemo<PartitionOption[]>(() => {
    return (topic?.partitions || []).map(({ partition }) => ({
      label: `Partition #${partition}`,
      value: partition,
    }));
  }, [topic?.partitions]);

  const preferredKeySerde = getPreferredDescription(serdes.key || [])?.name;
  const preferredValueSerde = getPreferredDescription(serdes.value || [])?.name;

  useEffect(() => {
    if (!keySerde) setKeySerde(preferredKeySerde);
    if (!valueSerde) setValueSerde(preferredValueSerde);
  }, [keySerde, preferredKeySerde, preferredValueSerde, valueSerde]);

  useEffect(() => {
    if (!selectedPartition && partitionOptions[0]) {
      setSelectedPartition(partitionOptions[0].value.toString());
    }
  }, [partitionOptions, selectedPartition]);

  const handleFiles = ({ target }: ChangeEvent<HTMLInputElement>) => {
    setFiles(Array.from(target.files || []));
    setResult(undefined);
  };

  const runUpload = async (dryRun: boolean) => {
    const response = await uploadMessages.mutateAsync({
      clusterName,
      topicName,
      files,
      parseMode,
      partitionStrategy,
      keyMode,
      partition: partitionStrategy === 'SELECTED' ? selectedPartition : undefined,
      partitions:
        partitionStrategy === 'RANDOM' || partitionStrategy === 'EVEN'
          ? targetPartitions.map(({ value }) => value)
          : undefined,
      keySerde,
      valueSerde,
      headersJson: headersJson || undefined,
      includeMetadataHeaders,
      dryRun,
      messageLimit,
    });
    setResult(response);
  };

  const canSubmit = files.length > 0 && keySerde && valueSerde;

  return (
    <Page>
      <Hero>
        <div>
          <Eyebrow>Topic import center</Eyebrow>
          <Title>Upload messages</Title>
          <Description>
            Produce single files, multiple files, or every file inside a ZIP to
            this topic. Dry run first, preview the parsed records, then produce.
          </Description>
        </div>
        <HeroActions>
          <Button
            buttonType="secondary"
            buttonSize="M"
            onClick={() => runUpload(true)}
            disabled={!canSubmit || uploadMessages.isPending}
          >
            Dry run
          </Button>
          <Button
            buttonType="primary"
            buttonSize="M"
            onClick={() => runUpload(false)}
            disabled={!canSubmit || uploadMessages.isPending}
          >
            {uploadMessages.isPending ? 'Producing...' : 'Produce files'}
          </Button>
        </HeroActions>
      </Hero>

      <Grid>
        <Card>
          <CardTitle>1. Files</CardTitle>
          <DropZone htmlFor="uploadFiles">
            <strong>Select files or a ZIP archive</strong>
            <span>Single file, multiple files, JSON, NDJSON, TXT, or ZIP.</span>
            <input
              id="uploadFiles"
              type="file"
              multiple
              accept=".zip,.json,.ndjson,.txt,.log,application/zip,application/json,text/*"
              onChange={handleFiles}
            />
          </DropZone>
          {files.length > 0 && (
            <FileList>
              {files.map((file) => (
                <li key={`${file.name}-${file.size}-${file.lastModified}`}>
                  <span>{file.name}</span>
                  <small>{fileSize(file.size)}</small>
                </li>
              ))}
            </FileList>
          )}
        </Card>

        <Card>
          <CardTitle>2. Parse mode</CardTitle>
          <Field>
            <Label>How file content becomes Kafka records</Label>
            <Select
              id="uploadParseMode"
              options={parseModeOptions}
              value={parseMode}
              onChange={setParseMode}
              minWidth="100%"
            />
          </Field>
          <Hint>
            ZIP files are expanded first. Each ZIP entry is parsed using the
            selected mode, so one archive can produce many records.
          </Hint>
        </Card>

        <Card>
          <CardTitle>3. Partition strategy</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Strategy</Label>
              <Select
                id="uploadPartitionStrategy"
                options={partitionStrategyOptions}
                value={partitionStrategy}
                onChange={setPartitionStrategy}
                minWidth="100%"
              />
            </Field>
            {partitionStrategy === 'SELECTED' && (
              <Field>
                <Label>Partition</Label>
                <Select
                  id="uploadSelectedPartition"
                  options={partitionOptions.map(({ label, value }) => ({
                    label,
                    value: value.toString(),
                  }))}
                  value={selectedPartition}
                  onChange={setSelectedPartition}
                  minWidth="100%"
                />
              </Field>
            )}
            {(partitionStrategy === 'RANDOM' || partitionStrategy === 'EVEN') && (
              <Field>
                <Label>Optional partition subset</Label>
                <MultiSelect
                  options={partitionOptions}
                  filterOptions={filterOptions}
                  onChange={(value: PartitionOption[]) => setTargetPartitions(value)}
                  value={targetPartitions}
                  minWidth="100%"
                  labelledBy="uploadPartitionSubset"
                  overrideStrings={{
                    selectSomeItems: 'All partitions unless selected',
                  }}
                />
              </Field>
            )}
          </FlexBox>
        </Card>

        <Card>
          <CardTitle>4. Serdes and keys</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Key serde</Label>
              <Select
                id="uploadKeySerde"
                options={getSerdeOptions(serdes.key || [])}
                value={keySerde}
                onChange={setKeySerde}
                minWidth="100%"
                placeholder="Key Serde"
              />
            </Field>
            <Field>
              <Label>Value serde</Label>
              <Select
                id="uploadValueSerde"
                options={getSerdeOptions(serdes.value || [])}
                value={valueSerde}
                onChange={setValueSerde}
                minWidth="100%"
                placeholder="Value Serde"
              />
            </Field>
            <Field>
              <Label>Key source</Label>
              <Select
                id="uploadKeyMode"
                options={keyModeOptions}
                value={keyMode}
                onChange={setKeyMode}
                minWidth="100%"
              />
            </Field>
          </FlexBox>
        </Card>

        <Card>
          <CardTitle>5. Safety and headers</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Max parsed messages</Label>
              <Input
                inputSize="M"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={messageLimit}
                onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                  setMessageLimit(value.replace(/\D/g, ''));
                }}
              />
            </Field>
            <CheckboxLabel>
              <input
                type="checkbox"
                checked={includeMetadataHeaders}
                onChange={({ target: { checked } }) => {
                  setIncludeMetadataHeaders(checked);
                }}
              />
              Add file metadata headers
            </CheckboxLabel>
          </FlexBox>
          <FieldFull>
            <Label>Extra headers JSON</Label>
            <TextArea
              value={headersJson}
              placeholder='{"source":"manual-upload","team":"example"}'
              onChange={({ target: { value } }) => setHeadersJson(value)}
            />
          </FieldFull>
        </Card>
      </Grid>

      {result && (
        <ResultCard>
          <CardTitle>{result.dryRun ? 'Dry run result' : 'Produce result'}</CardTitle>
          <Stats>
            <Stat><b>{result.filesReceived}</b><span>files received</span></Stat>
            <Stat><b>{result.entriesRead}</b><span>entries read</span></Stat>
            <Stat><b>{result.messagesParsed}</b><span>messages parsed</span></Stat>
            <Stat><b>{result.messagesProduced}</b><span>messages produced</span></Stat>
            <Stat><b>{result.failures}</b><span>failures</span></Stat>
          </Stats>
          {result.previews.length > 0 && (
            <TableWrapper>
              <PreviewTable>
                <thead>
                  <tr>
                    <th>Entry</th>
                    <th>Partition</th>
                    <th>Key</th>
                    <th>Bytes</th>
                    <th>Preview</th>
                  </tr>
                </thead>
                <tbody>
                  {result.previews.map((preview) => (
                    <tr
                      key={`${preview.sourceFile}-${preview.entryName}-${preview.partition}-${preview.key}-${preview.valueBytes}-${preview.valuePreview}`}
                    >
                      <td>{preview.entryName}</td>
                      <td>{preview.partition ?? 'broker'}</td>
                      <td>{preview.key || '—'}</td>
                      <td>{preview.valueBytes}</td>
                      <td>{preview.valuePreview}</td>
                    </tr>
                  ))}
                </tbody>
              </PreviewTable>
            </TableWrapper>
          )}
          {result.errors.length > 0 && (
            <Errors>
              {result.errors.map((error) => (
                <li key={error}>{error}</li>
              ))}
            </Errors>
          )}
        </ResultCard>
      )}
    </Page>
  );
};

const Page = styled.div`
  width: 100%;
  max-width: 1400px;
  min-width: 0;
  box-sizing: border-box;
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.S}px) {
    padding: 12px;
  }
`;

const Hero = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.modal.border.contrast};
  border-radius: 12px;
  background-color: ${({ theme }) => theme.modal.backgroundColor};
  color: ${({ theme }) => theme.default.color.normal};

  & > div:first-child {
    flex: 1 1 360px;
    min-width: 0;
    max-width: 100%;
  }

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.M}px) {
    flex-direction: column;
    align-items: stretch;
  }
`;

const HeroActions = styled.div`
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 100%;

  & > button {
    white-space: nowrap;
  }

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.S}px) {
    flex-direction: column;
    align-items: stretch;
    width: 100%;

    & > button {
      width: 100%;
    }
  }
`;

const Eyebrow = styled.div`
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: ${({ theme }) => theme.input.label.color};
`;

const Title = styled.h2`
  margin: 4px 0;
  color: ${({ theme }) => theme.default.color.normal};
  overflow-wrap: anywhere;
`;

const Description = styled.p`
  margin: 0;
  max-width: 760px;
  color: ${({ theme }) => theme.modal.contentColor};
  line-height: 1.5;
  overflow-wrap: anywhere;
`;

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 16px;
  min-width: 0;
  max-width: 100%;

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.S}px) {
    grid-template-columns: 1fr;
  }
`;

const Card = styled.div`
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  padding: 16px;
  border: 1px solid ${({ theme }) => theme.modal.border.contrast};
  border-radius: 12px;
  background-color: ${({ theme }) => theme.modal.backgroundColor};
  color: ${({ theme }) => theme.default.color.normal};
`;

const ResultCard = styled(Card)`
  overflow: hidden;
`;

const CardTitle = styled.h3`
  margin: 0 0 14px;
  color: ${({ theme }) => theme.default.color.normal};
  overflow-wrap: anywhere;
`;

const Field = styled.label`
  display: flex;
  flex-direction: column;
  flex: 1 1 220px;
  gap: 6px;
  min-width: 0;
  max-width: 100%;

  & > div {
    width: 100%;
    min-width: 0;
    max-width: 100%;
  }

  & > div > ul[role='listbox'] {
    width: 100%;
    min-width: 0;
    max-width: 100%;
    box-sizing: border-box;
  }

  & > div > ul[role='listbox'] [role='option'] {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .dropdown-container {
    max-width: 100%;
  }

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.S}px) {
    flex-basis: 100%;
    width: 100%;
  }
`;

const FieldFull = styled(Field)`
  margin-top: 14px;
  width: 100%;
  flex-basis: 100%;
`;

const Label = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.input.label.color};
  overflow-wrap: anywhere;
`;

const Hint = styled.p`
  margin: 12px 0 0;
  color: ${({ theme }) => theme.modal.contentColor};
  line-height: 1.5;
  overflow-wrap: anywhere;
`;

const DropZone = styled.label`
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  padding: 22px;
  border: 1px dashed ${({ theme }) => theme.input.borderColor.normal};
  border-radius: 12px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  color: ${({ theme }) => theme.default.color.normal};
  cursor: pointer;

  &:hover {
    border-color: ${({ theme }) => theme.input.borderColor.hover};
  }

  &:focus-within {
    border-color: ${({ theme }) => theme.input.borderColor.focus};
  }

  strong {
    overflow-wrap: anywhere;
  }

  input {
    margin-top: 6px;
    max-width: 100%;
    color: ${({ theme }) => theme.default.color.normal};
  }

  span {
    color: ${({ theme }) => theme.input.label.color};
    line-height: 1.5;
    overflow-wrap: anywhere;
  }
`;

const FileList = styled.ul`
  margin: 12px 0 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    padding: 8px 0;
    border-bottom: 1px solid ${({ theme }) => theme.layout.stuffBorderColor};
    color: ${({ theme }) => theme.default.color.normal};
  }

  span {
    min-width: 0;
    overflow-wrap: anywhere;
  }

  small {
    flex-shrink: 0;
    color: ${({ theme }) => theme.input.label.color};
    white-space: nowrap;
  }

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.S}px) {
    li {
      flex-direction: column;
      gap: 4px;
    }

    small {
      white-space: normal;
    }
  }
`;

const CheckboxLabel = styled.label`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  color: ${({ theme }) => theme.default.color.normal};
  overflow-wrap: anywhere;
`;

const TextArea = styled.textarea`
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-height: 96px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.textArea.borderColor.normal};
  color: ${({ theme }) => theme.default.color.normal};
  background-color: ${({ theme }) => theme.schema.backgroundColor.textarea};
  resize: vertical;

  &::placeholder {
    color: ${({ theme }) => theme.textArea.color.placeholder.normal};
  }

  &:hover {
    border-color: ${({ theme }) => theme.textArea.borderColor.hover};
  }

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.textArea.borderColor.focus};
  }
`;

const Stats = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 140px), 1fr));
  gap: 10px;
  min-width: 0;
  margin-bottom: 16px;
`;

const Stat = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid ${({ theme }) => theme.layout.stuffBorderColor};
  border-radius: 10px;
  color: ${({ theme }) => theme.default.color.normal};

  b {
    font-size: 22px;
    overflow-wrap: anywhere;
  }

  span {
    color: ${({ theme }) => theme.input.label.color};
    overflow-wrap: anywhere;
  }
`;

const TableWrapper = styled.div`
  max-width: 100%;
  overflow-x: auto;
  border: 1px solid ${({ theme }) => theme.layout.stuffBorderColor};
  border-radius: 10px;
`;

const PreviewTable = styled.table`
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  table-layout: fixed;
  color: ${({ theme }) => theme.default.color.normal};

  th {
    color: ${({ theme }) => theme.input.label.color};
    font-weight: 600;
    background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  }

  th,
  td {
    padding: 10px;
    border-bottom: 1px solid ${({ theme }) => theme.layout.stuffBorderColor};
    text-align: left;
    vertical-align: top;
    overflow-wrap: anywhere;
    word-break: break-word;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Errors = styled.ul`
  margin: 14px 0 0;
  padding: 12px 12px 12px 28px;
  border: 1px solid ${({ theme }) => theme.circularAlert.color.error};
  border-radius: 10px;
  color: ${({ theme }) => theme.circularAlert.color.error};
  overflow-wrap: anywhere;
`;

export default Upload;
