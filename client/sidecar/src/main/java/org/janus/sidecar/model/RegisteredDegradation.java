package org.janus.sidecar.model;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
@NullMarked
public class RegisteredDegradation {

  @Getter private final String degradationId;

  private final AtomicBoolean active = new AtomicBoolean(true);
  private final AtomicBoolean stateRefreshInFlight = new AtomicBoolean(false);

  private final AtomicReference<@Nullable PolicySnapshot> policyRef = new AtomicReference<>();
  private final AtomicReference<@Nullable StateSnapshot> stateRef = new AtomicReference<>();

  private final AtomicLong nextStateRefreshAtMillis = new AtomicLong(0);

  public boolean isActive() {
    return active.get();
  }

  public void deactivate() {
    active.compareAndSet(true, false);
  }

  public Optional<PolicySnapshot> getPolicy() {
    return Optional.ofNullable(policyRef.get());
  }

  public Optional<StateSnapshot> getState() {
    return Optional.ofNullable(stateRef.get());
  }

  public void replacePolicy(PolicySnapshot snapshot) {
    policyRef.set(snapshot);
  }

  public void setState(StateSnapshot snapshot) {
    stateRef.set(snapshot);
  }

  public boolean tryStartStateRefresh() {
    return isActive() && stateRefreshInFlight.compareAndSet(false, true);
  }

  public void finishStateRefresh() {
    stateRefreshInFlight.set(false);
  }

  public void setNextStateRefreshAt(Instant instant) {
    nextStateRefreshAtMillis.set(instant.toEpochMilli());
  }
}
