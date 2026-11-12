package io.kafbat.ui.model.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PermissionTest {

  /**
   * Ensures action expansion does not make a missing pattern select every resource name.
   */
  @ParameterizedTest
  @EnumSource(value = Resource.class, names = {"TOPIC", "CONSUMER", "SCHEMA", "CONNECT", "CONNECTOR"})
  void validateRejectsMissingValueForNamedResources(Resource resource) {
    var p = new Permission();
    p.setResource(resource.name());
    p.setActions(List.of("all"));

    assertThatIllegalArgumentException().isThrownBy(p::validate)
        .withMessageContaining(resource.name())
        .withMessageContaining("null or empty");
  }

  /**
   * Rejects an empty pattern that cannot match a non-empty resource name.
   */
  @ParameterizedTest
  @EnumSource(value = Resource.class, names = {"TOPIC", "CONSUMER", "SCHEMA", "CONNECT", "CONNECTOR"})
  void validateRejectsEmptyValueForNamedResources(Resource resource) {
    var p = new Permission();
    p.setResource(resource.name());
    p.setActions(List.of("view"));
    p.setValue("");

    assertThatIllegalArgumentException().isThrownBy(p::validate)
        .withMessageContaining(resource.name());
  }

  /**
   * Keeps explicit wildcard patterns valid for operators who intend access to all names.
   */
  @ParameterizedTest
  @EnumSource(value = Resource.class, names = {"TOPIC", "CONSUMER", "SCHEMA", "CONNECT", "CONNECTOR"})
  void validateAcceptsWildcardForNamedResources(Resource resource) {
    var p = new Permission();
    p.setResource(resource.name());
    p.setActions(List.of("all"));
    p.setValue(".*");

    assertThatCode(p::validate).doesNotThrowAnyException();
  }

  /**
   * Preserves permissions whose actions do not target named resources.
   */
  @ParameterizedTest
  @EnumSource(value = Resource.class, names = {"TOPIC", "CONSUMER", "SCHEMA", "CONNECT", "CONNECTOR"},
      mode = EnumSource.Mode.EXCLUDE)
  void validateAcceptsUnnamedResourcesWithoutValue(Resource resource) {
    var p = new Permission();
    p.setResource(resource.name());
    p.setActions(List.of("all"));

    assertThatCode(p::validate).doesNotThrowAnyException();
  }

  /**
   * Preserves the value-free permission needed for the registry-wide compatibility check.
   */
  @Test
  void validatePreservesGlobalSchemaCompatibilityPermissionWithoutValue() {
    var p = new Permission();
    p.setResource("schema");
    p.setActions(List.of("modify_global_compatibility"));

    p.validate();
    p.transform();

    assertThat(AccessContext.builder().schemaGlobalCompatChange().build().isAccessible(List.of(p))).isTrue();
  }

  /**
   * Directs mixed schema permissions to separate global and named entries, including action expansion.
   */
  @ParameterizedTest
  @ValueSource(strings = {"MODIFY_GLOBAL_COMPATIBILITY", "modify_global_compatibility", "ALL", "all"})
  void validateRequiresValueWhenGlobalSchemaActionIsCombinedWithNamedActions(String globalAction) {
    var p = new Permission();
    p.setResource("schema");
    p.setActions(List.of(globalAction, "VIEW"));

    assertThatThrownBy(p::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SCHEMA")
        .hasMessageContaining("separate permission without a value")
        .hasMessageContaining("intended value pattern")
        .hasMessageNotContaining(".*");
  }

  /**
   * Gives empty mixed schema entries the same migration guidance as entries with no value.
   */
  @Test
  void validateExplainsHowToSplitEmptyMixedSchemaPermission() {
    var p = new Permission();
    p.setResource("schema");
    p.setActions(List.of("MODIFY_GLOBAL_COMPATIBILITY", "VIEW"));
    p.setValue("");

    assertThatThrownBy(p::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("separate permission without a value")
        .hasMessageContaining("intended value pattern")
        .hasMessageNotContaining(".*");
  }

  /**
   * Demonstrates that splitting entries preserves global access without widening named-schema access.
   */
  @Test
  void splitSchemaPermissionsPreserveGlobalActionAndNamedScope() {
    var global = new Permission();
    global.setResource("schema");
    global.setActions(List.of("MODIFY_GLOBAL_COMPATIBILITY"));
    global.validate();
    global.transform();

    var named = new Permission();
    named.setResource("schema");
    named.setActions(List.of("VIEW"));
    named.setValue("public-.*");
    named.validate();
    named.transform();
    var permissions = List.of(global, named);

    assertThat(AccessContext.builder().schemaGlobalCompatChange().build().isAccessible(permissions)).isTrue();
    assertThat(AccessContext.builder().schemaActions("public-schema", SchemaAction.VIEW)
        .build().isAccessible(permissions)).isTrue();
    assertThat(AccessContext.builder().schemaActions("private-schema", SchemaAction.VIEW)
        .build().isAccessible(permissions)).isFalse();
  }

  /**
   * Applies named-resource validation to the default role as well as explicitly assigned roles.
   */
  @Test
  void validateRejectsMissingValueInDefaultRole() {
    var p = new Permission();
    p.setResource("topic");
    p.setActions(List.of("all"));
    var role = new DefaultRole();
    role.setPermissions(List.of(p));

    assertThatIllegalArgumentException().isThrownBy(role::validate)
        .withMessageContaining("TOPIC");
  }

  /**
   * Normalizes action names and compiles the configured resource pattern.
   */
  @Test
  void transformSetsParseableFields() {
    var p = new Permission();
    p.setResource("toPic");
    p.setActions(List.of("vIEW", "EdiT"));
    p.setValue("patt|ern");

    p.transform();

    assertThat(p.getParsedActions())
        .containsExactlyInAnyOrder(TopicAction.VIEW, TopicAction.EDIT);

    assertThat(p.getCompiledValuePattern())
        .isNotNull()
        .matches(pattern -> pattern.pattern().equals("patt|ern"));
  }

  /**
   * Expands the case-insensitive ALL action independently of resource matching.
   */
  @Test
  void transformSetsFullActionsListIfAllActionPassed() {
    var p = new Permission();
    p.setResource("toPic");
    p.setActions(List.of("All"));

    p.transform();

    assertThat(p.getParsedActions())
        .containsExactlyInAnyOrder(TopicAction.values());
  }

  /**
   * Includes the view action required by topic editing.
   */
  @Test
  void transformUnnestsDependantActions() {
    var p = new Permission();
    p.setResource("toPic");
    p.setActions(List.of("EDIT"));

    p.transform();

    assertThat(p.getParsedActions())
        .containsExactlyInAnyOrder(TopicAction.VIEW, TopicAction.EDIT);
  }

}
