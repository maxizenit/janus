package org.janus.sdk.starter.registry;

import java.lang.reflect.Method;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class RegistryBackedMethodDescriptorResolver implements MethodDescriptorResolver {

  private final DegradableMethodRegistry registry;

  @Override
  public Optional<DegradableMethodDescriptor> resolve(Method method, Class<?> targetClass) {
    var specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    return registry.find(specificMethod).or(() -> registry.find(method));
  }
}
