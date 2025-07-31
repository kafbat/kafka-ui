import 'react-datepicker/dist/react-datepicker.css';

import {
  SerdeUsage,
  TopicMessageConsuming,
  TopicMessage,
} from 'generated-sources';
import React, { ChangeEvent, useMemo, useState } from 'react';
import { format } from 'date-fns';
import MultiSelect from 'components/common/MultiSelect/MultiSelect.styled';
import Select from 'components/common/Select/Select';
import { Button } from 'components/common/Button/Button';
import Search from 'components/common/Search/Search';
import PlusIcon from 'components/common/Icons/PlusIcon';
import { getSerdeOptions } from 'components/Topics/Topic/SendMessage/utils';
import { useSerdes } from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import { useMessagesFilters } from 'lib/hooks/useMessagesFilters';
import { ModeOptions } from 'lib/hooks/filterUtils';
import { useTopicDetails } from 'lib/hooks/api/topics';
import EditIcon from 'components/common/Icons/EditIcon';
import CloseIcon from 'components/common/Icons/CloseIcon';
import FlexBox from 'components/common/FlexBox/FlexBox';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';
import useDataSaver from 'lib/hooks/useDataSaver';

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

type DownloadFormat = 'json' | 'csv';

function padCurrentDateTimeString(): string {
  const now: Date = new Date();
  const dateTimeString: string = format(now, 'yyyy-MM-dd HH:mm:ss');
  return `_${dateTimeString}`;
}

export interface FiltersProps {
  phaseMessage?: string;
  consumptionStats?: TopicMessageConsuming;
  isFetching: boolean;
  abortFetchData: () => void;
  messages?: TopicMessage[];
}

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
  } = useMessagesFilters();

  const { data: topic } = useTopicDetails({ clusterName, topicName });
  const [createdEditedSmartId, setCreatedEditedSmartId] = useState<string>();
  const remove = useMessageFiltersStore(
    (state: { remove: (id: string) => void }) => state.remove
  );

  // Download functionality
  const [showFormatSelector, setShowFormatSelector] = useState(false);

  const formatOptions = [
    { label: 'Export JSON', value: 'json' as DownloadFormat },
    { label: 'Export CSV', value: 'csv' as DownloadFormat },
  ];

  const baseFileName = `topic-messages${padCurrentDateTimeString()}`;

  const savedMessagesJson: MessageData[] = messages.map(
    (message: TopicMessage) => ({
      Value: message.content,
      Offset: message.offset,
      Key: message.key,
      Partition: message.partition,
      Headers: message.headers,
      Timestamp: message.timestamp,
    })
  );

  const convertToCSV = useMemo(() => {
    return (messagesData: MessageData[]) => {
      const headers = [
        'Value',
        'Offset',
        'Key',
        'Partition',
        'Headers',
        'Timestamp',
      ] as const;
      const rows = messagesData.map((msg) =>
        headers
          .map((header) => {
            const value = msg[header];
            if (header === 'Headers') {
              return JSON.stringify(value || {});
            }
            return String(value ?? '');
          })
          .join(',')
      );
      return [headers.join(','), ...rows].join('\n');
    };
  }, []);

  const jsonSaver = useDataSaver(
    `${baseFileName}.json`,
    JSON.stringify(savedMessagesJson, null, '\t')
  );
  const csvSaver = useDataSaver(
    `${baseFileName}.csv`,
    convertToCSV(savedMessagesJson)
  );

  const handleFormatSelect = (downloadFormat: DownloadFormat) => {
    setShowFormatSelector(false);

    // Automatically download after format selection
    if (downloadFormat === 'json') {
      jsonSaver.saveFile();
    } else {
      csvSaver.saveFile();
    }
  };

  const handleDownloadClick = () => {
    setShowFormatSelector(!showFormatSelector);
  };

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

        // eslint-disable-next-line no-param-reassign
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

  const handleRefresh = () => {
    if (isLiveMode(mode) && isFetching) {
      abortFetchData();
    }
    refreshData();
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
          <div style={{ position: 'relative' }}>
            <Button
              disabled={isFetching || messages.length === 0}
              buttonType="secondary"
              buttonSize="M"
              onClick={handleDownloadClick}
              style={{
                minWidth: '40px',
                padding: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 12"
                fill="none"
                stroke="currentColor"
                strokeWidth="1"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
	      <path d="M16,9.5v4A2.5,2.5,0,0,1,13.5,16H2.5A2.5,2.5,0,0,1,0,13.5V2.5A2.5,2.5,0,0,1,2.5,0h3A.5.5,0,0,1,6,.5a.5.5,0,0,1-.5.5h-3A1.5,1.5,0,0,0,1,2.5v11A1.5,1.5,0,0,0,2.5,15h11A1.5,1.5,0,0,0,15,13.5v-4a.5.5,0,0,1,1,0ZM5,9.5a.5.5,0,0,0,1,0v-2A3.5,3.5,0,0,1,9.5,4h4.79L12.15,6.15a.48.48,0,0,0,0,.7.48.48,0,0,0,.7,0l3-3A.36.36,0,0,0,16,3.69a.5.5,0,0,0,0-.38.36.36,0,0,0-.11-.16l-3-3a.48.48,0,0,0-.7,0,.48.48,0,0,0,0,.7L14.29,3H9.5A4.51,4.51,0,0,0,5,7.5Z"/>
              </svg>{' '}
              Export
            </Button>
            {showFormatSelector && (
              <div
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: '0',
                  zIndex: 1000,
                  backgroundColor: 'white',
                  border: '1px solid #ccc',
                  borderRadius: '4px',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                  padding: '8px',
                  minWidth: '120px',
                }}
              >
                {formatOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => handleFormatSelect(option.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        handleFormatSelect(option.value);
                      }
                    }}
                    style={{
                      padding: '8px 12px',
                      cursor: 'pointer',
                      borderRadius: '4px',
                      fontSize: '12px',
                      border: 'none',
                      background: 'transparent',
                      width: '100%',
                      textAlign: 'left',
                    }}
                    onMouseEnter={(e) => {
                      const target = e.currentTarget;
                      target.style.backgroundColor = '#f5f5f5';
                    }}
                    onMouseLeave={(e) => {
                      const target = e.currentTarget;
                      target.style.backgroundColor = 'transparent';
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            )}
          </div>
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
                remove(smartFilter.id);
              }}
              disabled={!!createdEditedSmartId}
            >
              <CloseIcon />
            </S.DeleteSmartFilterIcon>
          </S.ActiveSmartFilter>
        )}
      </FlexBox>
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
          consumptionStats={consumptionStats}
        />
      )}
    </FlexBox>
  );
};

export default Filters;
