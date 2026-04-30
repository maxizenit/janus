package org.janus.adminui.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.client.policystore.PolicyStoreAdminClient;
import org.janus.adminui.model.PolicyView;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PolicyAdminService {

  private final PolicyStoreAdminClient client;

  public List<PolicyView> getPolicies() {
    return client.getAllPolicies();
  }

  public PolicyView createPolicy(PolicyView policy) {
    log.debug("Service create policy: degradationId={}", policy.degradationId());
    return client.createPolicy(policy);
  }

  public PolicyView updatePolicy(PolicyView policy) {
    log.debug("Service update policy: degradationId={}", policy.degradationId());
    return client.updatePolicy(policy);
  }

  public void deletePolicy(String degradationId) {
    log.debug("Service delete policy: degradationId={}", degradationId);
    client.deletePolicy(degradationId);
  }
}
