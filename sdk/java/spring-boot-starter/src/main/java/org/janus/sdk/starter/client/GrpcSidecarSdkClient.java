package org.janus.sdk.starter.client;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.janus.api.sidecar.GetDegradationsRequest;
import org.janus.api.sidecar.SidecarServiceGrpc;
import org.janus.api.sidecar.SyncActualDegradationsRequest;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.janus.sdk.starter.mapper.SidecarRuntimeStateMapper;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class GrpcSidecarSdkClient implements SidecarSdkClient {

  private final SidecarServiceGrpc.SidecarServiceBlockingStub stub;
  private final SidecarRuntimeStateMapper mapper;

  @Override
  public void syncActualDegradations(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      return;
    }

    var request =
        SyncActualDegradationsRequest.newBuilder().addAllDegradationIds(degradationIds).build();

    stub.syncActualDegradations(request);
  }

  @Override
  public List<DegradationRuntimeState> getDegradations() {
    var response = stub.getDegradations(GetDegradationsRequest.getDefaultInstance());
    return response.getDegradationsList().stream().map(mapper::toRuntimeState).toList();
  }
}
