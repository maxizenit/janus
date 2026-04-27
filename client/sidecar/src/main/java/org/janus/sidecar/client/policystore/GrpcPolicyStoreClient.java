package org.janus.sidecar.client.policystore;

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
import org.janus.api.policystore.GetSidecarDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.SidecarDegradationPolicy;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
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
      log.debug("Skipping policy store lookup: empty degradationIds");
      return Collections.emptyMap();
    }

    log.debug("Requesting policies from policy store: requested={}", degradationIds.size());

    var request =
        GetSidecarDegradationPoliciesRequest.newBuilder()
            .addAllDegradationIds(degradationIds)
            .build();
    var response = policyStoreStub.getSidecarDegradationPolicies(request);
    var policies =
        response.getDegradationPoliciesList().stream()
            .collect(
                Collectors.toMap(SidecarDegradationPolicy::getDegradationId, this::toSnapshot));

    log.debug(
        "Policies received from policy store: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    var missingIds = new HashSet<>(degradationIds);
    missingIds.removeAll(policies.keySet());
    if (!missingIds.isEmpty()) {
      log.warn(
          "Policy store returned partial response: missingCount={}, missingIds={}",
          missingIds.size(),
          missingIds.size() <= 20 ? missingIds : "[omitted]");
    }

    return policies;
  }

  private PolicySnapshot toSnapshot(SidecarDegradationPolicy policy) {
    return new PolicySnapshot(
        policy.getDegradationId(),
        Duration.ofMillis(Durations.toMillis(policy.getEvaluationInterval())),
        policy.getCriticalThreshold(),
        policy.getMinFallbackRatio(),
        policy.getMaxFallbackRatio(),
        policy.getFallbackCurveExponent(),
        Instant.now());
  }
}
