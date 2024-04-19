import React, { FC } from 'react';
import { PollingMode, TopicMessageConsuming } from 'generated-sources';
import FlexBox from 'components/common/FlexBox/FlexBox';
import ClockIcon from 'components/common/Icons/ClockIcon';
import ArrowDownIcon from 'components/common/Icons/ArrowDownIcon';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';
import FileIcon from 'components/common/Icons/FileIcon';

import { isLiveMode } from './utils';
import * as S from './Filters.styled';

export interface FiltersMetricsProps {
  mode: PollingMode;
  isFetching: boolean;
  phaseMessage?: string;
  abortFetchData: () => void;
  consumptionStats: TopicMessageConsuming;
}

const FiltersMetrics: FC<FiltersMetricsProps> = ({
  mode,
  isFetching,
  phaseMessage,
  abortFetchData,
  consumptionStats,
}) => {
  return (
    <FlexBox
      justifyContent="flex-end"
      alignItems="center"
      gap="22px"
      padding="16px 0"
    >
      <S.Message>{!isLiveMode(mode) && isFetching && phaseMessage}</S.Message>
      <S.MessageLoading isLive={isLiveMode(mode) && isFetching}>
        <S.MessageLoadingSpinner isFetching={isFetching} />
        Loading messages...
        <S.StopLoading onClick={abortFetchData}>Stop loading</S.StopLoading>
      </S.MessageLoading>
      <S.Message />
      <S.Metric title="Elapsed Time">
        <S.MetricsIcon>
          <ClockIcon />
        </S.MetricsIcon>
        <span>{Math.max(consumptionStats.elapsedMs || 0, 0)} ms</span>
      </S.Metric>
      <S.Metric title="Bytes Consumed">
        <S.MetricsIcon>
          <ArrowDownIcon />
        </S.MetricsIcon>
        <BytesFormatted value={consumptionStats.bytesConsumed} />
      </S.Metric>
      <S.Metric title="Messages Consumed">
        <S.MetricsIcon>
          <FileIcon />
        </S.MetricsIcon>
        <span>{consumptionStats.messagesConsumed} messages consumed</span>
      </S.Metric>
      {!!consumptionStats.filterApplyErrors && (
        <S.Metric title="Errors">
          <span>{consumptionStats.filterApplyErrors} errors</span>
        </S.Metric>
      )}
    </FlexBox>
  );
};

export default FiltersMetrics;
