package org.janus.decider.model;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.janus.decider.model.snapshot.PolicySnapshot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
@NullMarked
public class RegisteredDegradation {

  @Getter private final String degradationId;

  private final AtomicBoolean active = new AtomicBoolean(true);
  private final AtomicBoolean evaluationInFlight = new AtomicBoolean(false);

  private final AtomicReference<@Nullable PolicySnapshot> policyRef = new AtomicReference<>();
  private final AtomicLong nextEvaluationAtMillis = new AtomicLong(0);

  public boolean isActive() {
    return active.get();
  }

  public void deactivate() {
    active.compareAndSet(true, false);
  }

  public Optional<PolicySnapshot> getPolicy() {
    return Optional.ofNullable(policyRef.get());
  }

  public void replacePolicy(PolicySnapshot policySnapshot) {
    policyRef.set(policySnapshot);
  }

  public boolean tryStartEvaluation() {
    return isActive() && evaluationInFlight.compareAndSet(false, true);
  }

  public void finishEvaluation() {
    evaluationInFlight.set(false);
  }

  public void setNextEvaluationAt(Instant instant) {
    nextEvaluationAtMillis.set(instant.toEpochMilli());
  }
}
