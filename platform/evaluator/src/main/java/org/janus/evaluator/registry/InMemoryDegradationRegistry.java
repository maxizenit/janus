package org.janus.evaluator.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.janus.evaluator.model.RegisteredDegradation;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@NullMarked
public class InMemoryDegradationRegistry implements DegradationRegistry {

  private final ConcurrentHashMap<String, RegisteredDegradation> registry =
      new ConcurrentHashMap<>();

  @Override
  public void sync(Set<String> desiredIds) {
    var currentIds = Set.copyOf(registry.keySet());

    var toAdd = new HashSet<>(desiredIds);
    toAdd.removeAll(currentIds);

    var toRemove = new HashSet<>(currentIds);
    toRemove.removeAll(desiredIds);

    for (var id : toAdd) {
      registry.putIfAbsent(id, new RegisteredDegradation(id));
      log.debug("Registered degradation {}", id);
    }

    for (var id : toRemove) {
      var removed = registry.remove(id);
      if (removed != null) {
        removed.deactivate();
      }
      log.debug("Unregistered degradation {}", id);
    }
  }

  @Override
  public Optional<RegisteredDegradation> find(String degradationId) {
    return Optional.ofNullable(registry.get(degradationId)).filter(RegisteredDegradation::isActive);
  }

  @Override
  public List<RegisteredDegradation> findAllActive() {
    return registry.values().stream().filter(RegisteredDegradation::isActive).toList();
  }

  @Override
  public int size() {
    return (int) registry.values().stream().filter(RegisteredDegradation::isActive).count();
  }
}
