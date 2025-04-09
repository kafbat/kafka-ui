const JAAS_CONFIGS = {
  'SASL/GSSAPI': 'com.sun.security.auth.module.Krb5LoginModule',
  'SASL/OAUTHBEARER':
    'org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule',
  'SASL/PLAIN': 'org.apache.kafka.common.security.plain.PlainLoginModule',
  'SASL/SCRAM-256': 'org.apache.kafka.common.security.scram.ScramLoginModule',
  'SASL/SCRAM-512': 'org.apache.kafka.common.security.scram.ScramLoginModule',
  'Delegation tokens':
    'org.apache.kafka.common.security.scram.ScramLoginModule',
  'SASL/LDAP': 'org.apache.kafka.common.security.plain.PlainLoginModule',
  'SASL/AWS IAM': 'software.amazon.msk.auth.iam.IAMLoginModule',
  'SASL/Azure Entra':
    'org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule',
  'SASL/GCP IAM':
    'com.google.cloud.hosted.kafka.auth.GcpLoginCallbackHandler',
};

type MethodName = keyof typeof JAAS_CONFIGS;

export const getJaasConfig = (
  method: MethodName,
  options: Record<string, string>
) => {
  const optionsString = Object.entries(options)
    .map(([key, value]) => {
      if (value === undefined) return null;
      if (value === 'true' || value === 'false') {
        return ` ${key}=${value}`;
      }
      return ` ${key}="${value}"`;
    })
    .join('');

  return `${JAAS_CONFIGS[method]} required${optionsString};`;
};
