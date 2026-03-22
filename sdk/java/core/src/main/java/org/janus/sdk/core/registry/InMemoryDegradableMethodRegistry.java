package org.janus.sdk.core.registry;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.validation.InvalidDegradableDefinitionException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InMemoryDegradableMethodRegistry implements DegradableMethodRegistry {

  private final Map<Method, DegradableMethodDescriptor> descriptorsByMethod =
      new ConcurrentHashMap<>();

  @Override
  public void register(DegradableMethodDescriptor descriptor) {
    var previous = descriptorsByMethod.putIfAbsent(descriptor.method(), descriptor);
    if (previous != null) {
      throw new InvalidDegradableDefinitionException(
          "Degradable method already registered: %s"
              .formatted(descriptor.method().toGenericString()));
    }
  }

  @Override
  public Optional<DegradableMethodDescriptor> find(Method method) {
    return Optional.ofNullable(descriptorsByMethod.get(method));
  }

  @Override
  public Set<String> getAllDegradationIds() {
    return descriptorsByMethod.values().stream()
        .map(DegradableMethodDescriptor::degradationId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  @Override
  public Collection<DegradableMethodDescriptor> getAll() {
    return java.util.List.copyOf(descriptorsByMethod.values());
  }
}
