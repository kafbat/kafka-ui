import { Topic } from 'generated-sources';

export type ConsumerGroupTopicsTableRow = {
  topicName: Topic['name'];
  consumerLag: string | number;
};
