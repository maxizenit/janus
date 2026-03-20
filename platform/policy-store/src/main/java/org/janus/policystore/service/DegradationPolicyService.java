package org.janus.policystore.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.mapper.DegradationPolicyMapper;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.janus.policystore.validation.DegradationPolicyValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationPolicyService {

  private final DegradationPolicyRepository policyRepository;
  private final DegradationPolicyValidator policyValidator;
  private final DegradationPolicyMapper policyMapper;

  @Transactional(readOnly = true)
  public List<DegradationPolicy> getAllPolicies() {
    log.debug("Loading all degradation policies");
    var policies = policyRepository.findAll();
    log.debug("All degradation policies loaded: count={}", policies.size());
    return policies;
  }

  @Transactional(readOnly = true)
  public List<DegradationPolicy> getPoliciesByDegradationIds(List<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping degradation policy lookup: empty degradationIds");
      return List.of();
    }

    log.debug("Loading degradation policies by ids: degradationCount={}", degradationIds.size());
    var policies = policyRepository.findAllById(degradationIds);
    log.debug(
        "Degradation policies loaded by ids: requested={}, found={}, missing={}",
        degradationIds.size(),
        policies.size(),
        degradationIds.size() - policies.size());
    return policies;
  }

  public DegradationPolicy createPolicy(CreateDegradationPolicyRequest createRequest) {
    log.info(
        "Creating degradation policy started: degradationId={}", createRequest.getDegradationId());

    var policy = policyMapper.fromCreateRequestProtoToEntity(createRequest);
    policyValidator.validateForCreate(policy);
    var savedPolicy = policyRepository.save(policy);

    log.info(
        "Creating degradation policy completed: degradationId={}, signalSourceType={}, evaluationIntervalMs={}",
        savedPolicy.getDegradationId(),
        savedPolicy.getSignalSourceType(),
        savedPolicy.getEvaluationIntervalMs());
    return savedPolicy;
  }

  public DegradationPolicy updatePolicy(UpdateDegradationPolicyRequest updateRequest) {
    var degradationId = updateRequest.getDegradationId();
    log.info(
        "Updating degradation policy started: degradationId={}, updateMaskPaths={}",
        degradationId,
        updateRequest.getUpdateMask().getPathsList());

    var policy =
        policyRepository
            .findById(degradationId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "Updating degradation policy failed: policy not found, degradationId={}",
                      degradationId);
                  return new IllegalArgumentException("Policy not found: " + degradationId);
                });

    var oldEvaluationIntervalMs = policy.getEvaluationIntervalMs();
    var oldSignalSourceType = policy.getSignalSourceType();
    var oldCriticalThreshold = policy.getCriticalThreshold();
    var oldMinFallbackRatio = policy.getMinFallbackRatio();
    var oldMaxFallbackRatio = policy.getMaxFallbackRatio();

    policyMapper.updateEntityFromUpdateRequestProto(policy, updateRequest);
    policyValidator.validateForUpdate(policy);
    var savedPolicy = policyRepository.save(policy);

    log.info(
        "Updating degradation policy completed: degradationId={}, oldEvaluationIntervalMs={}, newEvaluationIntervalMs={}, oldSignalSourceType={}, newSignalSourceType={}, oldCriticalThreshold={}, newCriticalThreshold={}, oldMinFallbackRatio={}, newMinFallbackRatio={}, oldMaxFallbackRatio={}, newMaxFallbackRatio={}",
        savedPolicy.getDegradationId(),
        oldEvaluationIntervalMs,
        savedPolicy.getEvaluationIntervalMs(),
        oldSignalSourceType,
        savedPolicy.getSignalSourceType(),
        oldCriticalThreshold,
        savedPolicy.getCriticalThreshold(),
        oldMinFallbackRatio,
        savedPolicy.getMinFallbackRatio(),
        oldMaxFallbackRatio,
        savedPolicy.getMaxFallbackRatio());
    return savedPolicy;
  }

  public void deletePolicyByDegradationId(String degradationId) {
    log.info("Deleting degradation policy started: degradationId={}", degradationId);
    policyValidator.validateForDelete(degradationId);
    policyRepository.deleteById(degradationId);
    log.info("Deleting degradation policy completed: degradationId={}", degradationId);
  }
}
