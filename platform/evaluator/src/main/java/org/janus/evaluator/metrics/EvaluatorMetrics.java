package org.janus.evaluator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class EvaluatorMetrics {

  private static final String METRIC_EVALUATIONS = "janus.evaluator.evaluations";
  private static final String METRIC_SIGNAL_FETCH_DURATION =
      "janus.evaluator.signal.fetch.duration";
  private static final String METRIC_LEADERSHIP = "janus.evaluator.leadership.acquisitions";

  private static final String TAG_DEGRADATION_ID = "degradation.id";
  private static final String TAG_OUTCOME = "outcome";
  private static final String TAG_RESULT = "result";

  private final MeterRegistry meterRegistry;

  public void recordEvaluation(String degradationId, String outcome) {
    Counter.builder(METRIC_EVALUATIONS)
        .tag(TAG_DEGRADATION_ID, degradationId)
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  public void recordSignalFetchDuration(String degradationId, Duration duration) {
    Timer.builder(METRIC_SIGNAL_FETCH_DURATION)
        .tag(TAG_DEGRADATION_ID, degradationId)
        .register(meterRegistry)
        .record(duration);
  }

  public void recordLeadershipAcquisition(String degradationId, String result) {
    Counter.builder(METRIC_LEADERSHIP)
        .tag(TAG_DEGRADATION_ID, degradationId)
        .tag(TAG_RESULT, result)
        .register(meterRegistry)
        .increment();
  }
}
