package org.janus.evaluator.client.leadership;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.evaluator.configuration.properties.EvaluatorProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class RedisLeadershipClient implements LeadershipClient {

  private static final String KEY_PREFIX = "janus:evaluator:leader:";

  private final StringRedisTemplate redisTemplate;
  private final EvaluatorProperties properties;

  @Override
  public LeadershipHandle tryAcquire(String degradationId, Duration leaseDuration) {
    var key = KEY_PREFIX + degradationId;
    var owner = properties.instanceId();

    log.debug(
        "Trying to acquire leadership: degradation={}, key={}, owner={}, leaseDuration={}",
        degradationId,
        key,
        owner,
        leaseDuration);

    var acquired =
        Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, owner, leaseDuration));

    log.debug(
        "Leadership acquire result: degradation={}, key={}, owner={}, acquired={}",
        degradationId,
        key,
        owner,
        acquired);

    return new RedisLeadershipHandle(acquired);
  }
}
