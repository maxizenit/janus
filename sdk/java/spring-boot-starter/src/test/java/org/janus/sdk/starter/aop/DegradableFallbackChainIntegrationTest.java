package org.janus.sdk.starter.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.fallback.DefaultFallbackDecisionService;
import org.janus.sdk.core.fallback.FallbackDecisionService;
import org.janus.sdk.core.fallback.StaleDegradationStrategy;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.core.registry.InMemoryDegradableMethodRegistry;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.core.runtime.InMemoryDegradationStateRegistry;
import org.janus.sdk.core.transform.DefaultFallbackArgumentsTransformer;
import org.janus.sdk.core.transform.FallbackArgumentsTransformer;
import org.janus.sdk.core.validation.DefaultDegradableDescriptorValidator;
import org.janus.sdk.core.validation.DegradableDescriptorValidator;
import org.janus.sdk.core.validation.FallbackCycleDetector;
import org.janus.sdk.starter.registry.RegistryBackedMethodDescriptorResolver;
import org.janus.sdk.starter.scanner.DegradableDescriptorFactory;
import org.janus.sdk.starter.scanner.DegradableMethodScanner;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

class DegradableFallbackChainIntegrationTest {

  @SuppressWarnings("unused")
  static class ChainedService {

    @Degradable(value = "deg.a", fallback = "b")
    public String a(String input) {
      return "a:" + input;
    }

    @Degradable(value = "deg.b", fallback = "c")
    public String b(String input) {
      return "b:" + input;
    }

    public String c(String input) {
      return "c:" + input;
    }
  }

  @SuppressWarnings("unused")
  static class ReactiveChainedService {

    @Degradable(
        value = "rdeg.a",
        fallback = "b",
        fallbackOnException = IOException.class)
    public String a(String input) throws IOException {
      throw new IOException("a failed");
    }

    @Degradable(
        value = "rdeg.b",
        fallback = "c",
        fallbackOnException = IOException.class)
    public String b(String input) throws IOException {
      throw new IOException("b failed");
    }

    public String c(String input) {
      return "c:" + input;
    }
  }

  @SuppressWarnings("unused")
  static class PlainFallbackService {

    @Degradable(value = "deg.plain", fallback = "fallback")
    public String primary(String input) {
      return "primary:" + input;
    }

    public String fallback(String input) {
      return "fallback:" + input;
    }
  }

  @SuppressWarnings("unused")
  static class ThrowingFallbackService {

    @Degradable(value = "deg.throw", fallback = "boom")
    public String primary(String input) {
      return "primary:" + input;
    }

    public String boom(String input) {
      throw new IllegalStateException("fallback exploded");
    }
  }

  @Configuration
  @Import(JanusTestConfig.class)
  static class ThrowingFallbackConfig {
    @Bean
    ThrowingFallbackService throwingFallbackService() {
      return new ThrowingFallbackService();
    }
  }

  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  @Import({
    DegradableAspect.class,
    DegradableMetrics.class,
    ReflectionFallbackMethodInvoker.class,
    RegistryBackedMethodDescriptorResolver.class,
    DegradableMethodScanner.class,
    DegradableDescriptorFactory.class
  })
  static class JanusTestConfig {

    @Bean
    DegradableMethodRegistry degradableMethodRegistry() {
      return new InMemoryDegradableMethodRegistry();
    }

    @Bean
    DegradationStateRegistry degradationStateRegistry() {
      return new InMemoryDegradationStateRegistry();
    }

    @Bean
    FallbackDecisionService fallbackDecisionService() {
      return new DefaultFallbackDecisionService(StaleDegradationStrategy.LAST_VALUE);
    }

    @Bean
    FallbackArgumentsTransformer fallbackArgumentsTransformer() {
      return new DefaultFallbackArgumentsTransformer();
    }

    @Bean
    FallbackCycleDetector fallbackCycleDetector() {
      return new FallbackCycleDetector();
    }

    @Bean
    DegradableDescriptorValidator degradableDescriptorValidator() {
      return new DefaultDegradableDescriptorValidator();
    }

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Configuration
  @Import(JanusTestConfig.class)
  static class ChainedServiceConfig {
    @Bean
    ChainedService chainedService() {
      return new ChainedService();
    }
  }

  @Configuration
  @Import(JanusTestConfig.class)
  static class ReactiveChainedServiceConfig {
    @Bean
    ReactiveChainedService reactiveChainedService() {
      return new ReactiveChainedService();
    }
  }

  @Configuration
  @Import(JanusTestConfig.class)
  static class PlainFallbackServiceConfig {
    @Bean
    PlainFallbackService plainFallbackService() {
      return new PlainFallbackService();
    }
  }

  private static DegradationRuntimeState criticalState(String degradationId) {
    return new DegradationRuntimeState(
        degradationId, 1.0, Duration.ofSeconds(1), 0.0, 1.0, 1.0, 1.0, false, Instant.EPOCH);
  }

  @Test
  void proactiveDegradationOnTopLevelOnly_invokesDirectFallback() {
    try (var context = new AnnotationConfigApplicationContext(ChainedServiceConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();
      context.getBean(DegradationStateRegistry.class)
          .replaceAll(Map.of("deg.a", criticalState("deg.a")));

      var service = context.getBean(ChainedService.class);

      assertThat(service.a("x")).isEqualTo("b:x");
    }
  }

  @Test
  void proactiveDegradationOnTwoLevels_chainsThroughFallbackOfFallback() {
    try (var context = new AnnotationConfigApplicationContext(ChainedServiceConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();
      context.getBean(DegradationStateRegistry.class)
          .replaceAll(
              Map.of(
                  "deg.a", criticalState("deg.a"),
                  "deg.b", criticalState("deg.b")));

      var service = context.getBean(ChainedService.class);

      assertThat(service.a("x")).isEqualTo("c:x");

      var meterRegistry = context.getBean(MeterRegistry.class);
      assertThat(counter(meterRegistry, "deg.a", "none", "fallback", "proactive"))
          .as("top-level proactive fallback for deg.a, caller is none")
          .isEqualTo(1.0);
      assertThat(counter(meterRegistry, "deg.b", "deg.a", "fallback", "proactive"))
          .as("nested proactive fallback for deg.b, caller is deg.a")
          .isEqualTo(1.0);
    }
  }

  @Test
  void metricsCallerTag_isNoneForTopLevelReactiveFallback() throws IOException {
    try (var context =
        new AnnotationConfigApplicationContext(ReactiveChainedServiceConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();

      var service = context.getBean(ReactiveChainedService.class);
      assertThat(service.a("x")).isEqualTo("c:x");

      var meterRegistry = context.getBean(MeterRegistry.class);
      assertThat(counter(meterRegistry, "rdeg.a", "none", "fallback", "reactive"))
          .as("top-level reactive fallback for rdeg.a, caller is none")
          .isEqualTo(1.0);
      assertThat(counter(meterRegistry, "rdeg.b", "rdeg.a", "fallback", "reactive"))
          .as("nested reactive fallback for rdeg.b, caller is rdeg.a")
          .isEqualTo(1.0);
    }
  }

  @Test
  void metrics_recordErrorWhenFallbackPropagatesUnmatched() {
    try (var context = new AnnotationConfigApplicationContext(ThrowingFallbackConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();
      context.getBean(DegradationStateRegistry.class)
          .replaceAll(Map.of("deg.throw", criticalState("deg.throw")));

      var service = context.getBean(ThrowingFallbackService.class);

      assertThatThrownBy(() -> service.primary("x"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("fallback exploded");

      var meterRegistry = context.getBean(MeterRegistry.class);
      assertThat(counter(meterRegistry, "deg.throw", "none", "error", "none"))
          .as("error recorded when fallback throws")
          .isEqualTo(1.0);
      assertThat(counter(meterRegistry, "deg.throw", "none", "fallback", "proactive"))
          .as("no fallback success metric when fallback throws")
          .isZero();
    }
  }

  private static double counter(
      MeterRegistry registry,
      String degradationId,
      String callerDegradationId,
      String outcome,
      String trigger) {
    var counter =
        registry
            .find("janus.degradable.invocations")
            .tag("degradation.id", degradationId)
            .tag("caller.degradation.id", callerDegradationId)
            .tag("outcome", outcome)
            .tag("trigger", trigger)
            .counter();
    return counter == null ? 0.0 : counter.count();
  }

  @Test
  void reactiveFallback_chainsThroughFallbackOfFallback() throws IOException {
    try (var context =
        new AnnotationConfigApplicationContext(ReactiveChainedServiceConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();

      var service = context.getBean(ReactiveChainedService.class);

      assertThat(service.a("x")).isEqualTo("c:x");
    }
  }

  @Test
  void nonDegradableFallbackMethod_stillInvoked() {
    try (var context = new AnnotationConfigApplicationContext(PlainFallbackServiceConfig.class)) {
      context.getBean(DegradableMethodScanner.class).scanAndRegister();
      context.getBean(DegradationStateRegistry.class)
          .replaceAll(Map.of("deg.plain", criticalState("deg.plain")));

      var service = context.getBean(PlainFallbackService.class);

      assertThat(service.primary("x")).isEqualTo("fallback:x");
    }
  }
}
