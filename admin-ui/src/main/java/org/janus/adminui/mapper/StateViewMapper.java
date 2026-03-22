package org.janus.adminui.mapper;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.janus.adminui.model.SourceStateView;
import org.janus.adminui.model.StateView;
import org.janus.api.statestore.AdminDegradationState;
import org.janus.api.statestore.DegradationStateSourceEntry;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class StateViewMapper {

  public StateView fromGrpc(AdminDegradationState state, Instant refreshedAt) {
    List<SourceStateView> sourceStates =
        state.getSourceStatesList().stream().map(this::fromGrpc).toList();

    return new StateView(
        state.getDegradationId(),
        state.getEffectiveValue(),
        state.getEffectiveSource().name(),
        sourceStates,
        refreshedAt);
  }

  private SourceStateView fromGrpc(DegradationStateSourceEntry entry) {
    return new SourceStateView(
        entry.getSource().name(),
        entry.getValue(),
        Duration.ofMillis(Durations.toMillis(entry.getRemainingTtl())));
  }
}
