package org.janus.sdk.starter.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.registry.InMemoryDegradableMethodRegistry;
import org.janus.sdk.core.validation.DefaultDegradableDescriptorValidator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

class DegradableMethodScannerTest {

  // --- helpers ---

  @SuppressWarnings("unused")
  static class EagerService {
    static final AtomicInteger INSTANCES = new AtomicInteger();

    EagerService() {
      INSTANCES.incrementAndGet();
    }

    @Degradable(value = "eager-degradation", fallback = "primaryFallback")
    public String primary(String input) {
      return input;
    }

    public String primaryFallback(String input) {
      return "fallback:" + input;
    }
  }

  @SuppressWarnings("unused")
  static class LazyService {
    static final AtomicInteger INSTANCES = new AtomicInteger();

    LazyService() {
      INSTANCES.incrementAndGet();
    }

    @Degradable(value = "lazy-degradation", fallback = "primaryFallback")
    public String primary(String input) {
      return input;
    }

    public String primaryFallback(String input) {
      return "fallback:" + input;
    }
  }

  @Configuration
  static class EagerOnlyConfig {
    @Bean
    EagerService eagerService() {
      return new EagerService();
    }
  }

  @Configuration
  static class LazyServiceConfig {
    @Bean
    @Lazy
    LazyService lazyService() {
      return new LazyService();
    }
  }

  // --- tests ---

  @Test
  void scanRegistersDegradableMethodFromComponentBean() {
    EagerService.INSTANCES.set(0);
    try (var context = new AnnotationConfigApplicationContext(EagerOnlyConfig.class)) {
      var registry = new InMemoryDegradableMethodRegistry();
      var scanner =
          new DegradableMethodScanner(
              context.getBeanFactory(),
              registry,
              new DegradableDescriptorFactory(),
              new DefaultDegradableDescriptorValidator());

      scanner.scanAndRegister();

      assertThat(registry.getAllDegradationIds()).containsExactly("eager-degradation");
    }
  }

  @Test
  void scanRegistersLazyBeanWithoutInstantiatingIt() {
    LazyService.INSTANCES.set(0);
    try (var context = new AnnotationConfigApplicationContext(LazyServiceConfig.class)) {
      var registry = new InMemoryDegradableMethodRegistry();
      var scanner =
          new DegradableMethodScanner(
              context.getBeanFactory(),
              registry,
              new DegradableDescriptorFactory(),
              new DefaultDegradableDescriptorValidator());

      assertThat(LazyService.INSTANCES.get())
          .as("lazy bean must not be instantiated by application context startup")
          .isZero();

      scanner.scanAndRegister();

      assertThat(registry.getAllDegradationIds()).containsExactly("lazy-degradation");
      assertThat(LazyService.INSTANCES.get())
          .as("scanner must not instantiate lazy beans")
          .isZero();
      assertThat(context.getBeanFactory().containsSingleton("lazyService"))
          .as("scanner must not create the singleton instance for lazy beans")
          .isFalse();
    }
  }

  @Test
  void scanIgnoresBeansWithoutDegradableMethods() {
    try (var context = new AnnotationConfigApplicationContext(NoDegradablesConfig.class)) {
      var registry = new InMemoryDegradableMethodRegistry();
      var scanner =
          new DegradableMethodScanner(
              context.getBeanFactory(),
              registry,
              new DegradableDescriptorFactory(),
              new DefaultDegradableDescriptorValidator());

      scanner.scanAndRegister();

      assertThat(registry.getAllDegradationIds()).isEmpty();
    }
  }

  @Component
  @SuppressWarnings("unused")
  static class PlainComponent {
    public String hello() {
      return "hi";
    }
  }

  @Configuration
  static class NoDegradablesConfig {
    @Bean
    PlainComponent plainComponent() {
      return new PlainComponent();
    }
  }
}
