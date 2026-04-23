package org.janus.policystore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.PrometheusMetric;
import org.janus.api.policystore.SignalSource;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.entity.SignalSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DegradationPolicyMapperTest {

  private DegradationPolicyMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new DegradationPolicyMapper();
  }

  private DegradationPolicy fullEntity() {
    var entity = new DegradationPolicy();
    entity.setDegradationId("test");
    entity.setEvaluationIntervalMs(30000L);
    entity.setSignalSourceType(SignalSourceType.PROMETHEUS);
    entity.setSourcePrometheusQuery("up{job=\"test\"}");
    entity.setCriticalThreshold(0.7);
    entity.setMinFallbackRatio(0.1);
    entity.setMaxFallbackRatio(0.9);
    entity.setFallbackCurveExponent(2.0);
    return entity;
  }

  private DegradationPolicy minimalEntity() {
    var entity = new DegradationPolicy();
    entity.setDegradationId("minimal");
    entity.setEvaluationIntervalMs(10000L);
    return entity;
  }

  // --- fromEntityToProto ---

  @Test
  void fromEntityToProto_allFieldsSet() {
    var entity = fullEntity();

    var proto = mapper.fromEntityToProto(entity);

    assertThat(proto.getDegradationId()).isEqualTo("test");
    assertThat(proto.getEvaluationInterval().getSeconds()).isEqualTo(30);
    assertThat(proto.hasSignalSource()).isTrue();
    assertThat(proto.getSignalSource().getPrometheus().getQuery())
        .isEqualTo("up{job=\"test\"}");
    assertThat(proto.hasCriticalThreshold()).isTrue();
    assertThat(proto.getCriticalThreshold()).isEqualTo(0.7);
    assertThat(proto.hasMinFallbackRatio()).isTrue();
    assertThat(proto.getMinFallbackRatio()).isEqualTo(0.1);
    assertThat(proto.hasMaxFallbackRatio()).isTrue();
    assertThat(proto.getMaxFallbackRatio()).isEqualTo(0.9);
    assertThat(proto.hasFallbackCurveExponent()).isTrue();
    assertThat(proto.getFallbackCurveExponent()).isEqualTo(2.0);
  }

  @Test
  void fromEntityToProto_optionalFieldsNull() {
    var entity = minimalEntity();

    var proto = mapper.fromEntityToProto(entity);

    assertThat(proto.getDegradationId()).isEqualTo("minimal");
    assertThat(proto.getEvaluationInterval().getSeconds()).isEqualTo(10);
    assertThat(proto.hasSignalSource()).isFalse();
    assertThat(proto.hasCriticalThreshold()).isFalse();
    assertThat(proto.hasMinFallbackRatio()).isFalse();
    assertThat(proto.hasMaxFallbackRatio()).isFalse();
    assertThat(proto.hasFallbackCurveExponent()).isFalse();
  }

  // --- fromEntityToEvaluatorProto ---

  @Test
  void fromEntityToEvaluatorProto_mapsOnlyRelevantFields() {
    var entity = fullEntity();

    var proto = mapper.fromEntityToEvaluatorProto(entity);

    assertThat(proto.getDegradationId()).isEqualTo("test");
    assertThat(proto.getEvaluationInterval().getSeconds()).isEqualTo(30);
    assertThat(proto.hasSignalSource()).isTrue();
    assertThat(proto.getSignalSource().getPrometheus().getQuery())
        .isEqualTo("up{job=\"test\"}");
  }

  // --- fromEntityToSidecarProto ---

  @Test
  void fromEntityToSidecarProto_hasThresholdsNoSignalSource() {
    var entity = fullEntity();

    var proto = mapper.fromEntityToSidecarProto(entity);

    assertThat(proto.getDegradationId()).isEqualTo("test");
    assertThat(proto.getEvaluationInterval().getSeconds()).isEqualTo(30);
    assertThat(proto.hasCriticalThreshold()).isTrue();
    assertThat(proto.getCriticalThreshold()).isEqualTo(0.7);
    assertThat(proto.hasMinFallbackRatio()).isTrue();
    assertThat(proto.getMinFallbackRatio()).isEqualTo(0.1);
    assertThat(proto.hasMaxFallbackRatio()).isTrue();
    assertThat(proto.getMaxFallbackRatio()).isEqualTo(0.9);
    assertThat(proto.hasFallbackCurveExponent()).isTrue();
    assertThat(proto.getFallbackCurveExponent()).isEqualTo(2.0);
  }

  // --- fromCreateRequestProtoToEntity ---

  @Test
  void fromCreateRequestProtoToEntity_allFieldsMapped() {
    var request =
        CreateDegradationPolicyRequest.newBuilder()
            .setDegradationId("new-policy")
            .setEvaluationInterval(Duration.newBuilder().setSeconds(60).build())
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(
                        PrometheusMetric.newBuilder()
                            .setQuery("http_requests_total")
                            .build())
                    .build())
            .setCriticalThreshold(0.8)
            .setMinFallbackRatio(0.2)
            .setMaxFallbackRatio(0.95)
            .setFallbackCurveExponent(1.5)
            .build();

    var entity = mapper.fromCreateRequestProtoToEntity(request);

    assertThat(entity.getDegradationId()).isEqualTo("new-policy");
    assertThat(entity.getEvaluationIntervalMs()).isEqualTo(60000L);
    assertThat(entity.getSignalSourceType()).isEqualTo(SignalSourceType.PROMETHEUS);
    assertThat(entity.getSourcePrometheusQuery()).isEqualTo("http_requests_total");
    assertThat(entity.getCriticalThreshold()).isEqualTo(0.8);
    assertThat(entity.getMinFallbackRatio()).isEqualTo(0.2);
    assertThat(entity.getMaxFallbackRatio()).isEqualTo(0.95);
    assertThat(entity.getFallbackCurveExponent()).isEqualTo(1.5);
  }

  @Test
  void fromCreateRequestProtoToEntity_optionalFieldsAbsent() {
    var request =
        CreateDegradationPolicyRequest.newBuilder()
            .setDegradationId("bare-policy")
            .setEvaluationInterval(Duration.newBuilder().setSeconds(15).build())
            .build();

    var entity = mapper.fromCreateRequestProtoToEntity(request);

    assertThat(entity.getDegradationId()).isEqualTo("bare-policy");
    assertThat(entity.getEvaluationIntervalMs()).isEqualTo(15000L);
    assertThat(entity.getSignalSourceType()).isNull();
    assertThat(entity.getSourcePrometheusQuery()).isNull();
    assertThat(entity.getCriticalThreshold()).isNull();
    assertThat(entity.getMinFallbackRatio()).isNull();
    assertThat(entity.getMaxFallbackRatio()).isNull();
    assertThat(entity.getFallbackCurveExponent()).isNull();
  }

  // --- updateEntityFromUpdateRequestProto ---

  @Test
  void updateEntityFromUpdateRequestProto_fieldInMaskAndValuePresent_updated() {
    var entity = fullEntity();

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("test")
            .setCriticalThreshold(0.5)
            .setMinFallbackRatio(0.3)
            .setUpdateMask(
                FieldMask.newBuilder()
                    .addPaths("critical_threshold")
                    .addPaths("min_fallback_ratio")
                    .build())
            .build();

    mapper.updateEntityFromUpdateRequestProto(entity, request);

    assertThat(entity.getCriticalThreshold()).isEqualTo(0.5);
    assertThat(entity.getMinFallbackRatio()).isEqualTo(0.3);
    // unchanged fields
    assertThat(entity.getMaxFallbackRatio()).isEqualTo(0.9);
    assertThat(entity.getFallbackCurveExponent()).isEqualTo(2.0);
  }

  @Test
  void updateEntityFromUpdateRequestProto_fieldInMaskValueAbsent_setToNull() {
    var entity = fullEntity();

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("test")
            .setUpdateMask(
                FieldMask.newBuilder()
                    .addPaths("critical_threshold")
                    .addPaths("max_fallback_ratio")
                    .build())
            .build();

    mapper.updateEntityFromUpdateRequestProto(entity, request);

    assertThat(entity.getCriticalThreshold()).isNull();
    assertThat(entity.getMaxFallbackRatio()).isNull();
    // unchanged fields
    assertThat(entity.getMinFallbackRatio()).isEqualTo(0.1);
    assertThat(entity.getFallbackCurveExponent()).isEqualTo(2.0);
  }

  @Test
  void updateEntityFromUpdateRequestProto_signalSourceInMaskAbsent_cleared() {
    var entity = fullEntity();

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("test")
            .setUpdateMask(FieldMask.newBuilder().addPaths("signal_source").build())
            .build();

    mapper.updateEntityFromUpdateRequestProto(entity, request);

    assertThat(entity.getSignalSourceType()).isNull();
    assertThat(entity.getSourcePrometheusQuery()).isNull();
  }

  @Test
  void updateEntityFromUpdateRequestProto_valuePresentButFieldNotInMask_unchanged() {
    var entity = fullEntity();

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("test")
            .setEvaluationInterval(Duration.newBuilder().setSeconds(99).build())
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(PrometheusMetric.newBuilder().setQuery("new_metric").build())
                    .build())
            .setUpdateMask(FieldMask.newBuilder().addPaths("critical_threshold").build())
            .build();

    mapper.updateEntityFromUpdateRequestProto(entity, request);

    assertThat(entity.getEvaluationIntervalMs()).isEqualTo(30000L);
    assertThat(entity.getSignalSourceType()).isEqualTo(SignalSourceType.PROMETHEUS);
    assertThat(entity.getSourcePrometheusQuery()).isEqualTo("up{job=\"test\"}");
  }

  @Test
  void updateEntityFromUpdateRequestProto_evaluationIntervalInMaskAbsent_setToNull() {
    var entity = fullEntity();

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("test")
            .setUpdateMask(FieldMask.newBuilder().addPaths("evaluation_interval").build())
            .build();

    mapper.updateEntityFromUpdateRequestProto(entity, request);

    assertThat(entity.getEvaluationIntervalMs()).isNull();
  }

  // --- signal source mapping ---

  @Test
  void signalSourceMapping_prometheus() {
    var entity = new DegradationPolicy();
    entity.setDegradationId("prom-test");
    entity.setEvaluationIntervalMs(5000L);
    entity.setSignalSourceType(SignalSourceType.PROMETHEUS);
    entity.setSourcePrometheusQuery("process_cpu_seconds_total");

    var proto = mapper.fromEntityToProto(entity);

    assertThat(proto.getSignalSource().getKindCase()).isEqualTo(SignalSource.KindCase.PROMETHEUS);
    assertThat(proto.getSignalSource().getPrometheus().getQuery())
        .isEqualTo("process_cpu_seconds_total");
  }
}
