package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Subject {

  Provider provider;
  @Setter
  String type;
  @Setter
  String value;
  @Setter
  boolean isRegex;

  public void setProvider(String provider) {
    this.provider = Provider.fromString(provider.toUpperCase());
  }

  public void validate() {
    checkNotNull(type, "Subject type cannot be null");
    checkNotNull(value, "Subject value cannot be null");

    checkArgument(!type.isEmpty(), "Subject type cannot be empty");
    checkArgument(!value.isEmpty(), "Subject value cannot be empty");
  }

  public boolean matches(final String attribute) {
    if (isRegex()) {
      return Objects.nonNull(attribute) && attribute.matches(getValue());
    }
    return getValue().equalsIgnoreCase(attribute);
  }
}
