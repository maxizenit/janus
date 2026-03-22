package org.janus.adminui.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.client.policystore.PolicyStoreAdminClient;
import org.janus.adminui.client.statestore.StateStoreAdminClient;
import org.janus.adminui.model.OverrideStateCommand;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.model.StateView;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class StateAdminService {

  private final PolicyStoreAdminClient policyStoreClient;
  private final StateStoreAdminClient stateStoreClient;

  public List<StateView> getStatesForAllPolicies() {
    List<String> degradationIds =
        policyStoreClient.getAllPolicies().stream()
            .map(PolicyView::degradationId)
            .distinct()
            .toList();

    log.debug("Loading states for all policies: degradationCount={}", degradationIds.size());

    return stateStoreClient.getAdminStates(degradationIds);
  }

  public void applyOverride(OverrideStateCommand command) {
    stateStoreClient.applyOverride(command);
  }

  public void clearOverride(String degradationId) {
    stateStoreClient.clearOverride(degradationId);
  }
}
