package org.janus.policystore.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
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
@NullMarked
public class DegradationPolicyService {

  private final DegradationPolicyRepository policyRepository;
  private final DegradationPolicyValidator policyValidator;
  private final DegradationPolicyMapper policyMapper;

  @Transactional(readOnly = true)
  public List<DegradationPolicy> getAllPolicies() {
    return policyRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<DegradationPolicy> getPoliciesByDegradationIds(List<String> degradationIds) {
    return policyRepository.findAllById(degradationIds);
  }

  public DegradationPolicy createPolicy(CreateDegradationPolicyRequest createRequest) {
    var policy = policyMapper.fromCreateRequestProtoToEntity(createRequest);
    policyValidator.validateForCreate(policy);
    return policyRepository.save(policy);
  }

  public DegradationPolicy updatePolicy(UpdateDegradationPolicyRequest updateRequest) {
    var policy = policyRepository.findById(updateRequest.getDegradationId()).orElseThrow();
    policyMapper.updateEntityFromUpdateRequestProto(policy, updateRequest);
    policyValidator.validateForUpdate(policy);
    return policyRepository.save(policy);
  }

  public void deletePolicyByDegradationId(String degradationId) {
    policyValidator.validateForDelete(degradationId);
    policyRepository.deleteById(degradationId);
  }
}
