package org.janus.evaluator.client.statestore;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StateStoreClient {

  void updateState(String degradationId, double value, Duration ttl);
}
