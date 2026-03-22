package org.janus.sdk.core.validation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.descriptor.ParameterDescriptor;
import org.janus.sdk.core.util.NumericTypes;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultDegradableDescriptorValidator implements DegradableDescriptorValidator {

  @Override
  public void validate(DegradableMethodDescriptor descriptor) {
    validateMethod(descriptor);
    validateFallbackMethod(descriptor);
    validateThresholds(descriptor);
    validateParameters(descriptor);
  }

  private void validateMethod(DegradableMethodDescriptor descriptor) {
    if (descriptor.degradationId().isBlank()) {
      throw new InvalidDegradableDefinitionException(
          "Degradation id must not be blank for method %s"
              .formatted(descriptor.method().toGenericString()));
    }
  }

  private void validateFallbackMethod(DegradableMethodDescriptor descriptor) {
    Method method = descriptor.method();
    Method fallbackMethod = descriptor.fallbackMethod();

    if (method.equals(fallbackMethod)) {
      throw new InvalidDegradableDefinitionException(
          "Fallback method must differ from degradable method: %s"
              .formatted(method.toGenericString()));
    }

    if (method.getParameterCount() != fallbackMethod.getParameterCount()) {
      throw new InvalidDegradableDefinitionException(
          "Fallback method must have the same number of parameters: method=%s, fallback=%s"
              .formatted(method.toGenericString(), fallbackMethod.toGenericString()));
    }

    Class<?>[] methodParameterTypes = method.getParameterTypes();
    Class<?>[] fallbackParameterTypes = fallbackMethod.getParameterTypes();

    for (int i = 0; i < methodParameterTypes.length; i++) {
      if (!methodParameterTypes[i].equals(fallbackParameterTypes[i])) {
        throw new InvalidDegradableDefinitionException(
            "Fallback method parameter types must match exactly: method=%s, fallback=%s, index=%d"
                .formatted(method.toGenericString(), fallbackMethod.toGenericString(), i));
      }
    }

    if (!method.getReturnType().isAssignableFrom(fallbackMethod.getReturnType())) {
      throw new InvalidDegradableDefinitionException(
          "Fallback method return type must be assignable to degradable method return type: method=%s, fallback=%s"
              .formatted(method.toGenericString(), fallbackMethod.toGenericString()));
    }
  }

  private void validateThresholds(DegradableMethodDescriptor descriptor) {
    validateNonNanOrRange(
        descriptor.criticalThreshold(), "criticalThreshold", descriptor.method(), 0.0, 1.0);

    validateNonNanOrRange(
        descriptor.minFallbackRatio(), "minFallbackRatio", descriptor.method(), 0.0, 1.0);

    validateNonNanOrRange(
        descriptor.maxFallbackRatio(), "maxFallbackRatio", descriptor.method(), 0.0, 1.0);

    if (!Double.isNaN(descriptor.minFallbackRatio())
        && !Double.isNaN(descriptor.maxFallbackRatio())
        && descriptor.minFallbackRatio() > descriptor.maxFallbackRatio()) {
      throw new InvalidDegradableDefinitionException(
          "minFallbackRatio must be less than or equal to maxFallbackRatio for method %s"
              .formatted(descriptor.method().toGenericString()));
    }
  }

  private void validateParameters(DegradableMethodDescriptor descriptor) {
    Set<Integer> indexes = new HashSet<>();

    for (ParameterDescriptor parameter : descriptor.parameters()) {
      if (!indexes.add(parameter.index())) {
        throw new InvalidDegradableDefinitionException(
            "Duplicate parameter descriptor index %d for method %s"
                .formatted(parameter.index(), descriptor.method().toGenericString()));
      }

      validateParameter(descriptor, parameter);
    }
  }

  private void validateParameter(
      DegradableMethodDescriptor descriptor, ParameterDescriptor parameter) {
    if (parameter.hasAbsoluteScale() && parameter.hasRelativeScale()) {
      throw new InvalidDegradableDefinitionException(
          "Parameter must not declare both AbsoluteScale and RelativeScale: method=%s, parameterIndex=%d"
              .formatted(descriptor.method().toGenericString(), parameter.index()));
    }

    if (parameter.hasBounds() && !parameter.hasRelativeScale()) {
      throw new InvalidDegradableDefinitionException(
          "Bounds can only be used together with RelativeScale: method=%s, parameterIndex=%d"
              .formatted(descriptor.method().toGenericString(), parameter.index()));
    }

    if (parameter.isScaled() && !NumericTypes.isSupported(parameter.parameterType())) {
      throw new InvalidDegradableDefinitionException(
          "Scale annotations are only supported for numeric parameter types: method=%s, parameterIndex=%d, parameterType=%s"
              .formatted(
                  descriptor.method().toGenericString(),
                  parameter.index(),
                  parameter.parameterType().getName()));
    }

    if (parameter.absoluteScale() != null) {
      if (parameter.absoluteScale().min() > parameter.absoluteScale().max()) {
        throw new InvalidDegradableDefinitionException(
            "AbsoluteScale.min must be less than or equal to max: method=%s, parameterIndex=%d"
                .formatted(descriptor.method().toGenericString(), parameter.index()));
      }
    }

    if (parameter.relativeScale() != null) {
      if (parameter.relativeScale().minFactor() > parameter.relativeScale().maxFactor()) {
        throw new InvalidDegradableDefinitionException(
            "RelativeScale.minFactor must be less than or equal to maxFactor: method=%s, parameterIndex=%d"
                .formatted(descriptor.method().toGenericString(), parameter.index()));
      }
    }

    if (parameter.bounds() != null) {
      if (parameter.bounds().min() > parameter.bounds().max()) {
        throw new InvalidDegradableDefinitionException(
            "Bounds.min must be less than or equal to max: method=%s, parameterIndex=%d"
                .formatted(descriptor.method().toGenericString(), parameter.index()));
      }
    }
  }

  private void validateNonNanOrRange(
      double value, String field, Method method, double min, double max) {
    if (Double.isNaN(value)) {
      return;
    }
    if (value < min || value > max) {
      throw new InvalidDegradableDefinitionException(
          "%s must be in range [%s, %s] for method %s"
              .formatted(field, min, max, method.toGenericString()));
    }
  }
}
