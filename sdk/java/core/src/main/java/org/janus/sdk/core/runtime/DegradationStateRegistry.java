package org.janus.sdk.core.runtime;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DegradationStateRegistry {

  void replaceAll(Map<String, DegradationRuntimeState> states);

  void upsertAll(Map<String, DegradationRuntimeState> states);

  Optional<DegradationRuntimeState> find(String degradationId);

  Map<String, DegradationRuntimeState> getAll();
}
