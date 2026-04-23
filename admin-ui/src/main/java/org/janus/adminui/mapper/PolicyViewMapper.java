package org.janus.adminui.mapper;

import com.google.protobuf.FieldMask;
import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.model.SignalSourceTypeView;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.DegradationPolicy;
import org.janus.api.policystore.SignalSource;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.springframework.stereotype.Component;

@Component
public class PolicyViewMapper {

  public PolicyView fromGrpc(DegradationPolicy policy) {
    SignalSourceTypeView signalSourceType =
        policy.hasSignalSource() ? SignalSourceTypeView.PROMETHEUS : SignalSourceTypeView.MANUAL;

    String query =
        policy.hasSignalSource() && policy.getSignalSource().hasPrometheus()
            ? policy.getSignalSource().getPrometheus().getQuery()
            : null;

    return new PolicyView(
        policy.getDegradationId(),
        Duration.ofMillis(Durations.toMillis(policy.getEvaluationInterval())),
        signalSourceType,
        query,
        policy.hasCriticalThreshold() ? policy.getCriticalThreshold() : null,
        policy.hasMinFallbackRatio() ? policy.getMinFallbackRatio() : null,
        policy.hasMaxFallbackRatio() ? policy.getMaxFallbackRatio() : null,
        policy.hasFallbackCurveExponent() ? policy.getFallbackCurveExponent() : null);
  }

  public CreateDegradationPolicyRequest toCreateRequest(PolicyView view) {
    var builder =
        CreateDegradationPolicyRequest.newBuilder()
            .setDegradationId(view.degradationId())
            .setEvaluationInterval(Durations.fromMillis(view.evaluationInterval().toMillis()));

    if (!SignalSourceTypeView.MANUAL.equals(view.signalSourceType())) {
      builder.setSignalSource(toSignalSource(view));
    }
    if (view.criticalThreshold() != null) {
      builder.setCriticalThreshold(view.criticalThreshold());
    }
    if (view.minFallbackRatio() != null) {
      builder.setMinFallbackRatio(view.minFallbackRatio());
    }
    if (view.maxFallbackRatio() != null) {
      builder.setMaxFallbackRatio(view.maxFallbackRatio());
    }
    if (view.fallbackCurveExponent() != null) {
      builder.setFallbackCurveExponent(view.fallbackCurveExponent());
    }

    return builder.build();
  }

  public UpdateDegradationPolicyRequest toUpdateRequest(PolicyView view) {
    List<String> paths = new ArrayList<>();
    var builder =
        UpdateDegradationPolicyRequest.newBuilder().setDegradationId(view.degradationId());

    builder.setEvaluationInterval(Durations.fromMillis(view.evaluationInterval().toMillis()));
    paths.add("evaluation_interval");

    paths.add("signal_source");
    if (!SignalSourceTypeView.MANUAL.equals(view.signalSourceType())) {
      builder.setSignalSource(toSignalSource(view));
    }

    if (view.criticalThreshold() != null) {
      builder.setCriticalThreshold(view.criticalThreshold());
    }
    paths.add("critical_threshold");
    if (view.minFallbackRatio() != null) {
      builder.setMinFallbackRatio(view.minFallbackRatio());
    }
    paths.add("min_fallback_ratio");
    if (view.maxFallbackRatio() != null) {
      builder.setMaxFallbackRatio(view.maxFallbackRatio());
    }
    paths.add("max_fallback_ratio");
    if (view.fallbackCurveExponent() != null) {
      builder.setFallbackCurveExponent(view.fallbackCurveExponent());
    }
    paths.add("fallback_curve_exponent");

    builder.setUpdateMask(FieldMask.newBuilder().addAllPaths(paths).build());
    return builder.build();
  }

  private SignalSource toSignalSource(PolicyView view) {
    var builder = SignalSource.newBuilder();
    return switch (view.signalSourceType()) {
      case PROMETHEUS ->
          builder
              .setPrometheus(
                  org.janus.api.policystore.PrometheusMetric.newBuilder()
                      .setQuery(view.query())
                      .build())
              .build();
      case MANUAL -> throw new IllegalArgumentException("Manual policy has no signal source");
    };
  }
}
