package io.kafbat.ui.service.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kafbat.ui.exception.ValidationException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AclCsvTest {

  private static final List<AclBinding> TEST_BINDINGS = List.of(
      new AclBinding(
          new ResourcePattern(ResourceType.TOPIC, "*", PatternType.LITERAL),
          new AccessControlEntry("User:test1", "*", AclOperation.READ, AclPermissionType.ALLOW)),
      new AclBinding(
          new ResourcePattern(ResourceType.GROUP, "group1", PatternType.PREFIXED),
          new AccessControlEntry("User:test2", "localhost", AclOperation.DESCRIBE, AclPermissionType.DENY))
  );

  @ParameterizedTest
  @MethodSource
  void parsesValidInputCsv(String csvString) {
    Collection<AclBinding> parsed = AclCsv.parseCsv(csvString);
    assertThat(parsed).containsExactlyInAnyOrderElementsOf(TEST_BINDINGS);
  }

  private static Stream<Arguments> parsesValidInputCsv() {
    return Stream.of(
        Arguments.of(
            "Principal,ResourceType, PatternType, ResourceName,Operation,PermissionType,Host" + System.lineSeparator()
                + "User:test1,TOPIC,LITERAL,*,READ,ALLOW,*" + System.lineSeparator()
                + "User:test2,GROUP,PREFIXED,group1,DESCRIBE,DENY,localhost"),
        Arguments.of(
            //without header
            "User:test1,TOPIC,LITERAL,*,READ,ALLOW,*" + System.lineSeparator()
                + System.lineSeparator()
                + "User:test2,GROUP,PREFIXED,group1,DESCRIBE,DENY,localhost"
                + System.lineSeparator()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      // columns > 7
      "User:test1,TOPIC,LITERAL,*,READ,ALLOW,*,1,2,3,4",
      // columns < 7
      "User:test1,TOPIC,LITERAL,*",
      // enum values are illegal
      "User:test1,ILLEGAL,LITERAL,*,READ,ALLOW,*",
      "User:test1,TOPIC,LITERAL,*,READ,ILLEGAL,*"
  })
  void throwsExceptionForInvalidInputCsv(String csvString) {
    assertThatThrownBy(() -> AclCsv.parseCsv(csvString))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void transformAndParseUseSameFormat() {
    String csv = AclCsv.transformToCsvString(TEST_BINDINGS);
    Collection<AclBinding> parsedBindings = AclCsv.parseCsv(csv);
    assertThat(parsedBindings).containsExactlyInAnyOrderElementsOf(TEST_BINDINGS);
  }

}
