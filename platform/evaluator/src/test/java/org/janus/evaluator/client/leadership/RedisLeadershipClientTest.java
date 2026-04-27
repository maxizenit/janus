package org.janus.evaluator.client.leadership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.janus.evaluator.configuration.properties.EvaluatorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class RedisLeadershipClientTest {

  private static final String DEGRADATION_ID = "deg-1";
  private static final String INSTANCE_ID = "instance-1";
  private static final Duration LEASE_DURATION = Duration.ofSeconds(45);
  private static final String KEY = "janus:evaluator:leader:" + DEGRADATION_ID;

  @Mock private StringRedisTemplate redisTemplate;

  private RedisLeadershipClient client;

  @BeforeEach
  void setUp() {
    client =
        new RedisLeadershipClient(
            redisTemplate,
            new EvaluatorProperties(
                INSTANCE_ID,
                Duration.ofMinutes(1),
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                2,
                100));
  }

  @Test
  void tryAcquire_whenLeaseGranted_returnsAcquiredHandle() {
    when(redisTemplate.execute(
            anyScript(),
            eq(List.of(KEY)),
            eq(INSTANCE_ID),
            eq(String.valueOf(LEASE_DURATION.toMillis()))))
        .thenReturn(1L);

    var handle = client.tryAcquire(DEGRADATION_ID, LEASE_DURATION);

    assertThat(handle.acquired()).isTrue();
    verify(redisTemplate)
        .execute(
            anyScript(),
            eq(List.of(KEY)),
            eq(INSTANCE_ID),
            eq(String.valueOf(LEASE_DURATION.toMillis())));
  }

  @Test
  void tryAcquire_whenLeaseRejected_returnsRejectedHandleWithoutRelease() {
    when(redisTemplate.execute(
            anyScript(),
            eq(List.of(KEY)),
            eq(INSTANCE_ID),
            eq(String.valueOf(LEASE_DURATION.toMillis()))))
        .thenReturn(0L);

    var handle = client.tryAcquire(DEGRADATION_ID, LEASE_DURATION);
    handle.release();

    assertThat(handle.acquired()).isFalse();
    verify(redisTemplate, never()).execute(anyScript(), eq(List.of(KEY)), eq(INSTANCE_ID));
  }

  @Test
  void release_whenLeaseGrantedDeletesOnlyOwnedKey() {
    when(redisTemplate.execute(
            anyScript(),
            eq(List.of(KEY)),
            eq(INSTANCE_ID),
            eq(String.valueOf(LEASE_DURATION.toMillis()))))
        .thenReturn(1L);
    when(redisTemplate.execute(anyScript(), eq(List.of(KEY)), eq(INSTANCE_ID)))
        .thenReturn(1L);

    var handle = client.tryAcquire(DEGRADATION_ID, LEASE_DURATION);
    handle.release();

    verify(redisTemplate).execute(anyScript(), eq(List.of(KEY)), eq(INSTANCE_ID));
  }

  private static DefaultRedisScript<Long> anyScript() {
    return any();
  }
}
