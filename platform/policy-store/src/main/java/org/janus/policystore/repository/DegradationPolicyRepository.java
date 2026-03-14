package org.janus.policystore.repository;

import org.janus.policystore.entity.DegradationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DegradationPolicyRepository extends JpaRepository<DegradationPolicy, String> {

  boolean existsById(String degradationId);

  boolean existsBySourceDegradationId(String sourceDegradationId);
}
