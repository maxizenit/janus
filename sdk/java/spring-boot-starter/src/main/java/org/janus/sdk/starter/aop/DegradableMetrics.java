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
  private static final String TAG_CALLER_DEGRADATION_ID = "caller.degradation.id";
  private static final String TAG_OUTCOME = "outcome";
  private static final String TAG_TRIGGER = "trigger";
  private static final String OUTCOME_NORMAL = "normal";
  private static final String OUTCOME_FALLBACK = "fallback";
  private static final String OUTCOME_ERROR = "error";
  private static final String TRIGGER_PROACTIVE = "proactive";
  private static final String TRIGGER_REACTIVE = "reactive";
  private static final String TRIGGER_NONE = "none";

  private final MeterRegistry meterRegistry;

  public void recordProactiveFallback(String degradationId, String callerDegradationId) {
    record(degradationId, callerDegradationId, OUTCOME_FALLBACK, TRIGGER_PROACTIVE);
  }

  public void recordReactiveFallback(String degradationId, String callerDegradationId) {
    record(degradationId, callerDegradationId, OUTCOME_FALLBACK, TRIGGER_REACTIVE);
  }

  public void recordNormal(String degradationId, String callerDegradationId) {
    record(degradationId, callerDegradationId, OUTCOME_NORMAL, TRIGGER_NONE);
  }

  public void recordError(String degradationId, String callerDegradationId) {
    record(degradationId, callerDegradationId, OUTCOME_ERROR, TRIGGER_NONE);
  }

  private void record(
      String degradationId, String callerDegradationId, String outcome, String trigger) {
    Counter.builder(METRIC_NAME)
        .tag(TAG_DEGRADATION_ID, degradationId)
        .tag(TAG_CALLER_DEGRADATION_ID, callerDegradationId)
        .tag(TAG_OUTCOME, outcome)
        .tag(TAG_TRIGGER, trigger)
        .register(meterRegistry)
        .increment();
  }
}
