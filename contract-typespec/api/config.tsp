import "@typespec/openapi";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/info")
@summary("Gets application info")
@get
@tag("ApplicationConfig")
op getApplicationInfo(): ApplicationInfo;

@route("/api/config")
@tag("ApplicationConfig")
interface ApplicationConfigApi {
  @summary("Gets current application configuration")
  @get
  @operationId("getCurrentConfig")
  getCurrentConfig(): ApplicationConfig;

  @put
  @summary("Restarts application with specified configuration")
  @operationId("restartWithConfig")
  restartWithConfig(@body config: RestartRequest): void | ApiBadRequestResponse;

  @put
  @route("/validated")
  @summary("Restarts application with specified configuration")
  @operationId("validateConfig")
  validateConfig(@body config: ApplicationConfig): ApplicationConfigValidation | ApiBadRequestResponse;

  @post
  @route("/relatedfiles")
  @summary("Upload config related file")
  @operationId("uploadConfigRelatedFile")
  uploadConfigRelatedFile(
    @multipartBody body: {
      file: HttpPart<bytes>;
    }
  ): UploadedFileInfo;

  @get
  @route("/authentication")
  @summary("Get authentication methods enabled for the app and other related settings")
  @operationId("getAuthenticationSettings")
  getAuthenticationSettings(): AppAuthenticationSettings;
}


model ApplicationInfo {
  enabledFeatures?: ApplicationFeature[];
  build?: {
    commitId?: string;
    version?: string;
    buildTime?: string;
    isLatestRelease?: boolean;
  };
  latestRelease?: {
    versionTag?: string;
    publishedAt?: string;
    htmlUrl?: string;
  };
}

model ApplicationConfig {
  properties: {
    auth?: {
      type: string;
      oauth2?: {
        client?: Record<{
            provider: string;
            clientId: string;
            clientSecret?: string;
            clientName?: string;
            redirectUri?: string;
            authorizationGrantType?: string;
            issuerUri?: string;
            authorizationUri?: string;
            tokenUri?: string;
            userInfoUri?: string;
            jwkSetUri?: string;
            userNameAttribute?: string;
            scope?: string[];
            customParams?: Record<string>;
          }>;
        resourceServer?: {
          jwt?: {
            jwkSetUri?: string;
            jwsAlgorithms?: string[];
            issuerUri?: string;
            publicKeyLocation?: string;
            audiences?: string[];
            authorityPrefix?: string;
            authoritiesClaimDelimiter?: string;
            authoritiesClaimName?: string;
            principalClaimName?: string;
          };
          opaquetoken?: {
            clientId?: string;
            clientSecret?: string;
            introspectionUri?: string;
          };
        };
      };
    };
    rbac?: {
      roles?: {
        name?: string;
        clusters?: string[];
        subjects?: {
            provider?: string;
            type?: string;
            value?: string;
            regex?: boolean = false;
        }[];
        permissions?: RbacPermission[];
      }[];
      defaultRole?: {
        permissions?: RbacPermission[];
      }
    };
    webclient?: {
      maxInMemoryBufferSize?: string;
      responseTimeoutMs?: int32;
    };
    kafka?: {
      polling?: {
        pollTimeoutMs?: int32;
        maxPageSize?: int32;
        defaultPageSize?: int32;
        responseTimeoutMs?: int32;
        maxMessagesToScanPerPoll?: int32;
      };
      adminClientTimeout?: int32;
      internalTopicPrefix?: string;
      defaultMetricsStorage?: ClusterMetricsStoreConfig;
      cache?: {
        enabled?: boolean;
        @format("duration")
        connectClusterCacheExpiry?: string;
      };
      clusters?: {
        name?: string;
        bootstrapServers?: string;
        ssl?: {
          truststoreLocation?: string;
          truststorePassword?: string;
          verify?: boolean = true;
        };
        schemaRegistry?: string;
        schemaRegistryAuth?: {
          username?: string;
          password?: string;
        };
        schemaRegistrySsl?: {
          keystoreLocation?: string;
          keystorePassword?: string;
        };
        ksqldbServer?: string;
        ksqldbServerSsl?: {
          keystoreLocation?: string;
          keystorePassword?: string;
        };
        ksqldbServerAuth?: {
          username?: string;
          password?: string;
        };
        kafkaConnect?: {
          name?: string;
          address?: string;
          username?: string;
          password?: string;
          keystoreLocation?: string;
          keystorePassword?: string;
        }[];
        metrics?: {
          type?: string;
          port?: int32;
          ssl?: boolean;
          username?: string;
          password?: string;
          keystoreLocation?: string;
          keystorePassword?: string;
          prometheusExpose?: boolean;
          store?: ClusterMetricsStoreConfig;
        };
        properties?: Record<unknown>;
        consumerProperties?: Record<unknown>;
        producerProperties?: Record<unknown>;
        readOnly?: boolean;
        serde?: {
          name?: string;
          className?: string;
          filePath?: string;
          properties?: Record<unknown>;
          topicKeysPattern?: string;
          topicValuesPattern?: string;
        }[];
        defaultKeySerde?: string;
        defaultValueSerde?: string;
        masking?: {
          type?: "REMOVE" | "MASK" | "REPLACE";
          fields?: string[];
          fieldsNamePattern?: string;
          maskingCharsReplacement?: string[];
          replacement?: string;
          topicKeysPattern?: string;
          topicValuesPattern?: string;
        }[];
        pollingThrottleRate?: int64;
        audit?: {
          level?: "ALL" | "ALTER_ONLY";
          topic?: string;
          auditTopicsPartitions?: int32;
          topicAuditEnabled?: boolean;
          consoleAuditEnabled?: boolean;
          auditTopicProperties?: Record<string>;
        };
      }[];
    };
  };
}

model ApplicationConfigValidation {
  clusters?: Record<ClusterConfigValidation>;
}

model ApplicationPropertyValidation {
  error: boolean;

  @doc("Contains error message if error = true")
  errorMessage?: string;
}

model ClusterConfigValidation {
  kafka: ApplicationPropertyValidation;
  schemaRegistry?: ApplicationPropertyValidation;
  kafkaConnects?: Record<ApplicationPropertyValidation>;
  ksqldb?: ApplicationPropertyValidation;
  prometheusStorage?: ApplicationPropertyValidation;
}

alias ApplicationFeature =
  "DYNAMIC_CONFIG";

enum AuthType {
  DISABLED,
  OAUTH2,
  LOGIN_FORM,
  LDAP
}

model AppAuthenticationSettings {
  authType?: Api.AuthType;
  oAuthProviders?: OAuthProvider[];
}

model OAuthProvider {
  clientName: string;
  authorizationUri: string;
}

model RestartRequest {
  config?: ApplicationConfig;
}

model UploadedFileInfo {
  location: string;
}

enum Action {
  ALL,
  VIEW,
  EDIT,
  CREATE,
  DELETE,
  RESET_OFFSETS,
  EXECUTE,
  MODIFY_GLOBAL_COMPATIBILITY,
  ANALYSIS_VIEW,
  ANALYSIS_RUN,
  MESSAGES_READ,
  MESSAGES_PRODUCE,
  MESSAGES_DELETE,
  OPERATE,
  RESTART,
}

enum ResourceType {
  APPLICATIONCONFIG,
  CLUSTERCONFIG,
  TOPIC,
  CONSUMER,
  SCHEMA,
  CONNECT,
  CONNECTOR,
  KSQL,
  ACL,
  AUDIT,
  CLIENT_QUOTAS,
}

model ClusterMetricsStoreConfig {
    prometheus?: PrometheusStorage;
}

model PrometheusStorage {
    url?: string;
    remoteWrite?: boolean;
    pushGatewayUrl?: string;
    pushGatewayUsername?: string;
    pushGatewayPassword?: string;
    pushGatewayJobName?: string;
}

model RbacPermission {
    resource?: ResourceType;
    value?: string;
    actions?: Action[];
}
