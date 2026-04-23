package org.janus.sdk.starter.lifecycle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.starter.scanner.DegradableMethodScanner;
import org.janus.sdk.starter.service.DegradationRefreshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JanusSdkStartupRunnerTest {

  @Mock private DegradableMethodScanner scanner;
  @Mock private DegradableMethodRegistry registry;
  @Mock private DegradationRefreshService refreshService;

  @Test
  void run_sidecarUnavailable_doesNotFailApplicationStartup() {
    var runner = new JanusSdkStartupRunner(scanner, registry, refreshService);

    when(registry.getAllDegradationIds()).thenReturn(Set.of("deg-1"));
    when(registry.getAll()).thenReturn(Set.of());
    org.mockito.Mockito.doThrow(new RuntimeException("sidecar down"))
        .when(refreshService)
        .syncAndRefresh(Set.of("deg-1"));

    assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();

    verify(scanner).scanAndRegister();
    verify(refreshService).syncAndRefresh(Set.of("deg-1"));
  }
}
