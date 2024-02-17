package io.kafbat.ui.model.rbac;

import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum Resource {

  APPLICATIONCONFIG,
  CLUSTERCONFIG,
  TOPIC,
  CONSUMER,
  SCHEMA,
  CONNECT,
  KSQL,
  ACL,
  AUDIT,
  CLIENT_QUOTAS;

  @Nullable
  public static Resource fromString(String name) {
    return EnumUtils.getEnum(Resource.class, name);
  }


}
