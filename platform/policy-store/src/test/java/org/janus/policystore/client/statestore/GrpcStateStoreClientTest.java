package org.janus.policystore.client.statestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.janus.api.statestore.ClearDegradationStatesRequest;
import org.janus.api.statestore.ClearDegradationStatesResponse;
import org.janus.api.statestore.DegradationStateUpdateSource;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrpcStateStoreClientTest {

  @Mock private StateStoreServiceGrpc.StateStoreServiceBlockingStub stub;

  @Test
  void clearAllStates_sendsRequestForEveryRecognizedSource() {
    var client = new GrpcStateStoreClient(stub);
    when(stub.clearDegradationStates(org.mockito.ArgumentMatchers.any()))
        .thenReturn(ClearDegradationStatesResponse.getDefaultInstance());

    client.clearAllStates("deg-1");

    var requestCaptor = ArgumentCaptor.forClass(ClearDegradationStatesRequest.class);
    verify(stub, org.mockito.Mockito.times(2)).clearDegradationStates(requestCaptor.capture());

    var requests = requestCaptor.getAllValues();
    assertThat(requests)
        .extracting(ClearDegradationStatesRequest::getSource)
        .containsExactlyInAnyOrder(
            DegradationStateUpdateSource.ADMIN_UI, DegradationStateUpdateSource.EVALUATOR);
    assertThat(requests)
        .allSatisfy(
            request -> assertThat(request.getDegradationIdsList()).containsExactly("deg-1"));
  }
}
