package io.kafbat.ui.service;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaConfigSanitizer {

  private static final String SANITIZED_VALUE = "******";

  private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

  private static final Pattern CONFIG_PROVIDER_REFERENCE =
      Pattern.compile("\\$\\{[^}:]+:([^}:]+:)?[^}]+}");

  private static final List<String> DEFAULT_PATTERNS_TO_SANITIZE = ImmutableList.<String>builder()
      .addAll(kafkaConfigKeysToSanitize())
      .add(
          "basic.auth.user.info",  /* For Schema Registry credentials */
          "password", "secret", "token", "key", ".*credentials.*", "passphrase",   /* General credential patterns */
          "aws.access.*", "aws.secret.*", "aws.session.*",   /* AWS-related credential patterns */
          "connection.uri" /* mongo credential patterns */
      )
      .build();

  private final List<Pattern> sanitizeKeysPatterns;

  KafkaConfigSanitizer(
      @Value("${kafka.config.sanitizer.enabled:true}") boolean enabled,
      @Value("${kafka.config.sanitizer.patterns:}") List<String> patternsToSanitize
  ) {
    this.sanitizeKeysPatterns = enabled
        ? compile(patternsToSanitize.isEmpty() ? DEFAULT_PATTERNS_TO_SANITIZE : patternsToSanitize)
        : List.of();
  }

  private static List<Pattern> compile(Collection<String> patternStrings) {
    return patternStrings.stream()
        .map(p -> isRegex(p)
            ? Pattern.compile(p, CASE_INSENSITIVE)
            : Pattern.compile(".*" + p + "$", CASE_INSENSITIVE))
        .toList();
  }

  private static boolean isRegex(String str) {
    return Arrays.stream(REGEX_PARTS).anyMatch(str::contains);
  }

  private static Set<String> kafkaConfigKeysToSanitize() {
    final ConfigDef configDef = new ConfigDef();
    SslConfigs.addClientSslSupport(configDef);
    SaslConfigs.addClientSaslSupport(configDef);
    return configDef.configKeys().entrySet().stream()
        .filter(entry -> entry.getValue().type().equals(ConfigDef.Type.PASSWORD))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  @Nullable
  public Object sanitize(String key, @Nullable Object value) {
    for (Pattern pattern : sanitizeKeysPatterns) {
      if (pattern.matcher(key).matches()) {
        // The likelihood that a credential matching key contains exactly CONFIG_PROVIDER_REFERENCE pattern is
        // astronomically low as it is matching against an exact reference to ${ ... : ... }
        if (isConfigProviderReference(value)) {
          return value;
        }
        return SANITIZED_VALUE;
      }
    }
    return value;
  }

  /**
   *  Checks if config value is an externalized secret / config provider indirection: ${provider:[path:]key}.
   *  Such a value is only a reference resolved at runtime, not an actual
   *  secret, so it must not be masked (masking would clobber the reference on re-submit).
   * @param value config value
   * @return true if provider reference, false otherwise
   */
  private static boolean isConfigProviderReference(@Nullable Object value) {
    return value instanceof CharSequence
        && CONFIG_PROVIDER_REFERENCE.matcher((CharSequence) value).matches();
  }

  public Map<String, Object> sanitizeConnectorConfig(@Nullable Map<String, Object> original) {
    var result = new HashMap<String, Object>(); //null-values supporting map!
    if (original != null) {
      original.forEach((k, v) -> result.put(k, sanitize(k, v)));
    }
    return result;
  }

}
