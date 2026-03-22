package org.janus.sdk.core.descriptor;

import java.lang.reflect.Method;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record DegradableMethodDescriptor(
    String degradationId,
    Method method,
    Method fallbackMethod,
    Class<?> beanClass,
    double criticalThreshold,
    double minFallbackRatio,
    double maxFallbackRatio,
    List<ParameterDescriptor> parameters) {}
