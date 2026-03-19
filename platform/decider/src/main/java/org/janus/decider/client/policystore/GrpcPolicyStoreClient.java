package org.janus.decider.client.policystore;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.DeciderDegradationPolicy;
import org.janus.api.policystore.GetDeciderDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.SignalSource;
import org.janus.decider.model.snapshot.PolicySnapshot;
import org.janus.decider.model.snapshot.SignalSourceSnapshot;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class GrpcPolicyStoreClient implements PolicyStoreClient {

  private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub;

  @Override
  public Map<String, PolicySnapshot> getPolicies(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    var request =
        GetDeciderDegradationPoliciesRequest.newBuilder()
            .addAllDegradationIds(degradationIds)
            .build();
    var response = policyStoreStub.getDeciderDegradationPolicies(request);
    return response.getDegradationPoliciesList().stream()
        .collect(Collectors.toMap(DeciderDegradationPolicy::getDegradationId, this::toSnapshot));
  }

  @Override
  public Map<String, PolicySnapshot> getAllPolicies() {
    return Map.of();
  }

  private PolicySnapshot toSnapshot(DeciderDegradationPolicy policy) {
    return new PolicySnapshot(
        policy.getDegradationId(),
        Duration.ofMillis(Durations.toMillis(policy.getEvaluationInterval())),
        toSignalSourceSnapshot(policy.getSignalSource()),
        Instant.now());
  }

  private SignalSourceSnapshot toSignalSourceSnapshot(SignalSource signalSource) {
    return switch (signalSource.getKindCase()) {
      case DEGRADATION ->
          new SignalSourceSnapshot(
              SignalSourceSnapshot.SignalSourceType.DEGRADATION,
              signalSource.getDegradation().getDegradationId());
      case PROMETHEUS ->
          new SignalSourceSnapshot(
              SignalSourceSnapshot.SignalSourceType.PROMETHEUS,
              signalSource.getPrometheus().getMetricReference());
      case KIND_NOT_SET -> throw new IllegalArgumentException("Signal source kind is not set");
    };
  }
}
