import { array, object, string } from 'yup';

const formSchema = object({
  principal: string().required(),
  host: string().required(),
  topics: array().of(
    object().shape({
      label: string().required(),
      value: string().required(),
    })
  ),
  topicsPrefix: string(),
  consumerGroups: array().of(
    object().shape({
      label: string().required(),
      value: string().required(),
    })
  ),
  consumerGroupsPrefix: string(),
});

export default formSchema;
