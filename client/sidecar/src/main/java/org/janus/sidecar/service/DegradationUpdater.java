package org.janus.sidecar.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class DegradationUpdater {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

  public void updateStates() {}
}
