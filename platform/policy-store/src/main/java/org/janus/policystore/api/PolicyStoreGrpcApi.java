package org.janus.policystore.api;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.DegradationPolicy;
import org.janus.api.policystore.DeleteDegradationPolicyRequest;
import org.janus.api.policystore.DeleteDegradationPolicyResponse;
import org.janus.api.policystore.GetDeciderDegradationPoliciesRequest;
import org.janus.api.policystore.GetDeciderDegradationPoliciesResponse;
import org.janus.api.policystore.GetDegradationPoliciesRequest;
import org.janus.api.policystore.GetDegradationPoliciesResponse;
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
@NullMarked
public class PolicyStoreGrpcApi extends PolicyStoreServiceGrpc.PolicyStoreServiceImplBase {

  private final DegradationPolicyService policyService;
  private final DegradationPolicyMapper policyMapper;

  @Override
  public void getDegradationPolicies(
      GetDegradationPoliciesRequest request,
      StreamObserver<GetDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    var policies =
        degradationIds.isEmpty()
            ? policyService.getAllPolicies()
            : policyService.getPoliciesByDegradationIds(degradationIds);
    var mappedPolicies = policies.stream().map(policyMapper::fromEntityToProto).toList();

    responseObserver.onNext(
        GetDegradationPoliciesResponse.newBuilder()
            .addAllDegradationPolicies(mappedPolicies)
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getDeciderDegradationPolicies(
      GetDeciderDegradationPoliciesRequest request,
      StreamObserver<GetDeciderDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    var policies = policyService.getPoliciesByDegradationIds(degradationIds);
    var mappedPolicies = policies.stream().map(policyMapper::fromEntityToDeciderProto).toList();

    responseObserver.onNext(
        GetDeciderDegradationPoliciesResponse.newBuilder()
            .addAllDegradationPolicies(mappedPolicies)
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getSidecarDegradationPolicies(
      GetSidecarDegradationPoliciesRequest request,
      StreamObserver<GetSidecarDegradationPoliciesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    var policies = policyService.getPoliciesByDegradationIds(degradationIds);
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
    var policy = policyService.createPolicy(request);
    var mappedPolicy = policyMapper.fromEntityToProto(policy);

    responseObserver.onNext(mappedPolicy);
    responseObserver.onCompleted();
  }

  @Override
  public void updateDegradationPolicy(
      UpdateDegradationPolicyRequest request, StreamObserver<DegradationPolicy> responseObserver) {
    var policy = policyService.updatePolicy(request);
    var mappedPolicy = policyMapper.fromEntityToProto(policy);

    responseObserver.onNext(mappedPolicy);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteDegradationPolicy(
      DeleteDegradationPolicyRequest request,
      StreamObserver<DeleteDegradationPolicyResponse> responseObserver) {
    policyService.deletePolicyByDegradationId(request.getDegradationId());

    responseObserver.onNext(DeleteDegradationPolicyResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
