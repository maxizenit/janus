package org.janus.adminui.client.policystore;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.mapper.PolicyViewMapper;
import org.janus.adminui.model.PolicyView;
import org.janus.api.policystore.DeleteDegradationPolicyRequest;
import org.janus.api.policystore.GetDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class GrpcPolicyStoreAdminClient implements PolicyStoreAdminClient {

  private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub stub;
  private final PolicyViewMapper mapper;

  @Override
  public List<PolicyView> getAllPolicies() {
    log.debug("Loading all policies");
    var response = stub.getDegradationPolicies(GetDegradationPoliciesRequest.getDefaultInstance());
    log.debug("Policies loaded: count={}", response.getDegradationPoliciesCount());
    return response.getDegradationPoliciesList().stream().map(mapper::fromGrpc).toList();
  }

  @Override
  public PolicyView createPolicy(PolicyView policy) {
    log.info("Creating policy: degradationId={}", policy.degradationId());
    var created = stub.createDegradationPolicy(mapper.toCreateRequest(policy));
    log.info("Policy created: degradationId={}", created.getDegradationId());
    return mapper.fromGrpc(created);
  }

  @Override
  public PolicyView updatePolicy(PolicyView policy) {
    log.info("Updating policy: degradationId={}", policy.degradationId());
    var updated = stub.updateDegradationPolicy(mapper.toUpdateRequest(policy));
    log.info("Policy updated: degradationId={}", updated.getDegradationId());
    return mapper.fromGrpc(updated);
  }

  @Override
  public void deletePolicy(String degradationId) {
    log.info("Deleting policy: degradationId={}", degradationId);
    stub.deleteDegradationPolicy(
        DeleteDegradationPolicyRequest.newBuilder().setDegradationId(degradationId).build());
    log.info("Policy deleted: degradationId={}", degradationId);
  }
}
