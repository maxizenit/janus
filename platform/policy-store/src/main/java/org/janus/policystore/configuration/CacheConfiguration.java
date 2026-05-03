package org.janus.policystore.configuration;

import lombok.extern.slf4j.Slf4j;
import org.janus.policystore.configuration.properties.PolicyStoreCacheProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
@NullMarked
public class CacheConfiguration implements CachingConfigurer {

  public static final String POLICIES_BY_IDS_CACHE = "degradationPoliciesByIds";
  public static final String POLICIES_ALL_CACHE = "degradationPoliciesAll";

  @Override
  public CacheErrorHandler errorHandler() {
    return new SilentCacheErrorHandler();
  }

  @Bean
  public RedisCacheManager cacheManager(
      RedisConnectionFactory connectionFactory, PolicyStoreCacheProperties properties) {
    var typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .allowIfSubType((ctx, clazz) -> true)
            .build();
    var serializer =
        GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator)
            .enableSpringCacheNullValueSupport()
            .build();

    var config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(properties.ttl())
            .serializeValuesWith(SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
  }

  @Slf4j
  @NullMarked
  static class SilentCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(
        RuntimeException exception, Cache cache, @Nullable Object key) {
      log.warn(
          "Cache GET failed, falling back to source: cache={}, key={}",
          cache.getName(),
          key,
          exception);
    }

    @Override
    public void handleCachePutError(
        RuntimeException exception, Cache cache, @Nullable Object key, @Nullable Object value) {
      log.warn("Cache PUT failed: cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(
        RuntimeException exception, Cache cache, @Nullable Object key) {
      log.warn("Cache EVICT failed: cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
      log.warn("Cache CLEAR failed: cache={}", cache.getName(), exception);
    }
  }
}
