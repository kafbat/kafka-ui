package io.kafbat.ui.service.filters;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.emitter.MessageFilters;
import io.kafbat.ui.exception.CelException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.TopicMessageDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class ClusterMessageFilters {

  public static final ClusterMessageFilters EMPTY = new ClusterMessageFilters(List.of());

  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final Pattern LEADING_OR_TRAILING_DASH = Pattern.compile("^-+|-+$");
  private static final String ID_PREFIX = "cfg-";

  List<PredefinedFilter> filters;

  public static ClusterMessageFilters empty() {
    return EMPTY;
  }

  public static ClusterMessageFilters create(
      @Nullable List<ClustersProperties.MessageFilterConfig> config) {
    List<ClustersProperties.MessageFilterConfig> source = Optional.ofNullable(config).orElse(List.of());
    Set<String> displayNames = new HashSet<>();
    Set<String> ids = new HashSet<>();
    List<PredefinedFilter> compiled = new ArrayList<>();

    for (ClustersProperties.MessageFilterConfig property : source) {
      if (StringUtils.isBlank(property.getDisplayName())) {
        throw new ValidationException("message filter displayName must not be blank");
      }
      if (StringUtils.isBlank(property.getFilterCode())) {
        throw new ValidationException(
            "message filter filterCode must not be blank for '%s'".formatted(property.getDisplayName())
        );
      }
      if (!displayNames.add(property.getDisplayName())) {
        throw new ValidationException(
            "duplicate message filter displayName '%s'".formatted(property.getDisplayName())
        );
      }

      String id = toId(property.getDisplayName());
      if (!ids.add(id)) {
        throw new ValidationException(
            "message filter displayName '%s' produces duplicate id '%s'".formatted(property.getDisplayName(), id)
        );
      }

      Predicate<TopicMessageDTO> predicate;
      try {
        predicate = MessageFilters.celScriptFilter(property.getFilterCode());
      } catch (CelException e) {
        throw new ValidationException(
            "invalid CEL in message filter '%s': %s".formatted(property.getDisplayName(), e.getMessage())
        );
      }

      compiled.add(new PredefinedFilter(
          id,
          property.getDisplayName(),
          property.getFilterCode(),
          property.isEnabledByDefault(),
          predicate
      ));
    }

    return new ClusterMessageFilters(List.copyOf(compiled));
  }

  public Optional<PredefinedFilter> findById(String id) {
    return filters.stream().filter(filter -> filter.getId().equals(id)).findFirst();
  }

  static String toId(String displayName) {
    String slug = LEADING_OR_TRAILING_DASH.matcher(
        NON_ALPHANUMERIC.matcher(displayName.toLowerCase(Locale.ROOT)).replaceAll("-")
    ).replaceAll("");
    if (slug.isBlank()) {
      throw new ValidationException(
          "message filter displayName '%s' does not produce a valid id".formatted(displayName)
      );
    }
    return ID_PREFIX + slug;
  }

  @Value
  public static class PredefinedFilter {
    String id;
    String displayName;
    String filterCode;
    boolean enabledByDefault;
    Predicate<TopicMessageDTO> predicate;
  }
}
