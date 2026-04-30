package org.janus.policystore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.policystore.client.statestore.StateStoreClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PolicyDeletionOrchestrator {

  private final DegradationPolicyService policyService;
  private final StateStoreClient stateStoreClient;

  public void deletePolicyByDegradationId(String degradationId) {
    policyService.deletePolicyByDegradationId(degradationId);

    try {
      stateStoreClient.clearAllStates(degradationId);
    } catch (RuntimeException e) {
      log.warn(
          "State cleanup failed after policy delete, state will expire by TTL: degradationId={}",
          degradationId,
          e);
    }
  }
}
