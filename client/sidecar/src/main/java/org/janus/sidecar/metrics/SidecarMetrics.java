package org.janus.sidecar.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class SidecarMetrics {

  private static final String METRIC_STALE = "janus.sidecar.degradations.stale";
  private static final String METRIC_ACTIVE = "janus.sidecar.degradations.active";

  private final MeterRegistry meterRegistry;
  private final ActualDegradationRegistry degradationRegistry;

  @PostConstruct
  void init() {
    Gauge.builder(METRIC_STALE, degradationRegistry, this::countStale).register(meterRegistry);

    Gauge.builder(METRIC_ACTIVE, degradationRegistry, this::countActive).register(meterRegistry);
  }

  private double countStale(ActualDegradationRegistry registry) {
    return registry.findAllActive().stream()
        .filter(d -> d.getState().map(StateSnapshot::stale).orElse(false))
        .count();
  }

  private double countActive(ActualDegradationRegistry registry) {
    return registry.findAllActive().size();
  }
}
