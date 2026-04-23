package org.janus.sidecar.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.model.handler.SyncActualDegradationsCommand;
import org.janus.sidecar.service.handler.SyncActualDegradationsHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationRestorationRunner implements ApplicationRunner {

  private final DegradationIdStore store;
  private final SyncActualDegradationsHandler syncHandler;

  @Override
  public void run(ApplicationArguments args) {
    var ids = store.loadAll();

    if (ids.isEmpty()) {
      log.info("No persisted degradation IDs found, skipping restoration");
      return;
    }

    log.info("Restoring {} persisted degradation IDs", ids.size());
    try {
      syncHandler.handle(new SyncActualDegradationsCommand(ids));
      log.info("Degradation restoration completed");
    } catch (Exception e) {
      log.warn(
          "Degradation restoration policy initialization failed, scheduled refresh will retry: count={}",
          ids.size(),
          e);
    }
  }
}
