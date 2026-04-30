package org.janus.policystore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import java.util.List;
import java.util.Optional;
import org.janus.api.policystore.CreateDegradationPolicyRequest;
import org.janus.api.policystore.PrometheusMetric;
import org.janus.api.policystore.SignalSource;
import org.janus.api.policystore.UpdateDegradationPolicyRequest;
import org.janus.policystore.entity.DegradationPolicy;
import org.janus.policystore.entity.SignalSourceType;
import org.janus.policystore.exception.PolicyNotFoundException;
import org.janus.policystore.mapper.DegradationPolicyMapper;
import org.janus.policystore.repository.DegradationPolicyRepository;
import org.janus.policystore.validation.DegradationPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradationPolicyServiceTest {

  @Mock private DegradationPolicyRepository policyRepository;
  @Mock private DegradationPolicyValidator policyValidator;
  @Mock private DegradationPolicyMapper policyMapper;

  private DegradationPolicyService service;

  @BeforeEach
  void setUp() {
    service = new DegradationPolicyService(policyRepository, policyValidator, policyMapper);
  }

  private DegradationPolicy sampleEntity(String id) {
    var entity = new DegradationPolicy();
    entity.setDegradationId(id);
    entity.setEvaluationIntervalMs(30000L);
    entity.setSignalSourceType(SignalSourceType.PROMETHEUS);
    entity.setSourcePrometheusQuery("up{job=\"test\"}");
    entity.setCriticalThreshold(0.7);
    entity.setMinFallbackRatio(0.1);
    entity.setMaxFallbackRatio(0.9);
    return entity;
  }

  // --- getAllPolicies ---

  @Test
  void getAllPolicies_returnsList() {
    var policies = List.of(sampleEntity("a"), sampleEntity("b"));
    when(policyRepository.findAll()).thenReturn(policies);

    var result = service.getAllPolicies();

    assertThat(result).hasSize(2);
    verify(policyRepository).findAll();
  }

  @Test
  void getAllPolicies_emptyList() {
    when(policyRepository.findAll()).thenReturn(List.of());

    var result = service.getAllPolicies();

    assertThat(result).isEmpty();
  }

  // --- getPoliciesByDegradationIds ---

  @Test
  void getPoliciesByDegradationIds_returnsMatchingPolicies() {
    var ids = List.of("a", "b");
    var policies = List.of(sampleEntity("a"), sampleEntity("b"));
    when(policyRepository.findAllById(ids)).thenReturn(policies);

    var result = service.getPoliciesByDegradationIds(ids);

    assertThat(result).hasSize(2);
    verify(policyRepository).findAllById(ids);
  }

  @Test
  void getPoliciesByDegradationIds_emptyIds_returnsEmpty() {
    var result = service.getPoliciesByDegradationIds(List.of());

    assertThat(result).isEmpty();
  }

  // --- createPolicy ---

  @Test
  void createPolicy_delegatesToMapperAndRepository() {
    var request =
        CreateDegradationPolicyRequest.newBuilder()
            .setDegradationId("new")
            .setEvaluationInterval(Duration.newBuilder().setSeconds(60).build())
            .setSignalSource(
                SignalSource.newBuilder()
                    .setPrometheus(
                        PrometheusMetric.newBuilder()
                            .setQuery("metric")
                            .build())
                    .build())
            .build();

    var entity = sampleEntity("new");
    when(policyMapper.fromCreateRequestProtoToEntity(request)).thenReturn(entity);
    when(policyRepository.save(entity)).thenReturn(entity);

    var result = service.createPolicy(request);

    assertThat(result.getDegradationId()).isEqualTo("new");
    verify(policyMapper).fromCreateRequestProtoToEntity(request);
    verify(policyValidator).validateForCreate(entity);
    verify(policyRepository).save(entity);
  }

  // --- updatePolicy ---

  @Test
  void updatePolicy_fetchesAppliesAndSaves() {
    var existing = sampleEntity("upd");
    when(policyRepository.findById("upd")).thenReturn(Optional.of(existing));
    when(policyRepository.save(existing)).thenReturn(existing);

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("upd")
            .setCriticalThreshold(0.5)
            .setUpdateMask(FieldMask.newBuilder().addPaths("critical_threshold").build())
            .build();

    var result = service.updatePolicy(request);

    assertThat(result).isSameAs(existing);
    verify(policyRepository).findById("upd");
    verify(policyMapper).updateEntityFromUpdateRequestProto(existing, request);
    verify(policyValidator).validateForUpdate(existing);
    verify(policyRepository).save(existing);
  }

  @Test
  void updatePolicy_notFound_throws() {
    when(policyRepository.findById("missing")).thenReturn(Optional.empty());

    var request =
        UpdateDegradationPolicyRequest.newBuilder()
            .setDegradationId("missing")
            .setUpdateMask(FieldMask.newBuilder().build())
            .build();

    assertThatThrownBy(() -> service.updatePolicy(request))
        .isInstanceOf(PolicyNotFoundException.class)
        .hasMessageContaining("missing");
  }

  // --- deletePolicy ---

  @Test
  void deletePolicyByDegradationId_delegatesToValidatorAndRepository() {
    service.deletePolicyByDegradationId("del");

    verify(policyValidator).validateForDelete("del");
    verify(policyRepository).deleteById("del");
  }
}
