package org.janus.sdk.starter.lifecycle;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.starter.client.SidecarSdkClient;
import org.janus.sdk.starter.scanner.DegradableMethodScanner;
import org.janus.sdk.starter.service.DegradationRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class JanusSdkStartupRunner implements ApplicationRunner {

  private final DegradableMethodScanner scanner;
  private final DegradableMethodRegistry registry;
  private final SidecarSdkClient sidecarSdkClient;
  private final DegradationRefreshService refreshService;

  @Override
  public void run(ApplicationArguments args) {
    scanner.scanAndRegister();

    Set<String> degradationIds = registry.getAllDegradationIds();
    log.info(
        "Degradable methods scanned: methods={}, degradationIds={}",
        registry.getAll().size(),
        degradationIds.size());

    if (degradationIds.isEmpty()) {
      log.info("Skipping SDK registration in sidecar: no degradation ids found");
      return;
    }

    sidecarSdkClient.syncActualDegradations(degradationIds);
    refreshService.refresh();

    log.info("SDK startup synchronization completed: degradationIds={}", degradationIds.size());
  }
}
