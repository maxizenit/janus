package org.janus.sdk.starter.service;

import java.util.Map;
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

  public void refresh() {
    var states =
        sidecarSdkClient.getDegradations().stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    DegradationRuntimeState::degradationId, state -> state));

    stateRegistry.replaceAll(states);

    log.debug("Degradation states refreshed: count={}", states.size());
  }

  public Map<String, DegradationRuntimeState> getAll() {
    return stateRegistry.getAll();
  }
}
