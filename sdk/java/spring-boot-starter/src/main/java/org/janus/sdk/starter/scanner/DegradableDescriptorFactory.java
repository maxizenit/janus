package org.janus.sdk.starter.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.annotation.param.AbsoluteScale;
import org.janus.sdk.annotation.param.RelativeScale;
import org.janus.sdk.core.descriptor.AbsoluteScaleDescriptor;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.descriptor.ParameterDescriptor;
import org.janus.sdk.core.descriptor.RelativeScaleDescriptor;
import org.janus.sdk.core.validation.InvalidDegradableDefinitionException;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class DegradableDescriptorFactory {

  public DegradableMethodDescriptor create(Class<?> targetClass, Method method) {
    var degradable = AnnotatedElementUtils.findMergedAnnotation(method, Degradable.class);
    if (degradable == null) {
      throw new InvalidDegradableDefinitionException(
          "Method is not annotated with @Degradable: " + method.toGenericString());
    }

    var fallbackMethod = resolveFallbackMethod(targetClass, method, degradable.fallback());
    var parameters = createParameterDescriptors(method);

    return new DegradableMethodDescriptor(
        degradable.value(),
        method,
        fallbackMethod,
        targetClass,
        List.copyOf(parameters));
  }

  private Method resolveFallbackMethod(Class<?> targetClass, Method method, String fallbackName) {
    if (fallbackName == null || fallbackName.isBlank()) {
      throw new InvalidDegradableDefinitionException(
          "Fallback method name must not be blank: " + method.toGenericString());
    }

    try {
      return targetClass.getDeclaredMethod(fallbackName, method.getParameterTypes());
    } catch (NoSuchMethodException e) {
      throw new InvalidDegradableDefinitionException(
          "Fallback method not found: class=%s, fallback=%s, sourceMethod=%s"
              .formatted(targetClass.getName(), fallbackName, method.toGenericString()));
    }
  }

  private List<ParameterDescriptor> createParameterDescriptors(Method method) {
    var parameters = method.getParameters();
    var result = new ArrayList<ParameterDescriptor>(parameters.length);

    for (int i = 0; i < parameters.length; i++) {
      result.add(createParameterDescriptor(i, parameters[i]));
    }

    return result;
  }

  private ParameterDescriptor createParameterDescriptor(int index, Parameter parameter) {
    var absoluteScale = parameter.getAnnotation(AbsoluteScale.class);
    var relativeScale = parameter.getAnnotation(RelativeScale.class);

    return new ParameterDescriptor(
        index,
        parameter.getType(),
        absoluteScale != null
            ? new AbsoluteScaleDescriptor(
                absoluteScale.min(), absoluteScale.max(), absoluteScale.direction())
            : null,
        relativeScale != null
            ? new RelativeScaleDescriptor(
                relativeScale.minFactor(),
                relativeScale.maxFactor(),
                relativeScale.direction(),
                relativeScale.min(),
                relativeScale.max())
            : null);
  }
}
