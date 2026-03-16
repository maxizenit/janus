package org.janus.sidecar.registry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.handler.SyncActualDegradationsResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ActualDegradationRegistry {
  SyncActualDegradationsResult sync(Set<String> desiredIds);

  Optional<RegisteredDegradation> find(String degradationId);

  List<RegisteredDegradation> findAllActive();

  int size();
}
