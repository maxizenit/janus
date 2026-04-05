package org.janus.evaluator.scheduling;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EvaluationScheduler {

  void scheduleNow(String degradationId);

  void scheduleAt(String degradationId, Instant instant);
}
