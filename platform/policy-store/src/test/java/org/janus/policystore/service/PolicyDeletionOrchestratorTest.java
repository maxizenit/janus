package org.janus.policystore.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.janus.policystore.client.statestore.StateStoreClient;
import org.janus.policystore.exception.PolicyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyDeletionOrchestratorTest {

  @Mock private DegradationPolicyService policyService;
  @Mock private StateStoreClient stateStoreClient;

  @Test
  void deletePolicyByDegradationId_clearsStateAfterDbDelete() {
    var orchestrator = new PolicyDeletionOrchestrator(policyService, stateStoreClient);

    orchestrator.deletePolicyByDegradationId("deg-1");

    var inOrder = inOrder(policyService, stateStoreClient);
    inOrder.verify(policyService).deletePolicyByDegradationId("deg-1");
    inOrder.verify(stateStoreClient).clearAllStates("deg-1");
  }

  @Test
  void deletePolicyByDegradationId_swallowsStateStoreFailure() {
    var orchestrator = new PolicyDeletionOrchestrator(policyService, stateStoreClient);
    doThrow(new RuntimeException("state store down"))
        .when(stateStoreClient)
        .clearAllStates("deg-1");

    orchestrator.deletePolicyByDegradationId("deg-1");

    verify(policyService).deletePolicyByDegradationId("deg-1");
    verify(stateStoreClient).clearAllStates("deg-1");
  }

  @Test
  void deletePolicyByDegradationId_dbFailure_skipsStateCleanup() {
    var orchestrator = new PolicyDeletionOrchestrator(policyService, stateStoreClient);
    doThrow(new PolicyNotFoundException("deg-1"))
        .when(policyService)
        .deletePolicyByDegradationId("deg-1");

    assertThatThrownBy(() -> orchestrator.deletePolicyByDegradationId("deg-1"))
        .isInstanceOf(PolicyNotFoundException.class);

    verify(stateStoreClient, never()).clearAllStates("deg-1");
  }
}
