package org.janus.policystore.service;

import static org.janus.policystore.configuration.CacheConfiguration.POLICIES_ALL_CACHE;
import static org.janus.policystore.configuration.CacheConfiguration.POLICIES_BY_IDS_CACHE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.exception.PolicyNotFoundException;
import org.janus.policystore.mapper.DegradationPolicyMapper;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.janus.policystore.validation.DegradationPolicyValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
  @Cacheable(POLICIES_ALL_CACHE)
  public List<DegradationPolicy> getAllPolicies() {
    log.debug("Loading all degradation policies");
    var policies = policyRepository.findAll();
    log.debug("All degradation policies loaded: count={}", policies.size());
    return policies;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = POLICIES_BY_IDS_CACHE, key = "#degradationIds", unless = "#result.isEmpty()")
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

  @Caching(
      evict = {
        @CacheEvict(value = POLICIES_ALL_CACHE, allEntries = true),
        @CacheEvict(value = POLICIES_BY_IDS_CACHE, allEntries = true)
      })
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

  @Caching(
      evict = {
        @CacheEvict(value = POLICIES_ALL_CACHE, allEntries = true),
        @CacheEvict(value = POLICIES_BY_IDS_CACHE, allEntries = true)
      })
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
                  return new PolicyNotFoundException(degradationId);
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

  @Caching(
      evict = {
        @CacheEvict(value = POLICIES_ALL_CACHE, allEntries = true),
        @CacheEvict(value = POLICIES_BY_IDS_CACHE, allEntries = true)
      })
  public void deletePolicyByDegradationId(String degradationId) {
    log.info("Deleting degradation policy started: degradationId={}", degradationId);
    policyValidator.validateForDelete(degradationId);
    policyRepository.deleteById(degradationId);
    log.info("Deleting degradation policy completed: degradationId={}", degradationId);
  }
}
