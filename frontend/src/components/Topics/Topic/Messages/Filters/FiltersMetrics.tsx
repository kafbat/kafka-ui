import React, { FC } from 'react';
import { PollingMode, TopicMessageConsuming } from 'generated-sources';
import FlexBox from 'components/common/FlexBox/FlexBox';
import ClockIcon from 'components/common/Icons/ClockIcon';
import ArrowDownIcon from 'components/common/Icons/ArrowDownIcon';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';
import FileIcon from 'components/common/Icons/FileIcon';
import { Button } from 'components/common/Button/Button';
import { formatBytes } from 'components/common/BytesFormatted/utils';
import { usePaginateTopics } from 'lib/hooks/useMessagesFilters';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';

import { isLiveMode } from './utils';
import * as S from './Filters.styled';

export interface FiltersMetricsProps {
  mode: PollingMode;
  isFetching: boolean;
  phaseMessage?: string;
  abortFetchData: () => void;
  onDownloadMessage: () => void;
  consumptionStats: TopicMessageConsuming;
}

/** Displays polling statistics and recovery actions when a byte limit is reached. */
const FiltersMetrics: FC<FiltersMetricsProps> = ({
  mode,
  isFetching,
  phaseMessage,
  abortFetchData,
  onDownloadMessage,
  consumptionStats,
}) => {
  const paginate = usePaginateTopics();
  const nextCursor = useMessageFiltersStore((state) => state.nextCursor);
  const {
    bytesLimitReached,
    bytesLimit,
    messagesConsumed = 0,
  } = consumptionStats;
  const formattedBytesLimit = formatBytes(bytesLimit);
  const messagesLoaded =
    messagesConsumed === 1
      ? '1 message loaded.'
      : `${messagesConsumed} messages loaded.`;

  return (
    <>
      {bytesLimitReached && (
        <S.Warning role="alert">
          <S.WarningContent>
            <S.WarningTitle>
              {`Loading paused at the ${formattedBytesLimit} safety limit`}
            </S.WarningTitle>
            <S.WarningDescription>
              {`${messagesLoaded} More messages may be available. Each request uses the same limit.`}
            </S.WarningDescription>
          </S.WarningContent>
          <S.WarningActions>
            <Button
              buttonType="primary"
              buttonSize="M"
              disabled={isFetching || !nextCursor}
              onClick={paginate}
            >
              {`Load next ${formattedBytesLimit}`}
            </Button>
            <Button
              buttonType="secondary"
              buttonSize="M"
              onClick={onDownloadMessage}
            >
              Download by offset
            </Button>
          </S.WarningActions>
        </S.Warning>
      )}
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
          <span>
            {consumptionStats.messagesConsumed === 1
              ? '1 message consumed'
              : `${consumptionStats.messagesConsumed || 0} messages consumed`}
          </span>
        </S.Metric>
        {!!consumptionStats.filterApplyErrors && (
          <S.Metric title="Errors">
            <span>{consumptionStats.filterApplyErrors} errors</span>
          </S.Metric>
        )}
      </FlexBox>
    </>
  );
};

export default FiltersMetrics;
