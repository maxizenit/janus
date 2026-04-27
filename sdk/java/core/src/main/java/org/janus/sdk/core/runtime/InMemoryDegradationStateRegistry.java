package org.janus.sdk.core.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InMemoryDegradationStateRegistry implements DegradationStateRegistry {

  private final AtomicReference<Map<String, DegradationRuntimeState>> states =
      new AtomicReference<>(Map.of());

  @Override
  public void replaceAll(Map<String, DegradationRuntimeState> states) {
    this.states.set(Map.copyOf(states));
  }

  @Override
  public void upsertAll(Map<String, DegradationRuntimeState> states) {
    if (states.isEmpty()) {
      return;
    }
    this.states.updateAndGet(
        current -> {
          var merged = new HashMap<>(current);
          merged.putAll(states);
          return Map.copyOf(merged);
        });
  }

  @Override
  public Optional<DegradationRuntimeState> find(String degradationId) {
    return Optional.ofNullable(states.get().get(degradationId));
  }

  @Override
  public Map<String, DegradationRuntimeState> getAll() {
    return states.get();
  }
}
