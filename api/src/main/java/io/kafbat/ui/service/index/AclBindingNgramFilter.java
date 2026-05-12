package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Collection;
import java.util.List;
import org.apache.kafka.common.acl.AclBinding;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class AclBindingNgramFilter extends NgramFilter<AclBinding> {
  private final List<Tuple2<List<String>, AclBinding>> bindings;

  public AclBindingNgramFilter(Collection<AclBinding> bindings) {
    this(bindings, true, new ClustersProperties.NgramProperties(1, 4, true));
  }

  public AclBindingNgramFilter(
      Collection<AclBinding> bindings,
      boolean enabled,
      ClustersProperties.NgramProperties properties) {
    super(properties, enabled);
    this.bindings = bindings.stream().map(g -> Tuples.of(List.of(g.entry().principal()), g)).toList();
  }

  @Override
  protected List<Tuple2<List<String>, AclBinding>> getItems() {
    return this.bindings;
  }
}
