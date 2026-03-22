package org.janus.sdk.starter.registry;

import java.lang.reflect.Method;
import java.util.Optional;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MethodDescriptorResolver {

  Optional<DegradableMethodDescriptor> resolve(Method method, Class<?> targetClass);
}
