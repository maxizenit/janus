package org.janus.sidecar.client.statestore;

import java.util.Map;
import java.util.Set;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StateStoreClient {

  Map<String, StateSnapshot> getStates(Set<String> degradationIds);
}
