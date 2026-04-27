package org.janus.sdk.core.descriptor;

import java.lang.reflect.Method;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record DegradableMethodDescriptor(
    String degradationId,
    Method method,
    @Nullable Method fallbackMethod,
    Class<?> beanClass,
    List<ParameterDescriptor> parameters) {}
