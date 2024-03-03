import { object, string } from 'yup';

const formSchema = object({
  resourceType: string().required(),
  resourceName: string().required(),
  namePatternType: string().required(),
  principal: string().required(),
  host: string().required(),
  operation: string().required(),
  permission: string().required(),
});

export default formSchema;
