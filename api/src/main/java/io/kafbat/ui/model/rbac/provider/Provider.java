package io.kafbat.ui.model.rbac.provider;

import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum Provider {

  OAUTH_GOOGLE,
  OAUTH_GITHUB,

  OAUTH_COGNITO,

  OAUTH,

  LDAP,
  LDAP_AD;

  @Nullable
  public static Provider fromString(String name) {
    return EnumUtils.getEnum(Provider.class, name);
  }

  public static class Name {
    public static final String GOOGLE = "google";
    public static final String GITHUB = "github";
    public static final String COGNITO = "cognito";

    public static final String OAUTH = "oauth";
  }

}
