package org.janus.sdk.core.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InMemoryDegradationStateRegistry implements DegradationStateRegistry {

  private final Map<String, DegradationRuntimeState> states = new ConcurrentHashMap<>();

  @Override
  public void replaceAll(Map<String, DegradationRuntimeState> states) {
    this.states.clear();
    this.states.putAll(states);
  }

  @Override
  public void upsertAll(Map<String, DegradationRuntimeState> states) {
    this.states.putAll(states);
  }

  @Override
  public Optional<DegradationRuntimeState> find(String degradationId) {
    return Optional.ofNullable(states.get(degradationId));
  }

  @Override
  public Map<String, DegradationRuntimeState> getAll() {
    return Map.copyOf(states);
  }
}
