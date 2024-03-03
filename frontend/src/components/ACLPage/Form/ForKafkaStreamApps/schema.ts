import { array, object, string } from 'yup';

const formSchema = object({
  principal: string().required(),
  host: string().required(),
  inputTopics: array().of(string()).required(),
  outputTopics: array().of(string()).required(),
  applicationId: string().required(),
});

export default formSchema;
