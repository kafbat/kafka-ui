package io.kafbat.ui.model.rbac;

import static io.kafbat.ui.model.rbac.permission.SchemaAction.MODIFY_GLOBAL_COMPATIBILITY;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.google.common.base.Preconditions;
import io.kafbat.ui.model.ActionDTO;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Permission {

  Resource resource;

  List<String> actions;
  transient List<PermissibleAction> parsedActions; //includes all dependant actions

  @Nullable
  String value;
  @Nullable
  transient Pattern compiledValuePattern;

  @SuppressWarnings("unused")
  public void setResource(String resource) {
    this.resource = Resource.fromString(resource.toUpperCase());
  }

  @SuppressWarnings("unused")
  public void setValue(@Nullable String value) {
    this.value = value;
  }

  @SuppressWarnings("unused")
  public void setActions(List<String> actions) {
    this.actions = actions;
  }

  /**
   * Validates required fields and gives migration guidance for named-resource permissions.
   *
   * @throws NullPointerException if the resource is absent
   * @throws IllegalArgumentException if actions are absent or a required value is missing
   */
  public void validate() {
    Preconditions.checkNotNull(resource, "resource cannot be null");
    Preconditions.checkArgument(isNotEmpty(actions), "Actions list for %s can't be null or empty", resource);
    boolean requiresValue = switch (resource) {
      case TOPIC, CONSUMER, CONNECT, CONNECTOR -> true;
      case SCHEMA -> actions.stream().anyMatch(action -> !MODIFY_GLOBAL_COMPATIBILITY.name().equalsIgnoreCase(action));
      default -> false;
    };
    boolean hasValue = value != null && !value.isEmpty();
    if (!hasValue && requiresValue && resource == Resource.SCHEMA
        && actions.stream().anyMatch(action -> ActionDTO.ALL.name().equalsIgnoreCase(action)
            || MODIFY_GLOBAL_COMPATIBILITY.name().equalsIgnoreCase(action))) {
      throw new IllegalArgumentException("Value for resource SCHEMA can't be null or empty; "
          + "move MODIFY_GLOBAL_COMPATIBILITY to a separate permission without a value "
          + "and specify the intended value pattern for named schema actions");
    }
    Preconditions.checkArgument(!requiresValue || hasValue,
        "Value for resource %s can't be null or empty; specify the intended value pattern", resource);
  }

  public void transform() {
    if (value != null) {
      this.compiledValuePattern = Pattern.compile(value);
    }
    if (actions.stream().anyMatch(ActionDTO.ALL.name()::equalsIgnoreCase)) {
      this.parsedActions = resource.allActions();
    } else {
      this.parsedActions = resource.parseActionsWithDependantsUnnest(actions);
    }
  }

}
