package io.kafbat.ui.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.List;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

public class AccessControlServiceMock {
  private final List<Role> roles;

  public AccessControlServiceMock(List<Role> roles) {
    this.roles = roles;
  }

  public AccessControlServiceMock() {
    this(List.of());
  }


  public AccessControlService getMock() {
    AccessControlService mock = Mockito.mock(AccessControlService.class);

    when(mock.validateAccess(any())).thenReturn(Mono.empty());
    when(mock.isSchemaAccessible(anyString(), anyString())).thenReturn(Mono.just(true));

    when(mock.filterViewableTopics(any(), any())).then(invocation -> Mono.just(invocation.getArgument(0)));
    when(mock.getRoles()).thenReturn(roles);

    return mock;
  }
}
