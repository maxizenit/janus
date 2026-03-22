package org.janus.sdk.core.validation;

import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DegradableDescriptorValidator {

  void validate(DegradableMethodDescriptor descriptor);
}
