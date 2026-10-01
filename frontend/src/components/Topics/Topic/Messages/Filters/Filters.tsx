import 'react-datepicker/dist/react-datepicker.css';

import {
  SerdeUsage,
  TopicMessageConsuming,
  TopicMessage,
} from 'generated-sources';
import React, { ChangeEvent, useMemo, useState } from 'react';
import MultiSelect from 'components/common/MultiSelect/MultiSelect.styled';
import Select from 'components/common/Select/Select';
import { Button } from 'components/common/Button/Button';
import Search from 'components/common/Search/Search';
import PlusIcon from 'components/common/Icons/PlusIcon';
import { getSerdeOptions } from 'components/Topics/Topic/SendMessage/utils';
import { downloadTopicMessage, useSerdes } from 'lib/hooks/api/topicMessages';
import { showServerError } from 'lib/errorHandling';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import { useMessagesFilters } from 'lib/hooks/useMessagesFilters';
import { ModeOptions } from 'lib/hooks/filterUtils';
import { useTopicDetails } from 'lib/hooks/api/topics';
import EditIcon from 'components/common/Icons/EditIcon';
import CloseIcon from 'components/common/Icons/CloseIcon';
import FlexBox from 'components/common/FlexBox/FlexBox';
import useDataSaver from 'lib/hooks/useDataSaver';
import ExportIcon from 'components/common/Icons/ExportIcon';
import { Dropdown, DropdownItem } from 'components/common/Dropdown';
import SlidingSidebar from 'components/common/SlidingSidebar';

import * as S from './Filters.styled';
import {
  ADD_FILTER_ID,
  filterOptions,
  isLiveMode,
  isModeOffsetSelector,
  isModeOptionWithInput,
} from './utils';
import FiltersSideBar from './FiltersSideBar';
import FiltersMetrics from './FiltersMetrics';

interface MessageData {
  Value: string | undefined;
  Offset: number;
  Key: string | undefined;
  Partition: number;
  Headers: { [key: string]: string | undefined } | undefined;
  Timestamp: Date;
}

const CSV_COLUMNS = [
  'Value',
  'Offset',
  'Key',
  'Partition',
  'Headers',
  'Timestamp',
] as const;

const FORMULA_TRIGGER = /^[=+\-@\t\r]/;

/** Quotes a CSV cell and neutralizes spreadsheet formula prefixes. */
const toCsvCell = (value: unknown) => {
  const text = String(value ?? '');
  const inert = FORMULA_TRIGGER.test(text) ? `'${text}` : text;
  return `"${inert.replace(/"/g, '""')}"`;
};

/** Serializes topic messages into the export's fixed-column CSV format. */
const convertToCSV = (messagesData: MessageData[]) =>
  [
    CSV_COLUMNS.join(','),
    ...messagesData.map((msg) =>
      CSV_COLUMNS.map((column) =>
        toCsvCell(
          column === 'Headers' ? JSON.stringify(msg[column] || {}) : msg[column]
        )
      ).join(',')
    ),
  ].join('\n');

/** Produces a filesystem-friendly UTC timestamp for exported filenames. */
const fileNameTimestamp = () =>
  new Date().toISOString().slice(0, 19).replace(/:/g, '-');

export interface FiltersProps {
  phaseMessage?: string;
  consumptionStats?: TopicMessageConsuming;
  isFetching: boolean;
  abortFetchData: () => void;
  messages?: TopicMessage[];
}

/** Provides topic-message filters, exports, and exact-offset download controls. */
const Filters: React.FC<FiltersProps> = ({
  consumptionStats,
  isFetching,
  abortFetchData,
  phaseMessage,
  messages = [],
}) => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();

  const {
    mode,
    setMode,
    date,
    setTimeStamp,
    keySerde,
    setKeySerde,
    valueSerde,
    setValueSerde,
    offset,
    setOffsetValue,
    search,
    setSearch,
    partitions: p,
    setPartition,
    smartFilter,
    setSmartFilter,
    refreshData,
  } = useMessagesFilters(topicName);

  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const [createdEditedSmartId, setCreatedEditedSmartId] = useState<string>();
  const { json: exportedJson, csv: exportedCsv } = useMemo(() => {
    const exported: MessageData[] = messages.map((message: TopicMessage) => ({
      Value: message.value,
      Offset: message.offset,
      Key: message.key,
      Partition: message.partition,
      Headers: message.headers,
      Timestamp: message.timestamp,
    }));

    return {
      json: JSON.stringify(exported, null, '\t'),
      csv: convertToCSV(exported),
    };
  }, [messages]);

  const baseFileName = `topic-messages_${fileNameTimestamp()}`;

  const jsonSaver = useDataSaver(`${baseFileName}.json`, exportedJson);
  const csvSaver = useDataSaver(`${baseFileName}.csv`, exportedCsv);
  const [downloadPartition, setDownloadPartition] = useState('');
  const [downloadOffset, setDownloadOffset] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);
  const [isDownloadPaneOpen, setIsDownloadPaneOpen] = useState(false);

  const partitions = useMemo(() => {
    return (topic?.partitions || []).reduce<{
      dict: Record<string, { label: string; value: number }>;
      list: { label: string; value: number }[];
    }>(
      (acc, currentValue) => {
        const label = {
          label: `Partition #${currentValue.partition.toString()}`,
          value: currentValue.partition,
        };

        acc.dict[label.value] = label;
        acc.list.push(label);
        return acc;
      },
      { dict: {}, list: [] }
    );
  }, [topic?.partitions]);

  const partitionValue = useMemo(() => {
    return p.map((value) => partitions.dict[value]);
  }, [p, partitions]);

  const { data: serdes = {}, isLoading } = useSerdes({
    clusterName,
    topicName,
    use: SerdeUsage.DESERIALIZE,
  });

  /** Stops a live request before starting a refresh. */
  const handleRefresh = () => {
    if (isLiveMode(mode) && isFetching) {
      abortFetchData();
    }
    refreshData();
  };

  const parsedDownloadPartition = Number(downloadPartition);
  const parsedDownloadOffset = Number(downloadOffset);
  const canDownloadMessage =
    downloadPartition.trim() !== '' &&
    downloadOffset.trim() !== '' &&
    Number.isInteger(parsedDownloadPartition) &&
    Number.isInteger(parsedDownloadOffset) &&
    parsedDownloadPartition >= 0 &&
    parsedDownloadOffset >= 0;

  /** Downloads the selected partition and offset using the active SerDes. */
  const handleDownloadMessage = async () => {
    if (!canDownloadMessage) return;
    setIsDownloading(true);
    try {
      await downloadTopicMessage({
        clusterName,
        topicName,
        partition: parsedDownloadPartition,
        offset: parsedDownloadOffset,
        keySerde,
        valueSerde,
      });
      setIsDownloadPaneOpen(false);
    } catch (error) {
      showServerError(error as Response);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <FlexBox flexDirection="column" padding="0 16px">
      <FlexBox width="100%" justifyContent="space-between" margin="10px 0 0 0">
        <FlexBox gap="8px" alignItems="flex-end" flexWrap="wrap">
          <S.FilterModeTypeSelectorWrapper>
            <S.FilterModeTypeSelect
              id="selectSeekType"
              onChange={setMode}
              value={mode}
              selectSize="M"
              minWidth="100px"
              options={ModeOptions}
            />

            {isModeOptionWithInput(mode) &&
              (isModeOffsetSelector(mode) ? (
                <S.OffsetSelector
                  id="offset"
                  type="text"
                  inputSize="M"
                  value={offset}
                  placeholder="Offset"
                  aria-label="Filter offset"
                  onChange={({
                    target: { value },
                  }: ChangeEvent<HTMLInputElement>) => {
                    setOffsetValue(value);
                  }}
                />
              ) : (
                <S.DatePickerInput
                  selected={date}
                  onChange={setTimeStamp}
                  showTimeInput
                  timeInputLabel="Time:"
                  dateFormat="MMM d, yyyy"
                  placeholderText="Select timestamp"
                />
              ))}
          </S.FilterModeTypeSelectorWrapper>
          <MultiSelect
            disabled={isLoading}
            options={partitions.list}
            filterOptions={filterOptions}
            onChange={setPartition}
            value={partitionValue}
            labelledBy="partitionsOptions"
            overrideStrings={{
              selectSomeItems: 'Select partitions',
            }}
          />
          <Select
            id="selectKeySerdeOptions"
            aria-labelledby="selectKeySerdeOptions"
            onChange={setKeySerde}
            minWidth="170px"
            options={getSerdeOptions(serdes.key || [])}
            value={keySerde}
            selectSize="M"
            placeholder="Key Serde"
          />
          <Select
            id="selectValueSerdeOptions"
            aria-labelledby="selectValueSerdeOptions"
            onChange={setValueSerde}
            options={getSerdeOptions(serdes.value || [])}
            value={valueSerde}
            minWidth="170px"
            selectSize="M"
            placeholder="Value Serde"
          />
          <Button
            type="submit"
            buttonType="secondary"
            buttonSize="M"
            onClick={handleRefresh}
            style={{ fontWeight: 500 }}
          >
            Refresh
          </Button>
        </FlexBox>

        <FlexBox gap="8px" alignItems="center">
          <Search placeholder="Search" value={search} onChange={setSearch} />
          <Dropdown
            disabled={isFetching || messages.length === 0}
            aria-label="Export messages"
            openBtnEl={
              <Button buttonType="secondary" buttonSize="M">
                <ExportIcon />
                Export
              </Button>
            }
          >
            <DropdownItem onClick={jsonSaver.saveFile}>
              Export JSON
            </DropdownItem>
            <DropdownItem onClick={csvSaver.saveFile}>Export CSV</DropdownItem>
          </Dropdown>
        </FlexBox>
      </FlexBox>
      <FlexBox
        gap="10px"
        alignItems="center"
        justifyContent="flex-start"
        padding="8px 0 5px"
      >
        <Button
          buttonType="secondary"
          buttonSize="M"
          onClick={() => setCreatedEditedSmartId(ADD_FILTER_ID)}
        >
          <PlusIcon />
          Add Filters
        </Button>
        <Button
          buttonType="secondary"
          buttonSize="M"
          onClick={() => setIsDownloadPaneOpen(true)}
        >
          Download message
        </Button>
        {smartFilter && (
          <S.ActiveSmartFilter data-testid="activeSmartFilter">
            <S.SmartFilterName>{smartFilter.id}</S.SmartFilterName>
            <S.EditSmartFilterIcon
              onClick={() => setCreatedEditedSmartId(smartFilter.id)}
              disabled={!!createdEditedSmartId}
            >
              <EditIcon />
            </S.EditSmartFilterIcon>
            <S.DeleteSmartFilterIcon
              onClick={() => {
                setSmartFilter(null);
              }}
              disabled={!!createdEditedSmartId}
            >
              <CloseIcon />
            </S.DeleteSmartFilterIcon>
          </S.ActiveSmartFilter>
        )}
      </FlexBox>
      <SlidingSidebar
        open={isDownloadPaneOpen}
        onClose={() => setIsDownloadPaneOpen(false)}
        title="Download message"
      >
        <S.DownloadPaneForm>
          <S.DownloadPaneDescription>
            Enter a partition and offset to download a specific message without
            expanding it in the table.
          </S.DownloadPaneDescription>
          <S.DownloadPaneField>
            <S.DownloadPaneLabel htmlFor="download-partition">
              Partition
            </S.DownloadPaneLabel>
            <S.ManualDownloadInput
              id="download-partition"
              type="number"
              min="0"
              inputSize="M"
              placeholder="Partition"
              value={downloadPartition}
              onChange={({ target: { value } }) => setDownloadPartition(value)}
            />
          </S.DownloadPaneField>
          <S.DownloadPaneField>
            <S.DownloadPaneLabel htmlFor="download-offset">
              Offset
            </S.DownloadPaneLabel>
            <S.ManualDownloadInput
              id="download-offset"
              type="number"
              min="0"
              inputSize="M"
              placeholder="Offset"
              value={downloadOffset}
              onChange={({ target: { value } }) => setDownloadOffset(value)}
            />
          </S.DownloadPaneField>
          <S.DownloadPaneActions>
            <Button
              buttonType="secondary"
              buttonSize="M"
              onClick={() => setIsDownloadPaneOpen(false)}
            >
              Cancel
            </Button>
            <Button
              buttonType="primary"
              buttonSize="M"
              disabled={!canDownloadMessage || isDownloading}
              inProgress={isDownloading}
              onClick={handleDownloadMessage}
            >
              Download message
            </Button>
          </S.DownloadPaneActions>
        </S.DownloadPaneForm>
      </SlidingSidebar>
      <FiltersSideBar
        setClose={() => setCreatedEditedSmartId('')}
        smartFilter={smartFilter}
        setSmartFilter={setSmartFilter}
        setFilterName={setCreatedEditedSmartId}
        filterName={createdEditedSmartId}
      />
      {consumptionStats && (
        <FiltersMetrics
          mode={mode}
          isFetching={isFetching}
          phaseMessage={phaseMessage}
          abortFetchData={abortFetchData}
          onDownloadMessage={() => setIsDownloadPaneOpen(true)}
          consumptionStats={consumptionStats}
        />
      )}
    </FlexBox>
  );
};

export default Filters;
