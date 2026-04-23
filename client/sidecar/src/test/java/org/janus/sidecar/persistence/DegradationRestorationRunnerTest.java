package org.janus.sidecar.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.janus.sidecar.model.handler.SyncActualDegradationsCommand;
import org.janus.sidecar.service.handler.SyncActualDegradationsHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradationRestorationRunnerTest {

  @Mock private DegradationIdStore store;
  @Mock private SyncActualDegradationsHandler syncHandler;

  @Test
  void run_syncFailure_doesNotFailStartup() {
    var runner = new DegradationRestorationRunner(store, syncHandler);
    var ids = Set.of("deg-1");

    when(store.loadAll()).thenReturn(ids);
    org.mockito.Mockito.doThrow(new RuntimeException("policy store down"))
        .when(syncHandler)
        .handle(new SyncActualDegradationsCommand(ids));

    assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
  }
}
