import { Topic } from 'generated-sources';
import { LagTrend } from 'lib/consumerGroups';

export type ConsumerGroupTopicsTableRow = {
  topicName: Topic['name'];
  consumerLag: string | number;
  lagTrend: LagTrend;
};
