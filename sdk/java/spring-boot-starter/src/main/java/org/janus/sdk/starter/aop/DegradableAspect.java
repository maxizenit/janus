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
      return joinPoint.proceed();
    }

    var fallbackArguments =
        argumentsTransformer.transform(descriptor, decision, joinPoint.getArgs());

    log.debug(
        "Fallback selected: degradationId={}, method={}, degradationValue={}, threshold={}",
        descriptor.degradationId(),
        descriptor.method().toGenericString(),
        decision.degradationValue(),
        decision.effectiveCriticalThreshold());

    return fallbackMethodInvoker.invoke(target, descriptor.fallbackMethod(), fallbackArguments);
  }
}
