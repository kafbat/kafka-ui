package io.kafbat.ui.config.auth;

import java.util.Collection;

public interface RbacUser {
  String name();

  Collection<String> groups();

}
