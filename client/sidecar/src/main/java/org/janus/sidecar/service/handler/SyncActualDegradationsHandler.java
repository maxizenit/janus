package org.janus.sidecar.service.handler;

import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.model.handler.SyncActualDegradationsCommand;
import org.janus.sidecar.model.handler.SyncActualDegradationsResult;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.janus.sidecar.service.InitialLoadService;
import org.janus.sidecar.service.PolicyRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class SyncActualDegradationsHandler {

  private final ActualDegradationRegistry registry;
  private final PolicyRefreshService policyRefreshService;
  private final InitialLoadService initialLoadService;

  public SyncActualDegradationsResult handle(SyncActualDegradationsCommand command) {
    var result = registry.sync(command.degradationIds());
    log.info(
        "SyncActualDegradations: added={}, removed={}, retained={}, total={}",
        result.addedIds().size(),
        result.removedIds().size(),
        result.retainedIds().size(),
        registry.size());

    if (!result.addedIds().isEmpty()) {
      var initializedIds = policyRefreshService.refreshPolicies(result.addedIds());
      var notInitializedIds = new HashSet<>(result.addedIds());
      notInitializedIds.removeAll(initializedIds);

      log.info(
          "Initial policy refresh completed: added={}, initialized={}",
          result.addedIds().size(),
          initializedIds.size());

      if (!notInitializedIds.isEmpty()) {
        log.warn(
            "Added degradations were not initialized by policy store: missingCount={}, missingIds={}",
            notInitializedIds.size(),
            notInitializedIds.size() <= 20 ? notInitializedIds : "[omitted]");
      }

      initialLoadService.scheduleInitialStateLoad(initializedIds);
    }

    return result;
  }
}
