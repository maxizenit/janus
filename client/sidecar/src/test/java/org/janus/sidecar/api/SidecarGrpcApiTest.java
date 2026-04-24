package org.janus.sidecar.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.janus.api.sidecar.SyncActualDegradationsRequest;
import org.janus.api.sidecar.SyncActualDegradationsResponse;
import org.janus.sidecar.mapper.SidecarGrpcMapper;
import org.janus.sidecar.service.handler.GetDegradationsHandler;
import org.janus.sidecar.service.handler.SyncActualDegradationsHandler;
import org.janus.sidecar.validation.ActualDegradationIdsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SidecarGrpcApiTest {

  @Mock private SyncActualDegradationsHandler syncHandler;
  @Mock private GetDegradationsHandler getHandler;
  @Mock private SidecarGrpcMapper grpcMapper;
  @Mock private ActualDegradationIdsValidator actualDegradationIdsValidator;
  @Mock private StreamObserver<SyncActualDegradationsResponse> responseObserver;

  private SidecarGrpcApi api;

  @BeforeEach
  void setUp() {
    api =
        new SidecarGrpcApi(
            syncHandler, getHandler, grpcMapper, actualDegradationIdsValidator);
  }

  @Test
  void syncActualDegradations_invalidIds_returnsInvalidArgument() {
    var request =
        SyncActualDegradationsRequest.newBuilder()
            .addDegradationIds("")
            .build();
    doThrow(new IllegalArgumentException("Degradation id must not be blank"))
        .when(actualDegradationIdsValidator)
        .validate(request.getDegradationIdsList());

    api.syncActualDegradations(request, responseObserver);

    var errorCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(responseObserver).onError(errorCaptor.capture());
    verify(responseObserver, never()).onNext(SyncActualDegradationsResponse.getDefaultInstance());
    verify(responseObserver, never()).onCompleted();
    verifyNoInteractions(grpcMapper, syncHandler);
    verifyNoMoreInteractions(responseObserver);

    var error = (StatusRuntimeException) errorCaptor.getValue();
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(error.getStatus().getDescription()).isEqualTo("Degradation id must not be blank");
  }
}
