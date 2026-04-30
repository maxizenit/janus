package org.janus.sdk.core.fallback;

import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface FallbackDecisionService {

  FallbackDecision decide(
      DegradableMethodDescriptor descriptor, @Nullable DegradationRuntimeState runtimeState);
}
