package org.janus.evaluator.client.policystore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.protobuf.util.Durations;
import java.util.Set;
import org.janus.api.policystore.EvaluatorDegradationPolicy;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesRequest;
import org.janus.api.policystore.GetEvaluatorDegradationPoliciesResponse;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.PrometheusMetric;
import org.janus.api.policystore.SignalSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrpcPolicyStoreClientTest {

  @Mock private PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub stub;

  @Test
  void getPolicies_skipsPolicyWithMissingSignalSource_returnsHealthyOnes() {
    var client = new GrpcPolicyStoreClient(stub);

    var healthy =
        EvaluatorDegradationPolicy.newBuilder()
            .setDegradationId("good")
            .setEvaluationInterval(Durations.fromSeconds(5))
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(PrometheusMetric.newBuilder().setQuery("up").build())
                    .build())
            .build();
    var broken =
        EvaluatorDegradationPolicy.newBuilder()
            .setDegradationId("bad")
            .setEvaluationInterval(Durations.fromSeconds(5))
            // signal source intentionally omitted -> KIND_NOT_SET
            .build();

    when(stub.getEvaluatorDegradationPolicies(any(GetEvaluatorDegradationPoliciesRequest.class)))
        .thenReturn(
            GetEvaluatorDegradationPoliciesResponse.newBuilder()
                .addDegradationPolicies(healthy)
                .addDegradationPolicies(broken)
                .build());

    var result = client.getPolicies(Set.of("good", "bad"));

    assertThat(result).containsOnlyKeys("good");
    assertThat(result.get("good").signalSource().query()).isEqualTo("up");
  }

  @Test
  void getAllPolicies_skipsPolicyWithMissingSignalSource_returnsHealthyOnes() {
    var client = new GrpcPolicyStoreClient(stub);

    var healthy =
        EvaluatorDegradationPolicy.newBuilder()
            .setDegradationId("good")
            .setEvaluationInterval(Durations.fromSeconds(5))
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(PrometheusMetric.newBuilder().setQuery("up").build())
                    .build())
            .build();
    var broken =
        EvaluatorDegradationPolicy.newBuilder()
            .setDegradationId("bad")
            .setEvaluationInterval(Durations.fromSeconds(5))
            .build();

    when(stub.getEvaluatorDegradationPolicies(any(GetEvaluatorDegradationPoliciesRequest.class)))
        .thenReturn(
            GetEvaluatorDegradationPoliciesResponse.newBuilder()
                .addDegradationPolicies(healthy)
                .addDegradationPolicies(broken)
                .build());

    var result = client.getAllPolicies();

    assertThat(result).containsOnlyKeys("good");
  }

  private static <T> T any(Class<T> type) {
    return org.mockito.ArgumentMatchers.any(type);
  }
}
