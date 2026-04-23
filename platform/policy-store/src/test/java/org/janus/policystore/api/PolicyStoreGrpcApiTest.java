package org.janus.policystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Duration;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.janus.api.policystore.EvaluatorDegradationPolicy;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesRequest;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesResponse;
import org.janus.api.policystore.PrometheusMetric;
import org.janus.api.policystore.SignalSource;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.entity.SignalSourceType;
import org.janus.policystore.mapper.DegradationPolicyMapper;
import org.janus.policystore.service.DegradationPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyStoreGrpcApiTest {

  @Mock private DegradationPolicyService policyService;
  @Mock private DegradationPolicyMapper policyMapper;
  @Mock private StreamObserver<GetEvaluatorDegradationPoliciesResponse> responseObserver;

  private PolicyStoreGrpcApi api;

  @BeforeEach
  void setUp() {
    api = new PolicyStoreGrpcApi(policyService, policyMapper);
  }

  @Test
  void getEvaluatorDegradationPolicies_emptyIds_loadsAllPolicies() {
    var evaluatorPolicy = policy("auto-policy", SignalSourceType.PROMETHEUS);
    var manualPolicy = policy("manual-policy", null);
    var evaluatorProto =
        EvaluatorDegradationPolicy.newBuilder()
            .setDegradationId("auto-policy")
            .setEvaluationInterval(Duration.newBuilder().setSeconds(30).build())
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(PrometheusMetric.newBuilder().setQuery("up").build())
                    .build())
            .build();

    when(policyService.getAllPolicies()).thenReturn(List.of(evaluatorPolicy, manualPolicy));
    when(policyMapper.fromEntityToEvaluatorProto(evaluatorPolicy)).thenReturn(evaluatorProto);

    api.getEvaluatorDegradationPolicies(
        GetEvaluatorDegradationPoliciesRequest.getDefaultInstance(), responseObserver);

    var responseCaptor =
        ArgumentCaptor.forClass(GetEvaluatorDegradationPoliciesResponse.class);
    verify(responseObserver).onNext(responseCaptor.capture());
    verify(responseObserver).onCompleted();
    verify(policyService).getAllPolicies();
    verify(policyService, never()).getPoliciesByDegradationIds(List.of());

    assertThat(responseCaptor.getValue().getDegradationPoliciesList())
        .containsExactly(evaluatorProto);
  }

  private DegradationPolicy policy(String degradationId, SignalSourceType signalSourceType) {
    var policy = new DegradationPolicy();
    policy.setDegradationId(degradationId);
    policy.setEvaluationIntervalMs(30000L);
    policy.setSignalSourceType(signalSourceType);
    policy.setSourcePrometheusQuery(signalSourceType == null ? null : "up");
    return policy;
  }
}
