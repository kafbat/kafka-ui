package io.kafbat.ui.model.rbac;

import io.kafbat.ui.model.rbac.provider.Provider;
import lombok.Getter;

@Getter
public class Subject {

  Provider provider;
  String type;
  String value;

  public void setProvider(String provider) {
    this.provider = Provider.fromString(provider.toUpperCase());
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
