import { Option } from 'react-multi-select-component';

export interface FormValues {
  principal: string;
  host: string;
  inputTopics: Option[];
  outputTopics: Option[];
  applicationId: string;
}
