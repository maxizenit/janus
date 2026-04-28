package org.janus.sdk.starter.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

  @Test
  void syncAndRefresh_syncFailure_marksRegistryStaleAndRethrows() {
    var service = new DegradationRefreshService(sidecarSdkClient, stateRegistry);
    var boom = new RuntimeException("boom");

    org.mockito.Mockito.doThrow(boom)
        .when(sidecarSdkClient)
        .syncActualDegradations(Set.of("deg-1"));

    assertThatThrownBy(() -> service.syncAndRefresh(Set.of("deg-1"))).isSameAs(boom);

    verify(stateRegistry).markAllStale();
    verify(sidecarSdkClient, org.mockito.Mockito.never()).getDegradations();
  }

  @Test
  void syncAndRefresh_loadFailure_marksRegistryStaleAndRethrows() {
    var service = new DegradationRefreshService(sidecarSdkClient, stateRegistry);
    var boom = new RuntimeException("boom");

    org.mockito.Mockito.when(sidecarSdkClient.getDegradations()).thenThrow(boom);

    assertThatThrownBy(() -> service.syncAndRefresh(Set.of("deg-1"))).isSameAs(boom);

    verify(sidecarSdkClient).syncActualDegradations(Set.of("deg-1"));
    verify(stateRegistry).markAllStale();
    verify(stateRegistry, org.mockito.Mockito.never()).replaceAll(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void refresh_loadFailure_marksRegistryStaleAndRethrows() {
    var service = new DegradationRefreshService(sidecarSdkClient, stateRegistry);
    var boom = new RuntimeException("boom");

    org.mockito.Mockito.when(sidecarSdkClient.getDegradations()).thenThrow(boom);

    assertThatThrownBy(service::refresh).isSameAs(boom);

    verify(stateRegistry).markAllStale();
    verify(stateRegistry, org.mockito.Mockito.never()).replaceAll(org.mockito.ArgumentMatchers.any());
  }
}
