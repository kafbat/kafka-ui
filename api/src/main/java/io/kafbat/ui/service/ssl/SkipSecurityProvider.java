package io.kafbat.ui.service.ssl;

import java.security.Provider;

public class SkipSecurityProvider extends Provider {
  public static final String NAME = "Skip";

  public SkipSecurityProvider() {
    super(NAME, "1.0", "Skip TrustManagerFactory Provider");
    put("TrustManagerFactory." + NAME, "io.kafbat.ui.service.ssl.SkipTrustManagerFactorySpi");
  }
}
