package org.janus.adminui.client.policystore;

import java.util.List;
import org.janus.adminui.model.PolicyView;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PolicyStoreAdminClient {

  List<PolicyView> getAllPolicies();

  PolicyView createPolicy(PolicyView policy);

  PolicyView updatePolicy(PolicyView policy);

  void deletePolicy(String degradationId);
}
