package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
public class Role {

  String name;
  List<String> clusters;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  transient List<Pattern> clusterPatterns;
  List<Subject> subjects;
  List<Permission> permissions = new ArrayList<>();

  /**
   * Validates the role configuration and compiles cluster matching patterns.
   */
  public void validate() {
    checkArgument(clusters != null && !clusters.isEmpty(), "Role clusters cannot be empty");
    this.clusterPatterns = clusters.stream()
        .map(Role::compileClusterPattern)
        .toList();
    checkArgument(subjects != null && !subjects.isEmpty(), "Role subjects cannot be empty");
    permissions.forEach(Permission::validate);
    permissions.forEach(Permission::transform);
    subjects.forEach(Subject::validate);
  }

  /**
   * Checks whether the given cluster name matches any configured cluster name,
   * regex pattern, or {@code "*"} wildcard (case-insensitively).
   */
  public boolean clusterMatches(@Nullable String clusterName) {
    if (StringUtils.isEmpty(clusterName) || clusters == null) {
      return false;
    }
    List<Pattern> patterns = clusterPatterns != null
        ? clusterPatterns
        : clusters.stream().map(Role::compileClusterPattern).toList();
    return patterns.stream().anyMatch(p -> p.matcher(clusterName).matches());
  }

  private static Pattern compileClusterPattern(String cluster) {
    String regex = "*".equals(cluster) ? ".*" : cluster;
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  }
}
