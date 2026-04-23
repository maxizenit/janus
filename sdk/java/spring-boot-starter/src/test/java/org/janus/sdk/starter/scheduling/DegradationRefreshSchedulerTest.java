package org.janus.sdk.starter.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.starter.service.DegradationRefreshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradationRefreshSchedulerTest {

  @Mock private DegradationRefreshService refreshService;
  @Mock private DegradableMethodRegistry registry;

  @Test
  void refresh_retriesSidecarSynchronizationForRegisteredIds() {
    var scheduler = new DegradationRefreshScheduler(refreshService, registry);

    when(registry.getAllDegradationIds()).thenReturn(Set.of("deg-1", "deg-2"));

    scheduler.refresh();

    verify(refreshService).syncAndRefresh(Set.of("deg-1", "deg-2"));
  }
}
