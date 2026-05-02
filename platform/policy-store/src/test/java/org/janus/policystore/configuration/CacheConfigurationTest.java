package org.janus.policystore.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.janus.policystore.configuration.CacheConfiguration.SilentCacheErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class CacheConfigurationTest {

  private final SilentCacheErrorHandler handler = new SilentCacheErrorHandler();
  private final Cache cache = mock(Cache.class);

  @Test
  void handleCacheGetError_doesNotRethrow() {
    assertThatCode(
            () -> handler.handleCacheGetError(new RuntimeException("redis down"), cache, "key"))
        .doesNotThrowAnyException();
  }

  @Test
  void handleCachePutError_doesNotRethrow() {
    assertThatCode(
            () ->
                handler.handleCachePutError(
                    new RuntimeException("redis down"), cache, "key", "value"))
        .doesNotThrowAnyException();
  }

  @Test
  void handleCacheEvictError_doesNotRethrow() {
    assertThatCode(
            () -> handler.handleCacheEvictError(new RuntimeException("redis down"), cache, "key"))
        .doesNotThrowAnyException();
  }

  @Test
  void handleCacheClearError_doesNotRethrow() {
    assertThatCode(() -> handler.handleCacheClearError(new RuntimeException("redis down"), cache))
        .doesNotThrowAnyException();
  }
}
