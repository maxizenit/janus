package org.janus.sdk.core.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.junit.jupiter.api.Test;

class DefaultFallbackDecisionServiceTest {

  @SuppressWarnings("unused")
  static class SampleService {
    public String doWork(int limit) {
      return "";
    }

    public String doWorkFallback(int limit) {
      return "fallback";
    }
  }

  private final DefaultFallbackDecisionService service =
      new DefaultFallbackDecisionService(StaleDegradationStrategy.LAST_VALUE);

  private DegradableMethodDescriptor descriptor() {
    try {
      Method method = SampleService.class.getDeclaredMethod("doWork", int.class);
      Method fallback = SampleService.class.getDeclaredMethod("doWorkFallback", int.class);
      return new DegradableMethodDescriptor(
          "test-degradation",
          method,
          fallback,
          SampleService.class,
          List.of(),
          List.of());
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private DegradationRuntimeState state(
      double value,
      double criticalThreshold,
      double minRatio,
      double maxRatio,
      double exponent,
      boolean stale) {
    return new DegradationRuntimeState(
        "test-degradation",
        value,
        Duration.ofSeconds(30),
        criticalThreshold,
        minRatio,
        maxRatio,
        exponent,
        stale,
        Instant.now());
  }

  @Test
  void nullRuntimeState_returnsNoFallback() {
    DegradableMethodDescriptor desc = descriptor();
    FallbackDecision decision = service.decide(desc, null);

    assertThat(decision.fallbackRequired()).isFalse();
    assertThat(decision.degradationValue()).isEqualTo(0.0);
  }

  @Test
  void degradationValueBelowCriticalThreshold_returnsNoFallback() {
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(0.3, 0.5, 0.1, 1.0, 1.0, false);

    FallbackDecision decision = service.decide(desc, rts);

    assertThat(decision.fallbackRequired()).isFalse();
    assertThat(decision.fallbackRatio()).isEqualTo(0.0);
  }

  @Test
  void degradationValueAt1WithMaxRatio1_alwaysTriggersFallback() {
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(1.0, 0.5, 0.0, 1.0, 1.0, false);

    // With value=1.0, threshold=0.5, exponent=1.0: normalized = (1.0-0.5)/(1.0-0.5) = 1.0
    // fallbackRatio = 0.0 + 1.0 * (1.0-0.0) = 1.0
    // ThreadLocalRandom.nextDouble() is in [0,1), so always < 1.0
    for (int i = 0; i < 100; i++) {
      FallbackDecision decision = service.decide(desc, rts);
      assertThat(decision.fallbackRequired()).isTrue();
      assertThat(decision.fallbackRatio()).isEqualTo(1.0);
    }
  }

  @Test
  void runtimeStateValuesAreUsed() {
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(0.8, 0.5, 0.1, 0.9, 1.0, false);

    FallbackDecision decision = service.decide(desc, rts);

    assertThat(decision.effectiveCriticalThreshold()).isEqualTo(0.5);
    assertThat(decision.effectiveMinFallbackRatio()).isEqualTo(0.1);
    assertThat(decision.effectiveMaxFallbackRatio()).isEqualTo(0.9);
  }

  @Test
  void linearInterpolation_atThreshold_minRatio() {
    // When value == criticalThreshold, normalized=0, so ratio = minRatio
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(0.5, 0.5, 0.2, 1.0, 1.0, false);

    FallbackDecision decision = service.decide(desc, rts);

    assertThat(decision.fallbackRatio()).isEqualTo(0.2);
  }

  @Test
  void linearInterpolation_atOne_maxRatio() {
    // When value=1.0, normalized=1.0, so ratio = maxRatio
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(1.0, 0.5, 0.2, 0.8, 1.0, false);

    FallbackDecision decision = service.decide(desc, rts);

    assertThat(decision.fallbackRatio()).isEqualTo(0.8);
  }

  @Test
  void criticalThresholdAtOrAboveOne_returnsMaxRatio() {
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(1.0, 1.0, 0.2, 0.8, 1.0, false);

    FallbackDecision decision = service.decide(desc, rts);

    assertThat(decision.fallbackRatio()).isEqualTo(0.8);
  }

  @Test
  void staleStrategy_failSafe_overridesValueToOne() {
    DefaultFallbackDecisionService failSafeService =
        new DefaultFallbackDecisionService(StaleDegradationStrategy.FAIL_SAFE);
    DegradableMethodDescriptor desc = descriptor();
    // value=0.1 is below threshold but stale + FAIL_SAFE overrides to 1.0
    DegradationRuntimeState rts = state(0.1, 0.5, 0.0, 1.0, 1.0, true);

    FallbackDecision decision = failSafeService.decide(desc, rts);

    assertThat(decision.degradationValue()).isEqualTo(1.0);
    assertThat(decision.fallbackRatio()).isEqualTo(1.0);
    assertThat(decision.fallbackRequired()).isTrue();
  }

  @Test
  void staleStrategy_failOpen_overridesValueToZero() {
    DefaultFallbackDecisionService failOpenService =
        new DefaultFallbackDecisionService(StaleDegradationStrategy.FAIL_OPEN);
    DegradableMethodDescriptor desc = descriptor();
    // value=0.9 is above threshold but stale + FAIL_OPEN overrides to 0.0
    DegradationRuntimeState rts = state(0.9, 0.5, 0.0, 1.0, 1.0, true);

    FallbackDecision decision = failOpenService.decide(desc, rts);

    assertThat(decision.degradationValue()).isEqualTo(0.0);
    assertThat(decision.fallbackRatio()).isEqualTo(0.0);
    assertThat(decision.fallbackRequired()).isFalse();
  }

  @Test
  void staleStrategy_lastValue_usesActualValue() {
    DefaultFallbackDecisionService lastValueService =
        new DefaultFallbackDecisionService(StaleDegradationStrategy.LAST_VALUE);
    DegradableMethodDescriptor desc = descriptor();
    DegradationRuntimeState rts = state(0.8, 0.5, 0.0, 1.0, 1.0, true);

    FallbackDecision decision = lastValueService.decide(desc, rts);

    assertThat(decision.degradationValue()).isEqualTo(0.8);
    // normalized = (0.8 - 0.5) / (1.0 - 0.5) = 0.6
    // ratio = 0.0 + 0.6 * (1.0 - 0.0) = 0.6
    assertThat(decision.fallbackRatio()).isCloseTo(0.6, org.assertj.core.data.Offset.offset(1e-9));
  }
}
