package org.janus.sdk.starter.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.starter.client.SidecarSdkClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradationRefreshServiceTest {

  @Mock private SidecarSdkClient sidecarSdkClient;
  @Mock private DegradationStateRegistry stateRegistry;

  @Test
  void syncAndRefresh_registersIdsBeforeLoadingStates() {
    var service = new DegradationRefreshService(sidecarSdkClient, stateRegistry);

    org.mockito.Mockito.when(sidecarSdkClient.getDegradations()).thenReturn(List.of());

    service.syncAndRefresh(Set.of("deg-1"));

    var inOrder = org.mockito.Mockito.inOrder(sidecarSdkClient, stateRegistry);
    inOrder.verify(sidecarSdkClient).syncActualDegradations(Set.of("deg-1"));
    inOrder.verify(sidecarSdkClient).getDegradations();
    inOrder.verify(stateRegistry).replaceAll(java.util.Map.of());
  }

  @Test
  void syncAndRefresh_emptyIds_skipsSidecarCalls() {
    var service = new DegradationRefreshService(sidecarSdkClient, stateRegistry);

    service.syncAndRefresh(Set.of());

    verifyNoInteractions(sidecarSdkClient, stateRegistry);
  }
}
