package org.janus.sdk.starter.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.junit.jupiter.api.AfterEach;
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

    @Degradable("reactive")
    public String reactiveMethod() {
      return "normal";
    }

    public String reactiveFallback() {
      return "reactive-fallback";
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
    verify(metrics).recordProactiveFallback("deg", "none");
  }

  @Test
  void noFallbackMethod_skipsInvocationAndReturnsZeroForPrimitiveReturnType() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("primitiveMethod");
    var aspect = aspect();

    var result = aspect.around(joinPointFor(method), degradable);

    assertThat(result).isEqualTo(0);
    verify(joinPoint, never()).proceed();
    verifyNoInteractions(argumentsTransformer, fallbackMethodInvoker);
    verify(metrics).recordProactiveFallback("deg", "none");
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
            "deg", method, null, TargetService.class, List.of(), List.of());
    var decision = new FallbackDecision(true, 0.8, 0.5, 0.1, 1.0, 0.7);

    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(target);
    lenient().when(joinPoint.getThis()).thenReturn(target);
    when(descriptorResolver.resolve(eq(method), eq(TargetService.class)))
        .thenReturn(Optional.of(descriptor));
    when(stateRegistry.find("deg")).thenReturn(Optional.empty());
    when(decisionService.decide(descriptor, null)).thenReturn(decision);

    return joinPoint;
  }

  @Test
  void reactiveFallback_invokedWhenPrimaryThrowsMatchingException() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method,
            fallback,
            List.of(IOException.class),
            new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0));
    var thrown = new java.net.SocketTimeoutException("server slow");
    when(joinPoint.proceed()).thenThrow(thrown);
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(fallbackMethodInvoker.invoke(any(), eq(fallback), any())).thenReturn("reactive-fallback");

    var result = aspect.around(joinPoint, degradable);

    assertThat(result).isEqualTo("reactive-fallback");
    verify(metrics).recordReactiveFallback("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
    verify(metrics, never()).recordNormal("deg", "none");
    verifyNoInteractions(argumentsTransformer);
  }

  @Test
  void reactiveFallback_propagatesUnmatchedException() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method,
            fallback,
            List.of(IOException.class),
            new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0));
    var thrown = new IllegalStateException("not network");
    when(joinPoint.proceed()).thenThrow(thrown);

    assertThatThrownBy(() -> aspect.around(joinPoint, degradable)).isSameAs(thrown);

    verify(metrics).recordError("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
    verify(metrics, never()).recordReactiveFallback("deg", "none");
    verify(metrics, never()).recordNormal("deg", "none");
    verifyNoInteractions(fallbackMethodInvoker);
  }

  @Test
  void reactiveFallback_noFallbackMethod_propagatesMatchingException() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var aspect =
        configureWith(
            method,
            null,
            List.of(IOException.class),
            new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0));
    var thrown = new java.net.SocketTimeoutException("server slow");
    when(joinPoint.proceed()).thenThrow(thrown);

    assertThatThrownBy(() -> aspect.around(joinPoint, degradable)).isSameAs(thrown);

    verify(metrics).recordError("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
    verify(metrics, never()).recordReactiveFallback("deg", "none");
    verify(metrics, never()).recordNormal("deg", "none");
    verifyNoInteractions(fallbackMethodInvoker);
  }

  @Test
  void proactiveFallback_invokesFallbackOnProxyNotRawTarget() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var rawTarget = new TargetService();
    var proxy = new TargetService();
    var descriptor =
        new DegradableMethodDescriptor(
            "deg", method, fallback, TargetService.class, List.of(), List.of());
    var decision = new FallbackDecision(true, 0.9, 0.5, 0.1, 1.0, 0.8);

    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(rawTarget);
    when(joinPoint.getThis()).thenReturn(proxy);
    when(descriptorResolver.resolve(eq(method), eq(TargetService.class)))
        .thenReturn(Optional.of(descriptor));
    when(stateRegistry.find("deg")).thenReturn(Optional.empty());
    when(decisionService.decide(descriptor, null)).thenReturn(decision);
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(argumentsTransformer.transform(eq(descriptor), eq(decision), any()))
        .thenReturn(new Object[0]);
    when(fallbackMethodInvoker.invoke(same(proxy), eq(fallback), any()))
        .thenReturn("proactive-fallback");

    var result = aspect().around(joinPoint, degradable);

    assertThat(result).isEqualTo("proactive-fallback");
    verify(fallbackMethodInvoker).invoke(same(proxy), eq(fallback), any());
    verify(fallbackMethodInvoker, never()).invoke(same(rawTarget), eq(fallback), any());
    verify(metrics).recordProactiveFallback("deg", "none");
  }

  @Test
  void reactiveFallback_invokesFallbackOnProxyNotRawTarget() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var rawTarget = new TargetService();
    var proxy = new TargetService();
    var descriptor =
        new DegradableMethodDescriptor(
            "deg", method, fallback, TargetService.class, List.of(), List.of(IOException.class));
    var decision = new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0);

    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(rawTarget);
    when(joinPoint.getThis()).thenReturn(proxy);
    when(descriptorResolver.resolve(eq(method), eq(TargetService.class)))
        .thenReturn(Optional.of(descriptor));
    when(stateRegistry.find("deg")).thenReturn(Optional.empty());
    when(decisionService.decide(descriptor, null)).thenReturn(decision);
    when(joinPoint.proceed()).thenThrow(new java.net.SocketTimeoutException("server slow"));
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(fallbackMethodInvoker.invoke(same(proxy), eq(fallback), any()))
        .thenReturn("reactive-fallback");

    var result = aspect().around(joinPoint, degradable);

    assertThat(result).isEqualTo("reactive-fallback");
    verify(fallbackMethodInvoker).invoke(same(proxy), eq(fallback), any());
    verify(fallbackMethodInvoker, never()).invoke(same(rawTarget), eq(fallback), any());
    verify(metrics).recordReactiveFallback("deg", "none");
  }

  @AfterEach
  void drainCallerContext() {
    // Defensive cleanup: in case a test pushed manually or aspect left state.
    while (!FallbackCallerContext.NONE.equals(FallbackCallerContext.currentOrNone())) {
      FallbackCallerContext.pop();
    }
  }

  @Test
  void proactiveFallback_recordsErrorWhenInvokerThrows() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method, fallback, List.of(), new FallbackDecision(true, 0.9, 0.5, 0.1, 1.0, 0.8));
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(argumentsTransformer.transform(any(), any(), any())).thenReturn(new Object[0]);
    var thrown = new IllegalStateException("fallback exploded");
    when(fallbackMethodInvoker.invoke(any(), eq(fallback), any())).thenThrow(thrown);

    assertThatThrownBy(() -> aspect.around(joinPoint, degradable)).isSameAs(thrown);

    verify(metrics).recordError("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
    verify(metrics, never()).recordReactiveFallback("deg", "none");
  }

  @Test
  void reactiveFallback_recordsErrorWhenFallbackThrows() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method,
            fallback,
            List.of(IOException.class),
            new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0));
    when(joinPoint.proceed()).thenThrow(new java.net.SocketTimeoutException("primary slow"));
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    var thrown = new IllegalStateException("fallback exploded");
    when(fallbackMethodInvoker.invoke(any(), eq(fallback), any())).thenThrow(thrown);

    assertThatThrownBy(() -> aspect.around(joinPoint, degradable)).isSameAs(thrown);

    verify(metrics).recordError("deg", "none");
    verify(metrics, never()).recordReactiveFallback("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
  }

  @Test
  void callerDegradationIdTag_isReadFromContext() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method, fallback, List.of(), new FallbackDecision(true, 0.9, 0.5, 0.1, 1.0, 0.8));
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(argumentsTransformer.transform(any(), any(), any())).thenReturn(new Object[0]);
    when(fallbackMethodInvoker.invoke(any(), eq(fallback), any())).thenReturn("ok");

    FallbackCallerContext.push("outer.degradation");
    try {
      var result = aspect.around(joinPoint, degradable);
      assertThat(result).isEqualTo("ok");
    } finally {
      FallbackCallerContext.pop();
    }

    verify(metrics).recordProactiveFallback("deg", "outer.degradation");
  }

  @Test
  void callerContext_popsEvenWhenFallbackThrows() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method, fallback, List.of(), new FallbackDecision(true, 0.9, 0.5, 0.1, 1.0, 0.8));
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(argumentsTransformer.transform(any(), any(), any())).thenReturn(new Object[0]);
    when(fallbackMethodInvoker.invoke(any(), eq(fallback), any()))
        .thenThrow(new IllegalStateException("boom"));

    assertThatThrownBy(() -> aspect.around(joinPoint, degradable))
        .isInstanceOf(IllegalStateException.class);

    assertThat(FallbackCallerContext.currentOrNone()).isEqualTo(FallbackCallerContext.NONE);
  }

  @Test
  void normalPath_recordsNonFallbackInvocation() throws Throwable {
    var method = TargetService.class.getDeclaredMethod("reactiveMethod");
    var fallback = TargetService.class.getDeclaredMethod("reactiveFallback");
    var aspect =
        configureWith(
            method,
            fallback,
            List.of(IOException.class),
            new FallbackDecision(false, 0.0, 0.5, 0.1, 1.0, 0.0));
    when(joinPoint.proceed()).thenReturn("normal");

    var result = aspect.around(joinPoint, degradable);

    assertThat(result).isEqualTo("normal");
    verify(metrics).recordNormal("deg", "none");
    verify(metrics, never()).recordProactiveFallback("deg", "none");
    verify(metrics, never()).recordReactiveFallback("deg", "none");
    verifyNoInteractions(fallbackMethodInvoker, argumentsTransformer);
  }

  private DegradableAspect configureWith(
      Method method,
      Method fallback,
      List<Class<? extends Throwable>> fallbackOnException,
      FallbackDecision decision) {
    var target = new TargetService();
    var descriptor =
        new DegradableMethodDescriptor(
            "deg", method, fallback, TargetService.class, List.of(), fallbackOnException);

    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(target);
    lenient().when(joinPoint.getThis()).thenReturn(target);
    when(descriptorResolver.resolve(eq(method), eq(TargetService.class)))
        .thenReturn(Optional.of(descriptor));
    when(stateRegistry.find("deg")).thenReturn(Optional.empty());
    when(decisionService.decide(descriptor, null)).thenReturn(decision);

    return aspect();
  }

}
