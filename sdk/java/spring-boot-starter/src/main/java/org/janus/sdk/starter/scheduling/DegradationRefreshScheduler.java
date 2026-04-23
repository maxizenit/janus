package org.janus.sdk.starter.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.starter.service.DegradationRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationRefreshScheduler {

  private final DegradationRefreshService refreshService;
  private final DegradableMethodRegistry registry;

  @Scheduled(fixedDelayString = "${janus.sdk.refresh-interval}")
  public void refresh() {
    try {
      refreshService.syncAndRefresh(registry.getAllDegradationIds());
    } catch (Exception e) {
      log.warn("Failed to refresh degradations from sidecar", e);
    }
  }
}
