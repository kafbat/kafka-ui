package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class AclBindingFilterTest extends AbstractNgramFilterTest<AclBinding> {
  @Override
  protected NgramFilter<AclBinding> buildFilter(List<AclBinding> items, boolean enabled,
                                                ClustersProperties.NgramProperties ngramProperties) {
    return new AclBindingNgramFilter(items, enabled, ngramProperties);
  }

  @Override
  protected List<AclBinding> items() {
    return IntStream.range(0, 100).mapToObj(i ->
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "resource-" + i, PatternType.LITERAL),
            new AccessControlEntry("principal-" + i, "*", AclOperation.ALL, AclPermissionType.ALLOW)
        )
    ).toList();
  }

  @Override
  protected Comparator<AclBinding> comparator() {
    return Comparator.comparing(a -> a.entry().principal());
  }

  @Override
  protected Map.Entry<String, AclBinding> example(List<AclBinding> items) {
    AclBinding binding = items.getFirst();
    return Map.entry(binding.entry().principal(), binding);
  }

  @Override
  protected List<AclBinding> sortedItems() {
    return List.of(
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part-2", PatternType.LITERAL),
            new AccessControlEntry("s2Principal", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        ),
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part", PatternType.LITERAL),
            new AccessControlEntry("principal-first-part", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        ),
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part-2", PatternType.LITERAL),
            new AccessControlEntry("s-principal", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        )
    );
  }

  @Override
  protected String sortedExample(List<AclBinding> items) {
    return "princ";
  }

  @Override
  protected List<AclBinding> sortedResult(List<AclBinding> items) {
    return List.of(
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part", PatternType.LITERAL),
            new AccessControlEntry("principal-first-part", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        ),
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part-2", PatternType.LITERAL),
            new AccessControlEntry("s-principal", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        ),
        new AclBinding(
            new ResourcePattern(ResourceType.TOPIC, "res-name-part-2", PatternType.LITERAL),
            new AccessControlEntry("s2Principal", "*", AclOperation.ALL, AclPermissionType.ALLOW)
        )
    );
  }
}
