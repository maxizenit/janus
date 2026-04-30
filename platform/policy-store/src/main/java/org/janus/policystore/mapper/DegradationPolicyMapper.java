package org.janus.policystore.mapper;

import com.google.protobuf.util.Durations;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.DegradationPolicy;
import org.janus.api.policystore.EvaluatorDegradationPolicy;
import org.janus.api.policystore.PrometheusMetric;
import org.janus.api.policystore.SidecarDegradationPolicy;
import org.janus.api.policystore.SignalSource;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.entity.SignalSourceType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NullMarked
public class DegradationPolicyMapper {

  public DegradationPolicy fromEntityToProto(
      org.janus.policystore.entity.DegradationPolicy entity) {
    var builder = DegradationPolicy.newBuilder();

    builder.setDegradationId(entity.getDegradationId());
    builder.setEvaluationInterval(Durations.fromMillis(entity.getEvaluationIntervalMs()));
    if (entity.getSignalSourceType() != null) {
      builder.setSignalSource(extractSignalSource(entity));
    }

    builder.setCriticalThreshold(required(entity.getCriticalThreshold(), "criticalThreshold"));
    builder.setMinFallbackRatio(required(entity.getMinFallbackRatio(), "minFallbackRatio"));
    builder.setMaxFallbackRatio(required(entity.getMaxFallbackRatio(), "maxFallbackRatio"));
    builder.setFallbackCurveExponent(
        required(entity.getFallbackCurveExponent(), "fallbackCurveExponent"));

    return builder.build();
  }

  public EvaluatorDegradationPolicy fromEntityToEvaluatorProto(
      org.janus.policystore.entity.DegradationPolicy entity) {
    var builder = EvaluatorDegradationPolicy.newBuilder();

    builder.setDegradationId(entity.getDegradationId());
    builder.setEvaluationInterval(Durations.fromMillis(entity.getEvaluationIntervalMs()));
    if (entity.getSignalSourceType() != null) {
      builder.setSignalSource(extractSignalSource(entity));
    }

    return builder.build();
  }

  public SidecarDegradationPolicy fromEntityToSidecarProto(
      org.janus.policystore.entity.DegradationPolicy entity) {
    var builder = SidecarDegradationPolicy.newBuilder();

    builder.setDegradationId(entity.getDegradationId());
    builder.setEvaluationInterval(Durations.fromMillis(entity.getEvaluationIntervalMs()));
    builder.setCriticalThreshold(required(entity.getCriticalThreshold(), "criticalThreshold"));
    builder.setMinFallbackRatio(required(entity.getMinFallbackRatio(), "minFallbackRatio"));
    builder.setMaxFallbackRatio(required(entity.getMaxFallbackRatio(), "maxFallbackRatio"));
    builder.setFallbackCurveExponent(
        required(entity.getFallbackCurveExponent(), "fallbackCurveExponent"));

    return builder.build();
  }

  public org.janus.policystore.entity.DegradationPolicy fromCreateRequestProtoToEntity(
      CreateDegradationPolicyRequest createRequestProto) {
    var policy = new org.janus.policystore.entity.DegradationPolicy();

    policy.setDegradationId(createRequestProto.getDegradationId());
    policy.setEvaluationIntervalMs(Durations.toMillis(createRequestProto.getEvaluationInterval()));

    if (createRequestProto.hasSignalSource()) {
      enrichEntityOfSignalSource(policy, createRequestProto.getSignalSource());
    }

    if (createRequestProto.hasCriticalThreshold()) {
      policy.setCriticalThreshold(createRequestProto.getCriticalThreshold());
    }
    if (createRequestProto.hasMinFallbackRatio()) {
      policy.setMinFallbackRatio(createRequestProto.getMinFallbackRatio());
    }
    if (createRequestProto.hasMaxFallbackRatio()) {
      policy.setMaxFallbackRatio(createRequestProto.getMaxFallbackRatio());
    }
    if (createRequestProto.hasFallbackCurveExponent()) {
      policy.setFallbackCurveExponent(createRequestProto.getFallbackCurveExponent());
    }

    return policy;
  }

  public void updateEntityFromUpdateRequestProto(
      org.janus.policystore.entity.DegradationPolicy entity,
      UpdateDegradationPolicyRequest updateRequestProto) {

    for (String path : updateRequestProto.getUpdateMask().getPathsList()) {
      switch (path) {
        case "evaluation_interval" -> {
          if (updateRequestProto.hasEvaluationInterval()) {
            entity.setEvaluationIntervalMs(
                Durations.toMillis(updateRequestProto.getEvaluationInterval()));
          } else {
            entity.setEvaluationIntervalMs(null);
          }
        }
        case "signal_source" -> {
          if (updateRequestProto.hasSignalSource()) {
            enrichEntityOfSignalSource(entity, updateRequestProto.getSignalSource());
          } else {
            clearSignalSource(entity);
          }
        }
        case "critical_threshold" -> {
          if (updateRequestProto.hasCriticalThreshold()) {
            entity.setCriticalThreshold(updateRequestProto.getCriticalThreshold());
          } else {
            entity.setCriticalThreshold(null);
          }
        }
        case "min_fallback_ratio" -> {
          if (updateRequestProto.hasMinFallbackRatio()) {
            entity.setMinFallbackRatio(updateRequestProto.getMinFallbackRatio());
          } else {
            entity.setMinFallbackRatio(null);
          }
        }
        case "max_fallback_ratio" -> {
          if (updateRequestProto.hasMaxFallbackRatio()) {
            entity.setMaxFallbackRatio(updateRequestProto.getMaxFallbackRatio());
          } else {
            entity.setMaxFallbackRatio(null);
          }
        }
        case "fallback_curve_exponent" -> {
          if (updateRequestProto.hasFallbackCurveExponent()) {
            entity.setFallbackCurveExponent(updateRequestProto.getFallbackCurveExponent());
          } else {
            entity.setFallbackCurveExponent(null);
          }
        }
        default -> throw new IllegalArgumentException("Unsupported update mask path: " + path);
      }
    }
  }

  private SignalSource extractSignalSource(org.janus.policystore.entity.DegradationPolicy entity) {
    var builder = SignalSource.newBuilder();

    if (SignalSourceType.PROMETHEUS.equals(entity.getSignalSourceType())) {
      builder.setPrometheus(
          PrometheusMetric.newBuilder()
              .setQuery(entity.getSourcePrometheusQuery())
              .build());
    } else {
      log.error(
          "Unsupported signal source type while mapping policy: degradationId={}, signalSourceType={}",
          entity.getDegradationId(),
          entity.getSignalSourceType());
      throw new IllegalArgumentException(
          "Unsupported signal source type: " + entity.getSignalSourceType());
    }

    return builder.build();
  }

  private void enrichEntityOfSignalSource(
      org.janus.policystore.entity.DegradationPolicy entity, SignalSource signalSource) {
    switch (signalSource.getKindCase()) {
      case SignalSource.KindCase.PROMETHEUS -> {
        entity.setSignalSourceType(SignalSourceType.PROMETHEUS);
        entity.setSourcePrometheusQuery(
            signalSource.getPrometheus().getQuery());
      }
      default -> {
        log.warn(
            "Signal source kind is not set while enriching policy entity: degradationId={}",
            entity.getDegradationId());
        throw new IllegalArgumentException("Signal source kind must be set");
      }
    }
  }

  private void clearSignalSource(org.janus.policystore.entity.DegradationPolicy entity) {
    entity.setSignalSourceType(null);
    entity.setSourcePrometheusQuery(null);
  }

  private static double required(@Nullable Double value, String field) {
    return Objects.requireNonNull(value, field + " must be set");
  }
}
