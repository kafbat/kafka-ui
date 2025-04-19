import { object, string, number, array, boolean, mixed, lazy } from 'yup';
import { ApplicationConfigPropertiesKafkaMaskingTypeEnum } from 'generated-sources';

const requiredString = string().required('required field');

const portSchema = number()
  .positive('positive only')
  .typeError('numbers only')
  .required('required');

const bootstrapServerSchema = object({
  host: requiredString,
  port: portSchema,
});

const sslSchema = lazy((value) => {
  if (typeof value === 'object') {
    return object({
      location: string().when('password', {
        is: (v: string) => !!v,
        then: (schema) => schema.required('required field'),
      }),
      password: string(),
    });
  }
  return mixed().optional();
});

const urlWithAuthSchema = lazy((value) => {
  if (typeof value === 'object') {
    return object({
      url: requiredString,
      isAuth: boolean(),
      username: string().when('isAuth', {
        is: true,
        then: (schema) => schema.required('required field'),
      }),
      password: string().when('isAuth', {
        is: true,
        then: (schema) => schema.required('required field'),
      }),
      keystore: sslSchema,
    });
  }
  return mixed().optional();
});

const serdeSchema = object({
  name: requiredString,
  className: requiredString,
  filePath: requiredString,
  topicKeysPattern: requiredString,
  topicValuesPattern: requiredString,
  properties: array().of(
    object({
      key: requiredString,
      value: requiredString,
    })
  ),
});

const serdesSchema = lazy((value) => {
  if (Array.isArray(value)) {
    return array().of(serdeSchema);
  }
  return mixed().optional();
});

const kafkaConnectSchema = object({
  name: requiredString,
  address: requiredString,
  isAuth: boolean(),
  username: string().when('isAuth', {
    is: true,
    then: (schema) => schema.required('required field'),
  }),
  password: string().when('isAuth', {
    is: true,
    then: (schema) => schema.required('required field'),
  }),
  keystore: sslSchema,
});

const kafkaConnectsSchema = lazy((value) => {
  if (Array.isArray(value)) {
    return array().of(kafkaConnectSchema);
  }
  return mixed().optional();
});

const metricsSchema = lazy((value) => {
  if (typeof value === 'object') {
    return object({
      type: string().oneOf(['JMX', 'PROMETHEUS']).required('required field'),
      port: portSchema,
      isAuth: boolean(),
      username: string().when('isAuth', {
        is: true,
        then: (schema) => schema.required('required field'),
      }),
      password: string().when('isAuth', {
        is: true,
        then: (schema) => schema.required('required field'),
      }),
      keystore: sslSchema,
    });
  }
  return mixed().optional();
});

const authPropsSchema = lazy((_, { parent }) => {
  switch (parent.method) {
    case 'SASL/JAAS':
      return object({
        saslJaasConfig: requiredString,
        saslMechanism: requiredString,
      });
    case 'SASL/GSSAPI':
      return object({
        saslKerberosServiceName: requiredString,
        keyTabFile: string(),
        storeKey: boolean(),
        principal: requiredString,
      });
    case 'SASL/OAUTHBEARER':
      return object({
        unsecuredLoginStringClaim_sub: requiredString,
      });
    case 'SASL/PLAIN':
    case 'SASL/SCRAM-256':
    case 'SASL/SCRAM-512':
    case 'SASL/LDAP':
      return object({
        username: requiredString,
        password: requiredString,
      });
    case 'Delegation tokens':
      return object({
        tokenId: requiredString,
        tokenValue: requiredString,
      });
    case 'SASL/AWS IAM':
      return object({
        awsProfileName: string(),
      });
    case 'SASL/Azure Entra':
    case 'SASL/GCP IAM':
    case 'mTLS':
    default:
      return mixed().optional();
  }
});

const authSchema = lazy((value) => {
  if (typeof value === 'object') {
    return object({
      method: string()
        .required('required field')
        .oneOf([
          'SASL/JAAS',
          'SASL/GSSAPI',
          'SASL/OAUTHBEARER',
          'SASL/PLAIN',
          'SASL/SCRAM-256',
          'SASL/SCRAM-512',
          'Delegation tokens',
          'SASL/LDAP',
          'SASL/AWS IAM',
          'SASL/Azure Entra',
          'mTLS',
        ]),
      securityProtocol: string()
        .oneOf(['SASL_SSL', 'SASL_PLAINTEXT'])
        .when('method', {
          is: (v: string) => {
            return [
              'SASL/JAAS',
              'SASL/GSSAPI',
              'SASL/OAUTHBEARER',
              'SASL/PLAIN',
              'SASL/SCRAM-256',
              'SASL/SCRAM-512',
              'SASL/LDAP',
              'SASL/AWS IAM',
              'SASL/Azure Entra',
            ].includes(v);
          },
          then: (schema) => schema.required('required field'),
        }),
      keystore: lazy((_, { parent }) => {
        if (parent.method === 'mTLS') {
          return object({
            location: requiredString,
            password: string(),
          });
        }
        return mixed().optional();
      }),
      props: authPropsSchema,
    });
  }
  return mixed().optional();
});

const maskingSchema = object({
  type: mixed<ApplicationConfigPropertiesKafkaMaskingTypeEnum>()
    .oneOf(Object.values(ApplicationConfigPropertiesKafkaMaskingTypeEnum))
    .required('required field'),
  fields: array().of(
    object().shape({
      value: string().test(
        'fieldsOrPattern',
        'Either fields or fieldsNamePattern is required',
        (value, { path, parent, ...ctx }) => {
          const maskingItem = ctx.from?.[1].value;

          if (value && value.trim() !== '') {
            return true;
          }

          const otherFieldHasValue =
            maskingItem.fields &&
            maskingItem.fields.some(
              (field: { value: string }) =>
                field.value && field.value.trim() !== ''
            );

          if (otherFieldHasValue) {
            return true;
          }

          const hasPattern =
            maskingItem.fieldsNamePattern &&
            maskingItem.fieldsNamePattern.trim() !== '';

          return hasPattern;
        }
      ),
    })
  ),
  fieldsNamePattern: string().test(
    'fieldsOrPattern',
    'Either fields or fieldsNamePattern is required',
    (value, { parent }) => {
      const hasValidFields =
        parent.fields &&
        parent.fields.length > 0 &&
        parent.fields.some(
          (field: { value: string }) => field.value && field.value.trim() !== ''
        );

      const hasPattern = value && value.trim() !== '';

      return hasValidFields || hasPattern;
    }
  ),
  maskingCharsReplacement: array().of(object().shape({ value: string() })),
  replacement: string(),
  topicKeysPattern: string(),
  topicValuesPattern: string(),
});

const maskingsSchema = lazy((value) => {
  if (Array.isArray(value)) {
    return array().of(maskingSchema);
  }
  return mixed().optional();
});

const formSchema = object({
  name: string()
    .required('required field')
    .min(3, 'Cluster name must be at least 3 characters'),
  readOnly: boolean().required('required field'),
  bootstrapServers: array().of(bootstrapServerSchema).min(1),
  truststore: sslSchema,
  auth: authSchema,
  schemaRegistry: urlWithAuthSchema,
  ksql: urlWithAuthSchema,
  serde: serdesSchema,
  kafkaConnect: kafkaConnectsSchema,
  masking: maskingsSchema,
  metrics: metricsSchema,
});

export default formSchema;
