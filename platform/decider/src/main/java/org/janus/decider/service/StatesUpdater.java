package org.janus.decider.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatesUpdater {

    private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

    public void updateStates() {
    }
}
