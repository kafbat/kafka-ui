package io.kafbat.ui.config.auth.condition;

import io.kafbat.ui.service.rbac.AbstractProviderCondition;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CognitoCondition extends AbstractProviderCondition implements Condition {
  @Override
  public boolean matches(final ConditionContext context, final @NotNull AnnotatedTypeMetadata metadata) {
    return getRegisteredProvidersTypes(context.getEnvironment()).stream().anyMatch(a -> a.equalsIgnoreCase("cognito"));
  }
}
