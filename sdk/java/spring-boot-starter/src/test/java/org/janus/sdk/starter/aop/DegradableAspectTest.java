package org.janus.sdk.starter.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.fallback.FallbackDecision;
import org.janus.sdk.core.fallback.FallbackDecisionService;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.core.transform.FallbackArgumentsTransformer;
import org.janus.sdk.starter.registry.MethodDescriptorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegradableAspectTest {

  @Mock private MethodDescriptorResolver descriptorResolver;
  @Mock private DegradationStateRegistry stateRegistry;
  @Mock private FallbackDecisionService decisionService;
  @Mock private FallbackArgumentsTransformer argumentsTransformer;
  @Mock private FallbackMethodInvoker fallbackMethodInvoker;
  @Mock private DegradableMetrics metrics;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private MethodSignature signature;
  @Mock private Degradable degradable;

  @SuppressWarnings("unused")
  static class TargetService {

    @Degradable("object")
    public String objectMethod() {
      return "normal";
    }

    @Degradable("primitive")
    public int primitiveMethod() {
      return 42;
    }
  }

  @Test
  void noFallbackMethod_skipsInvocationAndReturnsNullForReferenceReturnType() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("objectMethod");
    var aspect = aspect();

    var result = aspect.around(joinPointFor(method), degradable);

    assertThat(result).isNull();
    verify(joinPoint, never()).proceed();
    verifyNoInteractions(argumentsTransformer, fallbackMethodInvoker);
    verify(metrics).recordInvocation("deg", true);
  }

  @Test
  void noFallbackMethod_skipsInvocationAndReturnsZeroForPrimitiveReturnType() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("primitiveMethod");
    var aspect = aspect();

    var result = aspect.around(joinPointFor(method), degradable);

    assertThat(result).isEqualTo(0);
    verify(joinPoint, never()).proceed();
    verifyNoInteractions(argumentsTransformer, fallbackMethodInvoker);
    verify(metrics).recordInvocation("deg", true);
  }

  private DegradableAspect aspect() {
    return new DegradableAspect(
        descriptorResolver,
        stateRegistry,
        decisionService,
        argumentsTransformer,
        fallbackMethodInvoker,
        metrics);
  }

  private ProceedingJoinPoint joinPointFor(Method method) {
    var target = new TargetService();
    var descriptor =
        new DegradableMethodDescriptor(
            "deg",
            method,
            null,
            TargetService.class,
            List.of());
    var decision = new FallbackDecision(true, 0.8, 0.5, 0.1, 1.0, 0.7);

    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(target);
    when(descriptorResolver.resolve(eq(method), eq(TargetService.class)))
        .thenReturn(Optional.of(descriptor));
    when(stateRegistry.find("deg")).thenReturn(Optional.empty());
    when(decisionService.decide(descriptor, null)).thenReturn(decision);

    return joinPoint;
  }
}
