package org.janus.policystore.validation;

import lombok.RequiredArgsConstructor;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class DegradationPolicyValidator {

  private final DegradationPolicyRepository policyRepository;

  public void validateForCreate(DegradationPolicy policy) {
    require(
        !policyRepository.existsById(policy.getDegradationId()),
        "Policy already exists: " + policy.getDegradationId());

    validate(policy);
  }

  public void validateForUpdate(DegradationPolicy policy) {
    require(
        policyRepository.existsById(policy.getDegradationId()),
        "Policy not found: " + policy.getDegradationId());

    validate(policy);
  }

  public void validateForDelete(String degradationId) {
    require(policyRepository.existsById(degradationId), "Policy not found: " + degradationId);

    require(
        !policyRepository.existsBySourceDegradationId(degradationId),
        "Policy is referenced by other policies: " + degradationId);
  }

  private void validate(DegradationPolicy policy) {
    validateSource(policy);
    validateFallback(policy);
  }

  private void validateSource(DegradationPolicy policy) {
    switch (policy.getSignalSourceType()) {
      case DEGRADATION -> {
        requireNotBlank(policy.getSourceDegradationId(), "sourceDegradationId required");
        require(
            policy.getSourcePrometheusMetricReference() == null,
            "sourcePrometheusMetricReference must be null");

        require(
            !policy.getDegradationId().equals(policy.getSourceDegradationId()),
            "policy must not reference itself");

        require(
            policyRepository.existsById(policy.getSourceDegradationId()),
            "source policy not found: " + policy.getSourceDegradationId());
      }

      case PROMETHEUS -> {
        requireNotBlank(
            policy.getSourcePrometheusMetricReference(), "sourcePrometheusMetricReference required");
        require(policy.getSourceDegradationId() == null, "sourceDegradationId must be null");
      }
    }
  }

  private void validateFallback(DegradationPolicy policy) {

    if (policy.getMinFallbackRatio() != null && policy.getMaxFallbackRatio() != null) {

      require(
          policy.getMinFallbackRatio() <= policy.getMaxFallbackRatio(),
          "minFallbackRatio must be <= maxFallbackRatio");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private static void requireNotBlank(@Nullable String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
  }
}
