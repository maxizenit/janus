package org.janus.sidecar.configuration;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.janus.sidecar.configuration.properties.SidecarProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class SidecarConfiguration {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService stateRefreshExecutor(SidecarProperties properties) {
    return new ThreadPoolExecutor(
        properties.stateRefreshThreads(),
        properties.stateRefreshThreads(),
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(properties.stateRefreshQueueCapacity()),
        Thread.ofPlatform().name("sidecar-state-refresh-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
