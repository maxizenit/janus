package org.janus.statestore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.AdminDegradationState;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DegradationStateServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ObjectMapper objectMapper;
  @Mock private DegradationStateUpdateMapper stateUpdateMapper;
  @Mock private ValueOperations<String, String> valueOperations;

  private DegradationStateService service;

  @BeforeEach
  void setUp() {
    service = new DegradationStateService(redisTemplate, objectMapper, stateUpdateMapper);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Nested
  class UpdateDegradationStates {

    @SuppressWarnings("unchecked")
    @Test
    void storesValuesWithCorrectKeysAndTtl() {
      DegradationStateUpdate update =
          new DegradationStateUpdate(
              "deg-1", 0.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ofSeconds(300));
      DegradationState state = new DegradationState("deg-1", 0.5);

      when(stateUpdateMapper.fromUpdateToState(update)).thenReturn(state);
      when(objectMapper.writeValueAsString(state)).thenReturn("{\"degradationId\":\"deg-1\",\"value\":0.5}");
      when(redisTemplate.execute(any(SessionCallback.class)))
          .thenAnswer(
              invocation -> {
                SessionCallback<?> callback = invocation.getArgument(0);
                return callback.execute(redisTemplate);
              });
      when(redisTemplate.exec()).thenReturn(Collections.emptyList());

      service.updateDegradationStates(List.of(update));

      verify(redisTemplate).multi();
      verify(valueOperations)
          .set(
              eq("janus:state:deg-1:admin_ui"),
              eq("{\"degradationId\":\"deg-1\",\"value\":0.5}"),
              eq(Duration.ofSeconds(300)));
      verify(redisTemplate).exec();
    }

    @SuppressWarnings("unchecked")
    @Test
    void storesMultipleUpdatesInSameTransaction() {
      DegradationStateUpdate update1 =
          new DegradationStateUpdate(
              "deg-1", 0.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ofSeconds(300));
      DegradationStateUpdate update2 =
          new DegradationStateUpdate(
              "deg-2", 0.8, DegradationStateUpdateSource.EVALUATOR, Duration.ofSeconds(60));
      DegradationState state1 = new DegradationState("deg-1", 0.5);
      DegradationState state2 = new DegradationState("deg-2", 0.8);

      when(stateUpdateMapper.fromUpdateToState(update1)).thenReturn(state1);
      when(stateUpdateMapper.fromUpdateToState(update2)).thenReturn(state2);
      when(objectMapper.writeValueAsString(state1)).thenReturn("{\"v\":1}");
      when(objectMapper.writeValueAsString(state2)).thenReturn("{\"v\":2}");
      when(redisTemplate.execute(any(SessionCallback.class)))
          .thenAnswer(
              invocation -> {
                SessionCallback<?> callback = invocation.getArgument(0);
                return callback.execute(redisTemplate);
              });
      when(redisTemplate.exec()).thenReturn(Collections.emptyList());

      service.updateDegradationStates(List.of(update1, update2));

      verify(redisTemplate).multi();
      verify(valueOperations)
          .set(eq("janus:state:deg-1:admin_ui"), eq("{\"v\":1}"), eq(Duration.ofSeconds(300)));
      verify(valueOperations)
          .set(eq("janus:state:deg-2:evaluator"), eq("{\"v\":2}"), eq(Duration.ofSeconds(60)));
      verify(redisTemplate).exec();
    }

    @Test
    void emptyUpdates_skips() {
      service.updateDegradationStates(Collections.emptyList());

      verify(redisTemplate, never()).execute(any(SessionCallback.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidTtl_throws() {
      DegradationStateUpdate update =
          new DegradationStateUpdate(
              "deg-1", 0.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ZERO);

      when(redisTemplate.execute(any(SessionCallback.class)))
          .thenAnswer(
              invocation -> {
                SessionCallback<?> callback = invocation.getArgument(0);
                return callback.execute(redisTemplate);
              });

      assertThatThrownBy(() -> service.updateDegradationStates(List.of(update)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("TTL must be positive");
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidValue_throws() {
      DegradationStateUpdate update =
          new DegradationStateUpdate(
              "deg-1", 1.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ofSeconds(60));

      when(redisTemplate.execute(any(SessionCallback.class)))
          .thenAnswer(
              invocation -> {
                SessionCallback<?> callback = invocation.getArgument(0);
                return callback.execute(redisTemplate);
              });

      assertThatThrownBy(() -> service.updateDegradationStates(List.of(update)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Value must be in range [0.0, 1.0]");
    }

    @SuppressWarnings("unchecked")
    @Test
    void serializationFailure_discardsTransaction() {
      DegradationStateUpdate update =
          new DegradationStateUpdate(
              "deg-1", 0.5, DegradationStateUpdateSource.ADMIN_UI, Duration.ofSeconds(300));
      DegradationState state = new DegradationState("deg-1", 0.5);

      when(stateUpdateMapper.fromUpdateToState(update)).thenReturn(state);
      when(objectMapper.writeValueAsString(state)).thenThrow(new RuntimeException("serialization error"));
      when(redisTemplate.execute(any(SessionCallback.class)))
          .thenAnswer(
              invocation -> {
                SessionCallback<?> callback = invocation.getArgument(0);
                return callback.execute(redisTemplate);
              });

      assertThatThrownBy(() -> service.updateDegradationStates(List.of(update)))
          .isInstanceOf(RuntimeException.class);

      verify(redisTemplate).discard();
    }
  }

  @Nested
  class GetDegradationStates {

    @Test
    void returnsEffectiveValuesWithSourcePriority_adminUiOverEvaluator() {
      String adminJson = "{\"degradationId\":\"deg-1\",\"value\":0.3}";
      String evaluatorJson = "{\"degradationId\":\"deg-1\",\"value\":0.8}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(List.of(adminJson, evaluatorJson));

      when(objectMapper.readValue(adminJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.3));
      when(objectMapper.readValue(evaluatorJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.8));

      when(redisTemplate.getExpire("janus:state:deg-1:admin_ui", TimeUnit.MILLISECONDS))
          .thenReturn(60000L);
      when(redisTemplate.getExpire("janus:state:deg-1:evaluator", TimeUnit.MILLISECONDS))
          .thenReturn(30000L);

      List<DegradationState> result = service.getDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().degradationId()).isEqualTo("deg-1");
      assertThat(result.getFirst().value()).isEqualTo(0.3);
    }

    @Test
    void returnsEvaluatorWhenAdminUiAbsent() {
      String evaluatorJson = "{\"degradationId\":\"deg-1\",\"value\":0.8}";

      // admin_ui key is null, evaluator key has value
      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(null, evaluatorJson));

      when(objectMapper.readValue(evaluatorJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.8));

      when(redisTemplate.getExpire("janus:state:deg-1:evaluator", TimeUnit.MILLISECONDS))
          .thenReturn(30000L);

      List<DegradationState> result = service.getDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().value()).isEqualTo(0.8);
    }

    @Test
    void emptyDegradationIds_returnsEmptyList() {
      List<DegradationState> result = service.getDegradationStates(Collections.emptyList());

      assertThat(result).isEmpty();
      verify(valueOperations, never()).multiGet(anyList());
    }

    @Test
    void noDataInRedis_returnsEmptyList() {
      when(valueOperations.multiGet(anyList())).thenReturn(null);

      List<DegradationState> result = service.getDegradationStates(List.of("deg-1"));

      assertThat(result).isEmpty();
    }

    @Test
    void allKeysNull_returnsEmptyList() {
      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(null, null));

      List<DegradationState> result = service.getDegradationStates(List.of("deg-1"));

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class ClearDegradationStates {

    @Test
    void deletesCorrectKeys() {
      List<String> degradationIds = List.of("deg-1", "deg-2");

      when(redisTemplate.delete(anyList())).thenReturn(2L);

      service.clearDegradationStates(degradationIds, DegradationStateUpdateSource.ADMIN_UI);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
      verify(redisTemplate).delete(keysCaptor.capture());

      List<String> deletedKeys = keysCaptor.getValue();
      assertThat(deletedKeys)
          .containsExactly("janus:state:deg-1:admin_ui", "janus:state:deg-2:admin_ui");
    }

    @Test
    void deletesCorrectKeysForEvaluator() {
      List<String> degradationIds = List.of("deg-1");

      when(redisTemplate.delete(anyList())).thenReturn(1L);

      service.clearDegradationStates(degradationIds, DegradationStateUpdateSource.EVALUATOR);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
      verify(redisTemplate).delete(keysCaptor.capture());

      assertThat(keysCaptor.getValue()).containsExactly("janus:state:deg-1:evaluator");
    }

    @Test
    void emptyDegradationIds_skips() {
      service.clearDegradationStates(
          Collections.emptyList(), DegradationStateUpdateSource.ADMIN_UI);

      verify(redisTemplate, never()).delete(anyList());
    }
  }

  @Nested
  class GetAdminDegradationStates {

    @Test
    void returnsFullResolutionDetails() {
      String adminJson = "{\"degradationId\":\"deg-1\",\"value\":0.3}";
      String evaluatorJson = "{\"degradationId\":\"deg-1\",\"value\":0.8}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(List.of(adminJson, evaluatorJson));

      when(objectMapper.readValue(adminJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.3));
      when(objectMapper.readValue(evaluatorJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.8));

      when(redisTemplate.getExpire("janus:state:deg-1:admin_ui", TimeUnit.MILLISECONDS))
          .thenReturn(120000L);
      when(redisTemplate.getExpire("janus:state:deg-1:evaluator", TimeUnit.MILLISECONDS))
          .thenReturn(60000L);

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      AdminDegradationState adminState = result.getFirst();

      assertThat(adminState.degradationId()).isEqualTo("deg-1");
      assertThat(adminState.effectiveState().value()).isEqualTo(0.3);
      assertThat(adminState.effectiveState().source())
          .isEqualTo(DegradationStateUpdateSource.ADMIN_UI);

      assertThat(adminState.sourceStates()).hasSize(2);
      assertThat(adminState.sourceStates().get(0).source())
          .isEqualTo(DegradationStateUpdateSource.ADMIN_UI);
      assertThat(adminState.sourceStates().get(0).value()).isEqualTo(0.3);
      assertThat(adminState.sourceStates().get(0).remainingTtl())
          .isEqualTo(Duration.ofMillis(120000));

      assertThat(adminState.sourceStates().get(1).source())
          .isEqualTo(DegradationStateUpdateSource.EVALUATOR);
      assertThat(adminState.sourceStates().get(1).value()).isEqualTo(0.8);
      assertThat(adminState.sourceStates().get(1).remainingTtl())
          .isEqualTo(Duration.ofMillis(60000));
    }

    @Test
    void onlyEvaluatorPresent_effectiveStateIsEvaluator() {
      String evaluatorJson = "{\"degradationId\":\"deg-1\",\"value\":0.7}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(null, evaluatorJson));

      when(objectMapper.readValue(evaluatorJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.7));

      when(redisTemplate.getExpire("janus:state:deg-1:evaluator", TimeUnit.MILLISECONDS))
          .thenReturn(45000L);

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().effectiveState().source())
          .isEqualTo(DegradationStateUpdateSource.EVALUATOR);
      assertThat(result.getFirst().sourceStates()).hasSize(1);
    }

    @Test
    void noSourceStates_excludesDegradationFromResult() {
      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(null, null));

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1"));

      assertThat(result).isEmpty();
    }

    @Test
    void emptyDegradationIds_returnsEmptyList() {
      List<AdminDegradationState> result =
          service.getAdminDegradationStates(Collections.emptyList());

      assertThat(result).isEmpty();
      verify(valueOperations, never()).multiGet(anyList());
    }

    @Test
    void multipleDegradations_returnsOnlyThoseWithState() {
      // deg-1 has admin_ui, deg-2 has nothing
      String adminJson = "{\"degradationId\":\"deg-1\",\"value\":0.5}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(adminJson, null, null, null));

      when(objectMapper.readValue(adminJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.5));

      when(redisTemplate.getExpire("janus:state:deg-1:admin_ui", TimeUnit.MILLISECONDS))
          .thenReturn(100000L);

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1", "deg-2"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().degradationId()).isEqualTo("deg-1");
    }

    @Test
    void ttlReturnsNull_usesZeroDuration() {
      String adminJson = "{\"degradationId\":\"deg-1\",\"value\":0.5}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(adminJson, null));

      when(objectMapper.readValue(adminJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.5));

      when(redisTemplate.getExpire("janus:state:deg-1:admin_ui", TimeUnit.MILLISECONDS))
          .thenReturn(null);

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().sourceStates().getFirst().remainingTtl())
          .isEqualTo(Duration.ZERO);
    }

    @Test
    void ttlReturnsMinusTwo_keyDisappeared_usesZeroDuration() {
      String adminJson = "{\"degradationId\":\"deg-1\",\"value\":0.5}";

      when(valueOperations.multiGet(anyList()))
          .thenReturn(java.util.Arrays.asList(adminJson, null));

      when(objectMapper.readValue(adminJson, DegradationState.class))
          .thenReturn(new DegradationState("deg-1", 0.5));

      when(redisTemplate.getExpire("janus:state:deg-1:admin_ui", TimeUnit.MILLISECONDS))
          .thenReturn(-2L);

      List<AdminDegradationState> result =
          service.getAdminDegradationStates(List.of("deg-1"));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().sourceStates().getFirst().remainingTtl())
          .isEqualTo(Duration.ZERO);
    }
  }
}
