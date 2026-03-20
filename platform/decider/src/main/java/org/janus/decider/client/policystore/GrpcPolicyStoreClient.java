package org.janus.decider.client.policystore;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@NullMarked
public class GrpcPolicyStoreClient implements PolicyStoreClient {

  private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub;

  @Override
  public Map<String, PolicySnapshot> getPolicies(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping decider policy lookup: empty degradationIds");
      return Collections.emptyMap();
    }

    log.debug("Loading decider policies by ids: degradationCount={}", degradationIds.size());

    var request =
        GetDeciderDegradationPoliciesRequest.newBuilder()
            .addAllDegradationIds(degradationIds)
            .build();
    var response = policyStoreStub.getDeciderDegradationPolicies(request);
    var policies =
        response.getDegradationPoliciesList().stream()
            .collect(
                Collectors.toMap(DeciderDegradationPolicy::getDegradationId, this::toSnapshot));

    var missingIds = new HashSet<>(degradationIds);
    missingIds.removeAll(policies.keySet());

    log.debug(
        "Decider policies loaded by ids: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    if (!missingIds.isEmpty()) {
      log.warn(
          "Policy store returned partial decider policy response: requested={}, returned={}, missingCount={}, missingIds={}",
          degradationIds.size(),
          policies.size(),
          missingIds.size(),
          missingIds.size() <= 20 ? missingIds : "[omitted]");
    }

    return policies;
  }

  @Override
  public Map<String, PolicySnapshot> getAllPolicies() {
    log.debug("Loading all decider policies");

    var request = GetDeciderDegradationPoliciesRequest.newBuilder().build();
    var response = policyStoreStub.getDeciderDegradationPolicies(request);
    var policies =
        response.getDegradationPoliciesList().stream()
            .collect(
                Collectors.toMap(DeciderDegradationPolicy::getDegradationId, this::toSnapshot));

    log.debug("All decider policies loaded: count={}", policies.size());
    return policies;
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
