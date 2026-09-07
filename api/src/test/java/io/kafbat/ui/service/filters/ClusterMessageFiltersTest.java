package io.kafbat.ui.service.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.TopicMessageDTO;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClusterMessageFiltersTest {

  @Test
  void emptyWhenConfigIsNull() {
    assertThat(ClusterMessageFilters.create(null).getFilters()).isEmpty();
  }

  @Test
  void compilesCelAndAssignsStableId() {
    var config = filter("Non-heartbeat", "has(record.value.after)", true);

    ClusterMessageFilters filters = ClusterMessageFilters.create(List.of(config));

    assertThat(filters.getFilters()).hasSize(1);
    var predefined = filters.getFilters().getFirst();
    assertThat(predefined.getId()).isEqualTo("cfg-non-heartbeat");
    assertThat(predefined.getDisplayName()).isEqualTo("Non-heartbeat");
    assertThat(predefined.isEnabledByDefault()).isTrue();
    assertTrue(predefined.getPredicate().test(msg().value("{\"after\":{}}")));
    assertFalse(predefined.getPredicate().test(msg().value("{\"op\":\"r\"}")));
  }

  @Test
  void findByIdReturnsCompiledFilter() {
    var filters = ClusterMessageFilters.create(List.of(
        filter("Since yesterday", "has(record.timestampMs) && record.timestampMs > nowMs - 86400000", false)
    ));

    assertThat(filters.findById("cfg-since-yesterday")).isPresent();
    assertThat(filters.findById("unknown")).isEmpty();
  }

  @Test
  void rejectsDuplicateDisplayNames() {
    assertThatThrownBy(() -> ClusterMessageFilters.create(List.of(
        filter("Same", "true", false),
        filter("Same", "false", false)
    ))).isInstanceOf(ValidationException.class)
        .hasMessageContaining("duplicate message filter displayName");
  }

  @Test
  void rejectsDisplayNamesThatCollideAfterSlug() {
    assertThatThrownBy(() -> ClusterMessageFilters.create(List.of(
        filter("Non heartbeat", "true", false),
        filter("Non-heartbeat", "true", false)
    ))).isInstanceOf(ValidationException.class)
        .hasMessageContaining("duplicate id");
  }

  @Test
  void rejectsInvalidCel() {
    assertThatThrownBy(() -> ClusterMessageFilters.create(List.of(
        filter("Broken", "this is not cel", false)
    ))).isInstanceOf(ValidationException.class)
        .hasMessageContaining("invalid CEL");
  }

  @Test
  void rejectsBlankDisplayNameSlug() {
    assertThatThrownBy(() -> ClusterMessageFilters.toId("!!!"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("does not produce a valid id");
  }

  private static ClustersProperties.MessageFilterConfig filter(String name, String code, boolean enabledByDefault) {
    var config = new ClustersProperties.MessageFilterConfig();
    config.setDisplayName(name);
    config.setFilterCode(code);
    config.setEnabledByDefault(enabledByDefault);
    return config;
  }

  private static TopicMessageDTO msg() {
    return new TopicMessageDTO()
        .partition(0)
        .offset(0L)
        .timestamp(OffsetDateTime.now());
  }
}
