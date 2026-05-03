package org.janus.evaluator.configuration;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.janus.evaluator.configuration.properties.EvaluatorProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class EvaluatorConfiguration {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService evaluationExecutor(EvaluatorProperties properties) {
    return new ThreadPoolExecutor(
        properties.evaluationThreads(),
        properties.evaluationThreads(),
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(properties.evaluationQueueCapacity()),
        Thread.ofPlatform().name("evaluator-evaluation-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
