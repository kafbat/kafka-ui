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
import { useDownloadMessagesZip, useSerdes } from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';

interface Option<T> {
  label: string;
  value: T;
}

type PartitionOption = Option<number>;

type DownloadMode =
  | 'LATEST'
  | 'EARLIEST'
  | 'FROM_OFFSET'
  | 'TO_OFFSET'
  | 'FROM_TIMESTAMP'
  | 'TO_TIMESTAMP'
  | 'TIMEFRAME';

const downloadModeOptions: Option<DownloadMode>[] = [
  { label: 'Newest / last N', value: 'LATEST' },
  { label: 'Oldest / first N', value: 'EARLIEST' },
  { label: 'From offset', value: 'FROM_OFFSET' },
  { label: 'To offset', value: 'TO_OFFSET' },
  { label: 'From time', value: 'FROM_TIMESTAMP' },
  { label: 'To time', value: 'TO_TIMESTAMP' },
  { label: 'Time frame', value: 'TIMEFRAME' },
];

const formatOptions: Option<string>[] = [
  { label: 'Text export', value: 'TEXT' },
  { label: 'JSON metadata + payload', value: 'JSON' },
  { label: 'Payload only', value: 'VALUE_ONLY' },
];

const partitionModeOptions: Option<string>[] = [
  { label: 'All partitions', value: 'ALL' },
  { label: 'Selected partitions', value: 'SELECTED' },
];

const filterOptions = (options: PartitionOption[], filter: string) =>
  options.filter(({ label }) => label.toLowerCase().includes(filter.toLowerCase()));

const toEpochMillis = (value: string) => {
  if (!value) return undefined;

  const date = new Date(value);
  const timestamp = date.getTime();
  return Number.isNaN(timestamp) ? undefined : timestamp.toString();
};

const numericValue = (value: string, fallback: number) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
};

const Download: React.FC = () => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const { data: serdes = {} } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.DESERIALIZE,
  });
  const downloadMessagesZip = useDownloadMessagesZip();

  const [partitionMode, setPartitionMode] = useState('ALL');
  const [selectedPartitions, setSelectedPartitions] = useState<PartitionOption[]>([]);
  const [downloadMode, setDownloadMode] = useState<DownloadMode>('LATEST');
  const [limit, setLimit] = useState('100');
  const [offset, setOffset] = useState('0');
  const [fromTime, setFromTime] = useState('');
  const [toTime, setToTime] = useState('');
  const [format, setFormat] = useState('TEXT');
  const [search, setSearch] = useState('');
  const [smartFilterId, setSmartFilterId] = useState('');
  const [keySerde, setKeySerde] = useState<string | undefined>();
  const [valueSerde, setValueSerde] = useState<string | undefined>();

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

  const isOffsetMode = downloadMode === 'FROM_OFFSET' || downloadMode === 'TO_OFFSET';
  const isFromTimeVisible =
    downloadMode === 'FROM_TIMESTAMP' || downloadMode === 'TIMEFRAME';
  const isToTimeVisible = downloadMode === 'TO_TIMESTAMP' || downloadMode === 'TIMEFRAME';
  const resolvedLimit = numericValue(limit, 100);
  const selectedPartitionValues =
    partitionMode === 'SELECTED'
      ? selectedPartitions.map(({ value }) => value)
      : undefined;

  const handleDownload = () => {
    const resolvedMode = downloadMode === 'TIMEFRAME' ? 'FROM_TIMESTAMP' : downloadMode;
    const timestamp =
      downloadMode === 'TO_TIMESTAMP' ? toEpochMillis(toTime) : toEpochMillis(fromTime);
    const timestampTo = downloadMode === 'TIMEFRAME' ? toEpochMillis(toTime) : undefined;

    downloadMessagesZip.mutate({
      clusterName,
      topicName,
      limit: resolvedLimit,
      partitions: selectedPartitionValues,
      stringFilter: search || undefined,
      smartFilterId: smartFilterId || undefined,
      keySerde,
      valueSerde,
      downloadMode: resolvedMode,
      offset: isOffsetMode ? offset : undefined,
      timestamp,
      timestampTo,
      format,
    });
  };

  return (
    <Page>
      <Hero>
        <div>
          <Eyebrow>Topic export center</Eyebrow>
          <Title>Download messages</Title>
          <Description>
            Export one file per Kafka message as a ZIP with partition, offset,
            timestamp, serde-aware payloads, filters, and window controls.
          </Description>
        </div>
        <Button
          buttonType="primary"
          buttonSize="M"
          onClick={handleDownload}
          disabled={downloadMessagesZip.isPending}
        >
          {downloadMessagesZip.isPending ? 'Preparing ZIP...' : 'Download ZIP'}
        </Button>
      </Hero>

      <Grid>
        <Card>
          <CardTitle>1. Partitions</CardTitle>
          <Field>
            <Label>Scope</Label>
            <Select
              id="downloadPartitionMode"
              options={partitionModeOptions}
              value={partitionMode}
              onChange={setPartitionMode}
              minWidth="100%"
            />
          </Field>
          {partitionMode === 'SELECTED' && (
            <Field>
              <Label>Partition picker</Label>
              <MultiSelect
                options={partitionOptions}
                filterOptions={filterOptions}
                onChange={(value: PartitionOption[]) => setSelectedPartitions(value)}
                value={selectedPartitions}
                minWidth="100%"
                labelledBy="downloadPartitionOptions"
                overrideStrings={{
                  selectSomeItems: 'Select partitions',
                }}
              />
            </Field>
          )}
        </Card>

        <Card>
          <CardTitle>2. Window</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Mode</Label>
              <Select
                id="downloadMode"
                options={downloadModeOptions}
                value={downloadMode}
                onChange={setDownloadMode}
                minWidth="100%"
              />
            </Field>
            <Field>
              <Label>Max messages</Label>
              <Input
                inputSize="M"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={limit}
                onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                  setLimit(value.replace(/\D/g, ''));
                }}
              />
            </Field>
            {isOffsetMode && (
              <Field>
                <Label>Offset</Label>
                <Input
                  inputSize="M"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={offset}
                  onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                    setOffset(value.replace(/\D/g, ''));
                  }}
                />
              </Field>
            )}
            {isFromTimeVisible && (
              <Field>
                <Label>From time</Label>
                <Input
                  inputSize="M"
                  type="datetime-local"
                  value={fromTime}
                  onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                    setFromTime(value);
                  }}
                />
              </Field>
            )}
            {isToTimeVisible && (
              <Field>
                <Label>{downloadMode === 'TIMEFRAME' ? 'To time' : 'At / before time'}</Label>
                <Input
                  inputSize="M"
                  type="datetime-local"
                  value={toTime}
                  onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                    setToTime(value);
                  }}
                />
              </Field>
            )}
          </FlexBox>
        </Card>

        <Card>
          <CardTitle>3. Payload rendering</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Output format</Label>
              <Select
                id="downloadFormat"
                options={formatOptions}
                value={format}
                onChange={setFormat}
                minWidth="100%"
              />
            </Field>
            <Field>
              <Label>Key serde</Label>
              <Select
                id="downloadKeySerde"
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
                id="downloadValueSerde"
                options={getSerdeOptions(serdes.value || [])}
                value={valueSerde}
                onChange={setValueSerde}
                minWidth="100%"
                placeholder="Value Serde"
              />
            </Field>
          </FlexBox>
        </Card>

        <Card>
          <CardTitle>4. Optional filters</CardTitle>
          <FlexBox gap="12px" flexWrap="wrap" alignItems="flex-end">
            <Field>
              <Label>Contains text</Label>
              <Input
                inputSize="M"
                type="text"
                value={search}
                placeholder="Search payload/key/header text"
                onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                  setSearch(value);
                }}
              />
            </Field>
            <Field>
              <Label>Smart filter id</Label>
              <Input
                inputSize="M"
                type="text"
                value={smartFilterId}
                placeholder="Optional registered filter id"
                onChange={({ target: { value } }: ChangeEvent<HTMLInputElement>) => {
                  setSmartFilterId(value);
                }}
              />
            </Field>
          </FlexBox>
        </Card>
      </Grid>
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

  & > button {
    flex: 0 0 auto;
    white-space: nowrap;
  }

  @media screen and (max-width: ${({ theme }) => theme.breakpoints.M}px) {
    flex-direction: column;
    align-items: stretch;

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

const Label = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.input.label.color};
  overflow-wrap: anywhere;
`;

export default Download;
