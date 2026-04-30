package org.janus.evaluator.client.policystore;

import java.util.Map;
import java.util.Set;
import org.janus.evaluator.model.snapshot.PolicySnapshot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PolicyStoreClient {

  Map<String, PolicySnapshot> getPolicies(Set<String> degradationIds);

  Map<String, PolicySnapshot> getAllPolicies();
}
