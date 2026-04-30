package org.janus.policystore.client.statestore;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StateStoreClient {

  void clearAllStates(String degradationId);
}
