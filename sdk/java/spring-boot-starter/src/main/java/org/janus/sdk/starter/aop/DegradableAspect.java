package org.janus.sdk.starter.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.core.fallback.FallbackDecisionService;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.core.transform.FallbackArgumentsTransformer;
import org.janus.sdk.starter.registry.MethodDescriptorResolver;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradableAspect {

  private final MethodDescriptorResolver descriptorResolver;
  private final DegradationStateRegistry stateRegistry;
  private final FallbackDecisionService decisionService;
  private final FallbackArgumentsTransformer argumentsTransformer;
  private final FallbackMethodInvoker fallbackMethodInvoker;
  private final DegradableMetrics metrics;

  @Around("@annotation(degradable)")
  public Object around(ProceedingJoinPoint joinPoint, Degradable degradable) throws Throwable {
    var signature = (MethodSignature) joinPoint.getSignature();
    var method = signature.getMethod();
    var target = joinPoint.getTarget();
    var targetClass = AopProxyUtils.ultimateTargetClass(target);

    var descriptor =
        descriptorResolver
            .resolve(method, targetClass)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Descriptor not found for degradable method: " + method.toGenericString()));

    var state = stateRegistry.find(descriptor.degradationId()).orElse(null);
    var decision = decisionService.decide(descriptor, state);

    if (!decision.fallbackRequired()) {
      metrics.recordInvocation(descriptor.degradationId(), false);
      return joinPoint.proceed();
    }

    metrics.recordInvocation(descriptor.degradationId(), true);

    var fallbackMethod = descriptor.fallbackMethod();
    if (fallbackMethod == null) {
      log.debug(
          "Fallback selected without fallback method, skipping invocation: degradationId={}, method={}, degradationValue={}, threshold={}",
          descriptor.degradationId(),
          descriptor.method().toGenericString(),
          decision.degradationValue(),
          decision.effectiveCriticalThreshold());
      return defaultReturnValue(method.getReturnType());
    }

    var fallbackArguments =
        argumentsTransformer.transform(descriptor, decision, joinPoint.getArgs());

    log.debug(
        "Fallback selected: degradationId={}, method={}, degradationValue={}, threshold={}",
        descriptor.degradationId(),
        descriptor.method().toGenericString(),
        decision.degradationValue(),
        decision.effectiveCriticalThreshold());

    return fallbackMethodInvoker.invoke(target, fallbackMethod, fallbackArguments);
  }

  private static @Nullable Object defaultReturnValue(Class<?> returnType) {
    if (!returnType.isPrimitive() || void.class.equals(returnType)) {
      return null;
    }
    if (boolean.class.equals(returnType)) {
      return false;
    }
    if (char.class.equals(returnType)) {
      return '\0';
    }
    if (byte.class.equals(returnType)) {
      return (byte) 0;
    }
    if (short.class.equals(returnType)) {
      return (short) 0;
    }
    if (int.class.equals(returnType)) {
      return 0;
    }
    if (long.class.equals(returnType)) {
      return 0L;
    }
    if (float.class.equals(returnType)) {
      return 0.0f;
    }
    if (double.class.equals(returnType)) {
      return 0.0d;
    }
    throw new IllegalArgumentException("Unsupported primitive return type: " + returnType);
  }
}
