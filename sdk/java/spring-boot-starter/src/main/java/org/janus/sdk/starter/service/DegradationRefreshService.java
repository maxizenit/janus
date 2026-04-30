package org.janus.sdk.starter.service;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.starter.client.SidecarSdkClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationRefreshService {

  private final SidecarSdkClient sidecarSdkClient;
  private final DegradationStateRegistry stateRegistry;

  public void syncAndRefresh(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping degradation synchronization: no degradation ids");
      return;
    }

    try {
      sidecarSdkClient.syncActualDegradations(degradationIds);
    } catch (RuntimeException e) {
      stateRegistry.markAllStale();
      throw e;
    }
    refresh();
  }

  public void refresh() {
    Map<String, DegradationRuntimeState> states;
    try {
      states =
          sidecarSdkClient.getDegradations().stream()
              .collect(
                  java.util.stream.Collectors.toUnmodifiableMap(
                      DegradationRuntimeState::degradationId, state -> state));
    } catch (RuntimeException e) {
      stateRegistry.markAllStale();
      throw e;
    }

    stateRegistry.replaceAll(states);

    log.debug("Degradation states refreshed: count={}", states.size());
  }

  public Map<String, DegradationRuntimeState> getAll() {
    return stateRegistry.getAll();
  }
}
