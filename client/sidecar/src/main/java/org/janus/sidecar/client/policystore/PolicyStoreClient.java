package org.janus.sidecar.client.policystore;

import java.util.Map;
import java.util.Set;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PolicyStoreClient {

  Map<String, PolicySnapshot> getPolicies(Set<String> degradationIds);
}
