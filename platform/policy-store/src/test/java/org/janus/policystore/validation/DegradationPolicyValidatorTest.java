package org.janus.policystore.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.entity.SignalSourceType;
import org.janus.policystore.exception.PolicyAlreadyExistsException;
import org.janus.policystore.exception.PolicyNotFoundException;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradationPolicyValidatorTest {

  @Mock private DegradationPolicyRepository policyRepository;

  private DegradationPolicyValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DegradationPolicyValidator(policyRepository);
  }

  @Test
  void validateForCreate_acceptsValidPolicy() {
    var policy = validPolicy();
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(false);

    assertThatCode(() -> validator.validateForCreate(policy)).doesNotThrowAnyException();
  }

  @Test
  void validateForCreate_rejectsExistingPolicy() {
    var policy = validPolicy();
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForCreate(policy))
        .isInstanceOf(PolicyAlreadyExistsException.class)
        .hasMessageContaining(policy.getDegradationId());
  }

  @Test
  void validateForDelete_rejectsMissingPolicy() {
    when(policyRepository.existsById("missing-policy")).thenReturn(false);

    assertThatThrownBy(() -> validator.validateForDelete("missing-policy"))
        .isInstanceOf(PolicyNotFoundException.class)
        .hasMessageContaining("missing-policy");
  }

  @Test
  void validateForUpdate_rejectsBlankDegradationId() {
    var policy = validPolicy();
    policy.setDegradationId(" ");
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("degradationId required");
  }

  @Test
  void validateForUpdate_rejectsNonPositiveEvaluationInterval() {
    var policy = validPolicy();
    policy.setEvaluationIntervalMs(0L);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("evaluationIntervalMs must be positive");
  }

  @Test
  void validateForUpdate_rejectsCriticalThresholdOutsideRange() {
    var policy = validPolicy();
    policy.setCriticalThreshold(1.1);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("criticalThreshold must be in range");
  }

  @Test
  void validateForUpdate_rejectsMinFallbackRatioOutsideRange() {
    var policy = validPolicy();
    policy.setMinFallbackRatio(-0.1);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minFallbackRatio must be in range");
  }

  @Test
  void validateForUpdate_rejectsMaxFallbackRatioOutsideRange() {
    var policy = validPolicy();
    policy.setMaxFallbackRatio(1.5);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxFallbackRatio must be in range");
  }

  @Test
  void validateForUpdate_rejectsFallbackCurveExponentZero() {
    var policy = validPolicy();
    policy.setFallbackCurveExponent(0.0);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fallbackCurveExponent must be positive");
  }

  @Test
  void validateForUpdate_rejectsMinFallbackRatioGreaterThanMax() {
    var policy = validPolicy();
    policy.setMinFallbackRatio(0.8);
    policy.setMaxFallbackRatio(0.2);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minFallbackRatio must be <= maxFallbackRatio");
  }

  @Test
  void validateForUpdate_rejectsPrometheusSourceWithoutQuery() {
    var policy = validPolicy();
    policy.setSourcePrometheusQuery(" ");
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatThrownBy(() -> validator.validateForUpdate(policy))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sourcePrometheusQuery required");
  }

  @Test
  void validateForUpdate_allowsOptionalFallbackFieldsToBeUnset() {
    var policy = validPolicy();
    policy.setCriticalThreshold(null);
    policy.setMinFallbackRatio(null);
    policy.setMaxFallbackRatio(null);
    policy.setFallbackCurveExponent(null);
    when(policyRepository.existsById(policy.getDegradationId())).thenReturn(true);

    assertThatCode(() -> validator.validateForUpdate(policy)).doesNotThrowAnyException();
  }

  private DegradationPolicy validPolicy() {
    var policy = new DegradationPolicy();
    policy.setDegradationId("recommendation-quality");
    policy.setEvaluationIntervalMs(30000L);
    policy.setSignalSourceType(SignalSourceType.PROMETHEUS);
    policy.setSourcePrometheusQuery("http_server_requests_seconds_count");
    policy.setCriticalThreshold(0.7);
    policy.setMinFallbackRatio(0.1);
    policy.setMaxFallbackRatio(0.9);
    policy.setFallbackCurveExponent(2.0);
    return policy;
  }
}
