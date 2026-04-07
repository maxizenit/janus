package org.janus.policystore.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationPolicyValidator {

  private final DegradationPolicyRepository policyRepository;

  public void validateForCreate(DegradationPolicy policy) {
    log.debug(
        "Validating degradation policy for create: degradationId={}", policy.getDegradationId());

    require(
        !policyRepository.existsById(policy.getDegradationId()),
        "Policy already exists: " + policy.getDegradationId());
    validate(policy);

    log.debug(
        "Degradation policy validation for create completed: degradationId={}",
        policy.getDegradationId());
  }

  public void validateForUpdate(DegradationPolicy policy) {
    log.debug(
        "Validating degradation policy for update: degradationId={}", policy.getDegradationId());

    require(
        policyRepository.existsById(policy.getDegradationId()),
        "Policy not found: " + policy.getDegradationId());
    validate(policy);

    log.debug(
        "Degradation policy validation for update completed: degradationId={}",
        policy.getDegradationId());
  }

  public void validateForDelete(String degradationId) {
    require(policyRepository.existsById(degradationId), "Policy not found: " + degradationId);
    log.debug(
        "Degradation policy validation for delete completed: degradationId={}", degradationId);
  }

  private void validate(DegradationPolicy policy) {
    validateSource(policy);
    validateFallback(policy);
  }

  private void validateSource(DegradationPolicy policy) {
    if (policy.getSignalSourceType() == null) {
      require(
          policy.getSourcePrometheusQuery() == null,
          "sourcePrometheusQuery must be null");
      return;
    }

    switch (policy.getSignalSourceType()) {
      case PROMETHEUS ->
          requireNotBlank(
              policy.getSourcePrometheusQuery(),
              "sourcePrometheusQuery required");
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
    if (!condition) {
      log.warn("Degradation policy validation failed: {}", message);
      throw new IllegalArgumentException(message);
    }
  }

  private static void requireNotBlank(@Nullable String value, String message) {
    if (value == null || value.isBlank()) {
      log.warn("Degradation policy validation failed: {}", message);
      throw new IllegalArgumentException(message);
    }
  }
}
