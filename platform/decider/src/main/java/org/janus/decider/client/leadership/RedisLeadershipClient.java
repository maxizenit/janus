package org.janus.decider.client.leadership;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.decider.configuration.properties.DeciderProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class RedisLeadershipClient implements LeadershipClient {

  private static final String KEY_PREFIX = "janus:decider:leader:";

  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(
          """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
          Long.class);

  private final StringRedisTemplate redisTemplate;
  private final DeciderProperties properties;

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

    return new RedisLeadershipHandle(acquired, () -> releaseIfOwned(key, owner));
  }

  private void releaseIfOwned(String key, String owner) {
    Long released = redisTemplate.execute(RELEASE_SCRIPT, List.of(key), owner);
    if (Long.valueOf(1L).equals(released)) {
      log.debug("Leadership released: key={}, owner={}", key, owner);
    } else {
      log.debug(
          "Leadership release skipped or ownership lost: key={}, owner={}, released={}",
          key,
          owner,
          released);
    }
  }
}
