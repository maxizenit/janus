package org.janus.sidecar.registry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.handler.SyncActualDegradationsResult;
import org.janus.sidecar.persistence.DegradationIdStore;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PersistentActualDegradationRegistry implements ActualDegradationRegistry {

  private final InMemoryActualDegradationRegistry delegate;
  private final DegradationIdStore store;

  @Override
  public SyncActualDegradationsResult sync(Set<String> desiredIds) {
    var result = delegate.sync(desiredIds);

    try {
      store.replaceAll(desiredIds);
    } catch (Exception e) {
      log.error("Failed to persist degradation IDs to SQLite, in-memory state is authoritative", e);
    }

    return result;
  }

  @Override
  public Optional<RegisteredDegradation> find(String degradationId) {
    return delegate.find(degradationId);
  }

  @Override
  public List<RegisteredDegradation> findAllActive() {
    return delegate.findAllActive();
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
