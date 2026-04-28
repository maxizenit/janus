package org.janus.sidecar.model;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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

  public void clearPolicy() {
    policyRef.set(null);
  }

  public void setState(StateSnapshot snapshot) {
    stateRef.set(snapshot);
  }

  public void clearState() {
    stateRef.set(null);
  }

  public boolean markStateStale() {
    while (true) {
      var current = stateRef.get();
      if (current == null) {
        return false;
      }

      if (stateRef.compareAndSet(current, current.staleCopy())) {
        return true;
      }
    }
  }

  public boolean tryStartStateRefresh() {
    return isActive() && stateRefreshInFlight.compareAndSet(false, true);
  }

  public void finishStateRefresh() {
    stateRefreshInFlight.set(false);
  }
}
