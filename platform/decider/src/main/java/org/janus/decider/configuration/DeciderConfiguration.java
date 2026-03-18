package org.janus.decider.configuration;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.janus.decider.configuration.properties.DeciderProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class DeciderConfiguration {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService evaluationExecutor(DeciderProperties properties) {
    return new ThreadPoolExecutor(
        properties.evaluationThreads(),
        properties.evaluationThreads(),
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(properties.evaluationQueueCapacity()),
        Thread.ofPlatform().name("decider-evaluation-", 0).factory(),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
