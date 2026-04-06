package org.janus.sidecar.mapper;

import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Set;
import org.janus.api.sidecar.Degradation;
import org.janus.api.sidecar.GetDegradationsResponse;
import org.janus.api.sidecar.SyncActualDegradationsRequest;
import org.janus.sidecar.model.DegradationView;
import org.janus.sidecar.model.handler.SyncActualDegradationsCommand;
import org.springframework.stereotype.Component;

@Component
public class SidecarGrpcMapper {

  public SyncActualDegradationsCommand fromSyncActualDegradationsRequestToResponse(
      SyncActualDegradationsRequest request) {
    return new SyncActualDegradationsCommand(Set.copyOf(request.getDegradationIdsList()));
  }

  public GetDegradationsResponse fromDegradationViewsToGetDegradationsResponse(
      List<DegradationView> views) {
    var builder = GetDegradationsResponse.newBuilder();

    for (var view : views) {
      var degradationBuilder =
          Degradation.newBuilder()
              .setDegradationId(view.degradationId())
              .setValue(view.value())
              .setEvaluationInterval(Durations.fromMillis(view.evaluationInterval().toMillis()));

      if (view.criticalThreshold() != null) {
        degradationBuilder.setCriticalThreshold(view.criticalThreshold());
      }
      if (view.minFallbackRatio() != null) {
        degradationBuilder.setMinFallbackRatio(view.minFallbackRatio());
      }
      if (view.maxFallbackRatio() != null) {
        degradationBuilder.setMaxFallbackRatio(view.maxFallbackRatio());
      }
      if (view.fallbackCurveExponent() != null) {
        degradationBuilder.setFallbackCurveExponent(view.fallbackCurveExponent());
      }

      builder.addDegradations(degradationBuilder);
    }

    return builder.build();
  }
}
