import React from 'react';
import * as Metrics from 'components/common/Metrics';
import { TopicAnalysisStats } from 'generated-sources';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import { useTimezone } from 'lib/hooks/useTimezones';

const Total: React.FC<TopicAnalysisStats> = ({
  totalMsgs,
  minOffset,
  maxOffset,
  minTimestamp,
  maxTimestamp,
  nullKeys,
  nullValues,
  approxUniqKeys,
  approxUniqValues,
}) => {
  const { currentTimezone } = useTimezone();
  const offsets =
    minOffset === undefined || maxOffset === undefined
      ? 'N/A'
      : `${minOffset} - ${maxOffset}`;
  const timestamps =
    minTimestamp === undefined || maxTimestamp === undefined
      ? 'N/A'
      : `${formatTimestamp({ timestamp: minTimestamp, timezone: currentTimezone.value })} - ${formatTimestamp({ timestamp: maxTimestamp, timezone: currentTimezone.value })}`;

  return (
    <Metrics.Section title="Messages">
      <Metrics.Indicator label="Total number">
        {totalMsgs ?? 0}
      </Metrics.Indicator>
      <Metrics.Indicator label="Offsets min-max">{offsets}</Metrics.Indicator>
      <Metrics.Indicator label="Timestamp min-max">
        {timestamps}
      </Metrics.Indicator>
      <Metrics.Indicator label="Null keys">{nullKeys ?? 0}</Metrics.Indicator>
      <Metrics.Indicator
        label="Unique keys"
        title="Approximate number of unique keys"
      >
        {approxUniqKeys ?? 0}
      </Metrics.Indicator>
      <Metrics.Indicator label="Null values">
        {nullValues ?? 0}
      </Metrics.Indicator>
      <Metrics.Indicator
        label="Unique values"
        title="Approximate number of unique values"
      >
        {approxUniqValues ?? 0}
      </Metrics.Indicator>
    </Metrics.Section>
  );
};

export default Total;
