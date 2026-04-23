package org.janus.statestore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.janus.api.statestore.UpdateDegradationStatesResponse;
import org.janus.statestore.mapper.DegradationStateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateSourceMapper;
import org.janus.statestore.service.DegradationStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateStoreGrpcApiTest {

  @Mock private DegradationStateService stateService;
  @Mock private DegradationStateMapper stateMapper;
  @Mock private DegradationStateUpdateMapper updateMapper;
  @Mock private StreamObserver<UpdateDegradationStatesResponse> responseObserver;

  private StateStoreGrpcApi api;

  @BeforeEach
  void setUp() {
    api =
        new StateStoreGrpcApi(
            stateService,
            stateMapper,
            updateMapper,
            new DegradationStateUpdateSourceMapper());
  }

  @Test
  void updateDegradationStates_unspecifiedSource_rejected() {
    api.updateDegradationStates(UpdateDegradationStatesRequest.getDefaultInstance(), responseObserver);

    var errorCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(responseObserver).onError(errorCaptor.capture());
    verify(stateService, never()).updateDegradationStates(org.mockito.ArgumentMatchers.anyList());

    var error = (StatusRuntimeException) errorCaptor.getValue();
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
  }
}
