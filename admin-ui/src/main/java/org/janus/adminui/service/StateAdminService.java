package org.janus.adminui.service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final Clock clock;

  public List<StateView> getStatesForAllPolicies() {
    List<PolicyView> policies = policyStoreClient.getAllPolicies();
    List<String> degradationIds =
        policies.stream().map(PolicyView::degradationId).distinct().toList();

    log.debug("Loading states for all policies: degradationCount={}", degradationIds.size());

    List<StateView> existingStates = stateStoreClient.getAdminStates(degradationIds);

    Map<String, StateView> statesById = new LinkedHashMap<>();
    for (StateView state : existingStates) {
      statesById.put(state.degradationId(), state);
    }

    var refreshedAt = clock.instant();

    return degradationIds.stream()
        .map(
            degradationId ->
                statesById.getOrDefault(
                    degradationId,
                    new StateView(degradationId, null, null, List.of(), refreshedAt)))
        .toList();
  }

  public void applyOverride(OverrideStateCommand command) {
    stateStoreClient.applyOverride(command);
  }

  public void clearOverride(String degradationId) {
    stateStoreClient.clearOverride(degradationId);
  }
}
