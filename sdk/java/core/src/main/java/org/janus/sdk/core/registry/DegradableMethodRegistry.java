package org.janus.sdk.core.registry;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DegradableMethodRegistry {

  void register(DegradableMethodDescriptor descriptor);

  Optional<DegradableMethodDescriptor> find(Method method);

  Set<String> getAllDegradationIds();

  Collection<DegradableMethodDescriptor> getAll();
}
