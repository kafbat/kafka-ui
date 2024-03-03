import { Option } from 'react-multi-select-component';

export interface FormValues {
  principal: string;
  host: string;
  topics?: Option[];
  topicsPrefix?: string;
  consumerGroups?: Option[];
  consumerGroupsPrefix?: string;
}
