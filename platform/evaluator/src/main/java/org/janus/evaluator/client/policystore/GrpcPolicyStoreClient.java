package org.janus.evaluator.client.policystore;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.policystore.EvaluatorDegradationPolicy;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.SignalSource;
import org.janus.evaluator.model.snapshot.PolicySnapshot;
import org.janus.evaluator.model.snapshot.SignalSourceSnapshot;
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
      log.debug("Skipping evaluator policy lookup: empty degradationIds");
      return Collections.emptyMap();
    }

    log.debug("Loading evaluator policies by ids: degradationCount={}", degradationIds.size());

    var request =
        GetEvaluatorDegradationPoliciesRequest.newBuilder()
            .addAllDegradationIds(degradationIds)
            .build();
    var response = policyStoreStub.getEvaluatorDegradationPolicies(request);
    var policies = toSnapshots(response.getDegradationPoliciesList());

    var missingIds = new HashSet<>(degradationIds);
    missingIds.removeAll(policies.keySet());

    log.debug(
        "Evaluator policies loaded by ids: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    if (!missingIds.isEmpty()) {
      log.warn(
          "Policy store returned partial evaluator policy response: requested={}, returned={}, missingCount={}, missingIds={}",
          degradationIds.size(),
          policies.size(),
          missingIds.size(),
          missingIds.size() <= 20 ? missingIds : "[omitted]");
    }

    return policies;
  }

  @Override
  public Map<String, PolicySnapshot> getAllPolicies() {
    log.debug("Loading all evaluator policies");

    var request = GetEvaluatorDegradationPoliciesRequest.newBuilder().build();
    var response = policyStoreStub.getEvaluatorDegradationPolicies(request);
    var policies = toSnapshots(response.getDegradationPoliciesList());

    log.debug("All evaluator policies loaded: count={}", policies.size());
    return policies;
  }

  private Map<String, PolicySnapshot> toSnapshots(List<EvaluatorDegradationPolicy> policies) {
    var result = new HashMap<String, PolicySnapshot>(policies.size());
    for (var policy : policies) {
      try {
        result.put(policy.getDegradationId(), toSnapshot(policy));
      } catch (RuntimeException e) {
        log.warn(
            "Skipping evaluator policy with unparseable signal source: degradationId={}",
            policy.getDegradationId(),
            e);
      }
    }
    return result;
  }

  private PolicySnapshot toSnapshot(EvaluatorDegradationPolicy policy) {
    return new PolicySnapshot(
        policy.getDegradationId(),
        Duration.ofMillis(Durations.toMillis(policy.getEvaluationInterval())),
        toSignalSourceSnapshot(policy.getSignalSource()),
        Instant.now());
  }

  private SignalSourceSnapshot toSignalSourceSnapshot(SignalSource signalSource) {
    return switch (signalSource.getKindCase()) {
      case PROMETHEUS ->
          new SignalSourceSnapshot(
              SignalSourceSnapshot.SignalSourceType.PROMETHEUS,
              signalSource.getPrometheus().getQuery());
      case KIND_NOT_SET -> throw new IllegalArgumentException("Signal source kind is not set");
    };
  }
}
