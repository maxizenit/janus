package org.janus.sdk.starter.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class DegradableMetrics {

  private static final String METRIC_NAME = "janus.degradable.invocations";
  private static final String TAG_DEGRADATION_ID = "degradation.id";
  private static final String TAG_OUTCOME = "outcome";
  private static final String OUTCOME_NORMAL = "normal";
  private static final String OUTCOME_FALLBACK = "fallback";

  private final MeterRegistry meterRegistry;

  public void recordInvocation(String degradationId, boolean fallback) {
    Counter.builder(METRIC_NAME)
        .tag(TAG_DEGRADATION_ID, degradationId)
        .tag(TAG_OUTCOME, fallback ? OUTCOME_FALLBACK : OUTCOME_NORMAL)
        .register(meterRegistry)
        .increment();
  }
}
