package org.janus.statestore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import org.janus.api.statestore.DegradationStateUpdate;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.janus.api.statestore.UpdateDegradationStatesResponse;
import org.janus.statestore.mapper.DegradationStateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateSourceMapper;
import org.janus.statestore.model.DegradationStateUpdateSource;
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
    api.updateDegradationStates(
        UpdateDegradationStatesRequest.getDefaultInstance(), responseObserver);

    var errorCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(responseObserver).onError(errorCaptor.capture());
    verify(stateService, never()).updateDegradationStates(anyList());

    var error = (StatusRuntimeException) errorCaptor.getValue();
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
  }

  @Test
  void updateDegradationStates_validationError_returnsInvalidArgument() {
    var grpcUpdate =
        DegradationStateUpdate.newBuilder()
            .setDegradationId("deg-1")
            .setValue(1.5)
            .build();
    var domainUpdate =
        new org.janus.statestore.model.DegradationStateUpdate(
            "deg-1", 1.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ZERO);
    var request =
        UpdateDegradationStatesRequest.newBuilder()
            .setSource(org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI)
            .addUpdates(grpcUpdate)
            .build();

    when(updateMapper.fromGrpcToDomain(
            any(DegradationStateUpdate.class),
            any(org.janus.api.statestore.DegradationStateUpdateSource.class)))
        .thenReturn(domainUpdate);
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Value must be in range [0.0, 1.0]"))
        .when(stateService)
        .updateDegradationStates(anyList());

    api.updateDegradationStates(request, responseObserver);

    var errorCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(responseObserver).onError(errorCaptor.capture());
    verify(responseObserver, never()).onNext(UpdateDegradationStatesResponse.getDefaultInstance());
    verify(responseObserver, never()).onCompleted();
    verifyNoMoreInteractions(responseObserver);

    var error = (StatusRuntimeException) errorCaptor.getValue();
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(error.getStatus().getDescription()).isEqualTo("Value must be in range [0.0, 1.0]");
  }
}
