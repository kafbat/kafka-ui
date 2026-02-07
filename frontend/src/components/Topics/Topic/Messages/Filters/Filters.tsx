import 'react-datepicker/dist/react-datepicker.css';

import { SerdeUsage, TopicMessageConsuming } from 'generated-sources';
import React, { ChangeEvent, useMemo, useState } from 'react';
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

export interface FiltersProps {
  phaseMessage?: string;
  consumptionStats?: TopicMessageConsuming;
  isFetching: boolean;
  abortFetchData: () => void;
}

const Filters: React.FC<FiltersProps> = ({
  consumptionStats,
  isFetching,
  abortFetchData,
  phaseMessage,
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

        <Search placeholder="Search" value={search} onChange={setSearch} />
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
