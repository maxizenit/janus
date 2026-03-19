package org.janus.decider.registry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.janus.decider.model.RegisteredDegradation;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DegradationRegistry {

  void sync(Set<String> desiredIds);

  Optional<RegisteredDegradation> find(String degradationId);

  List<RegisteredDegradation> findAllActive();

  int size();
}
