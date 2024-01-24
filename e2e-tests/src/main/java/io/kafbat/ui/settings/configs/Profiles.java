package io.kafbat.ui.settings.configs;

import io.kafbat.ui.variables.Browser;
import io.kafbat.ui.variables.Suite;
import org.aeonbits.owner.Config;

public interface Profiles extends Config {

  @Key("browser")
  @DefaultValue(Browser.CONTAINER)
  String browser();

  @Key("suite")
  @DefaultValue(Suite.CUSTOM)
  String suite();
}
