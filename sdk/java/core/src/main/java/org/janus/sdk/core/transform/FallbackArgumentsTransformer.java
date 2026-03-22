package org.janus.sdk.core.transform;

import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.fallback.FallbackDecision;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FallbackArgumentsTransformer {

  Object[] transform(
      DegradableMethodDescriptor descriptor, FallbackDecision decision, Object[] originalArguments);
}
