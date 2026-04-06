package org.janus.policystore.api;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.DegradationPolicy;
import org.janus.api.policystore.DeleteDegradationPolicyRequest;
import org.janus.api.policystore.DeleteDegradationPolicyResponse;
import org.janus.api.policystore.GetDegradationPoliciesRequest;
import org.janus.api.policystore.GetDegradationPoliciesResponse;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesRequest;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesResponse;
import org.janus.api.policystore.GetSidecarDegradationPoliciesRequest;
import org.janus.api.policystore.GetSidecarDegradationPoliciesResponse;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.mapper.DegradationPolicyMapper;
import org.janus.policystore.service.DegradationPolicyService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PolicyStoreGrpcApi extends PolicyStoreServiceGrpc.PolicyStoreServiceImplBase {

  private final DegradationPolicyService policyService;
  private final DegradationPolicyMapper policyMapper;

  @Override
  public void getDegradationPolicies(
      GetDegradationPoliciesRequest request,
      StreamObserver<GetDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug(
        "GetDegradationPolicies request received: degradationCount={}", degradationIds.size());

    var policies =
        degradationIds.isEmpty()
            ? policyService.getAllPolicies()
            : policyService.getPoliciesByDegradationIds(degradationIds);
    log.debug(
        "GetDegradationPolicies request completed: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    var mappedPolicies = policies.stream().map(policyMapper::fromEntityToProto).toList();

    responseObserver.onNext(
        GetDegradationPoliciesResponse.newBuilder()
            .addAllDegradationPolicies(mappedPolicies)
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getEvaluatorDegradationPolicies(
      GetEvaluatorDegradationPoliciesRequest request,
      StreamObserver<GetEvaluatorDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug(
        "GetEvaluatorDegradationPolicies request received: degradationCount={}",
        degradationIds.size());

    var policies = policyService.getPoliciesByDegradationIds(degradationIds);
    log.debug(
        "GetEvaluatorDegradationPolicies request completed: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    var mappedPolicies =
        policies.stream()
            .filter(policy -> policy.getSignalSourceType() != null)
            .map(policyMapper::fromEntityToEvaluatorProto)
            .toList();

    responseObserver.onNext(
        GetEvaluatorDegradationPoliciesResponse.newBuilder()
            .addAllDegradationPolicies(mappedPolicies)
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getSidecarDegradationPolicies(
      GetSidecarDegradationPoliciesRequest request,
      StreamObserver<GetSidecarDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug(
        "GetSidecarDegradationPolicies request received: degradationCount={}",
        degradationIds.size());

    var policies = policyService.getPoliciesByDegradationIds(degradationIds);
    log.debug(
        "GetSidecarDegradationPolicies request completed: requested={}, returned={}",
        degradationIds.size(),
        policies.size());

    var mappedPolicies = policies.stream().map(policyMapper::fromEntityToSidecarProto).toList();

    responseObserver.onNext(
        GetSidecarDegradationPoliciesResponse.newBuilder()
            .addAllDegradationPolicies(mappedPolicies)
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void createDegradationPolicy(
      CreateDegradationPolicyRequest request, StreamObserver<DegradationPolicy> responseObserver) {
    log.info(
        "CreateDegradationPolicy request received: degradationId={}", request.getDegradationId());

    var policy = policyService.createPolicy(request);
    log.info(
        "CreateDegradationPolicy request completed: degradationId={}", policy.getDegradationId());

    var mappedPolicy = policyMapper.fromEntityToProto(policy);

    responseObserver.onNext(mappedPolicy);
    responseObserver.onCompleted();
  }

  @Override
  public void updateDegradationPolicy(
      UpdateDegradationPolicyRequest request, StreamObserver<DegradationPolicy> responseObserver) {
    log.info(
        "UpdateDegradationPolicy request received: degradationId={}, updateMaskPaths={}",
        request.getDegradationId(),
        request.getUpdateMask().getPathsList());

    var policy = policyService.updatePolicy(request);
    log.info(
        "UpdateDegradationPolicy request completed: degradationId={}", policy.getDegradationId());

    var mappedPolicy = policyMapper.fromEntityToProto(policy);

    responseObserver.onNext(mappedPolicy);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteDegradationPolicy(
      DeleteDegradationPolicyRequest request,
      StreamObserver<DeleteDegradationPolicyResponse> responseObserver) {
    log.info(
        "DeleteDegradationPolicy request received: degradationId={}", request.getDegradationId());

    policyService.deletePolicyByDegradationId(request.getDegradationId());
    log.info(
        "DeleteDegradationPolicy request completed: degradationId={}", request.getDegradationId());

    responseObserver.onNext(DeleteDegradationPolicyResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
