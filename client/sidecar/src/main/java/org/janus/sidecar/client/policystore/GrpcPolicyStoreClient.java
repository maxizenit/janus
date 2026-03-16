package org.janus.sidecar.client.policystore;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.GetSidecarDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.SidecarDegradationPolicy;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
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
        GetSidecarDegradationPoliciesRequest.newBuilder()
            .addAllDegradationIds(degradationIds)
            .build();
    var response = policyStoreStub.getSidecarDegradationPolicies(request);

    return response.getDegradationPoliciesList().stream()
        .collect(Collectors.toMap(SidecarDegradationPolicy::getDegradationId, this::toSnapshot));
  }

  private PolicySnapshot toSnapshot(SidecarDegradationPolicy policy) {
    return new PolicySnapshot(
        policy.getDegradationId(),
        Duration.ofMillis(Durations.toMillis(policy.getEvaluationInterval())),
        policy.hasCriticalThreshold() ? policy.getCriticalThreshold() : null,
        policy.hasMinFallbackRatio() ? policy.getMinFallbackRatio() : null,
        policy.hasMaxFallbackRatio() ? policy.getMaxFallbackRatio() : null,
        Instant.now());
  }
}
