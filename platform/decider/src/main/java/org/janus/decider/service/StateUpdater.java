package org.janus.decider.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class StateUpdater {

    private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

    public void updateStates() {
    }
}
