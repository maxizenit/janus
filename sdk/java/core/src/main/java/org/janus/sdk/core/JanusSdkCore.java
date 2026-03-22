package org.janus.sdk.core;

import org.janus.sdk.core.fallback.DefaultFallbackDecisionService;
import org.janus.sdk.core.fallback.FallbackDecisionService;
import org.janus.sdk.core.registry.DegradableMethodRegistry;
import org.janus.sdk.core.registry.InMemoryDegradableMethodRegistry;
import org.janus.sdk.core.runtime.DegradationStateRegistry;
import org.janus.sdk.core.runtime.InMemoryDegradationStateRegistry;
import org.janus.sdk.core.transform.DefaultFallbackArgumentsTransformer;
import org.janus.sdk.core.transform.FallbackArgumentsTransformer;
import org.janus.sdk.core.validation.DefaultDegradableDescriptorValidator;
import org.janus.sdk.core.validation.DegradableDescriptorValidator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class JanusSdkCore {

  private JanusSdkCore() {}

  public static DegradableDescriptorValidator degradableDescriptorValidator() {
    return new DefaultDegradableDescriptorValidator();
  }

  public static DegradableMethodRegistry degradableMethodRegistry() {
    return new InMemoryDegradableMethodRegistry();
  }

  public static DegradationStateRegistry degradationStateRegistry() {
    return new InMemoryDegradationStateRegistry();
  }

  public static FallbackDecisionService fallbackDecisionService() {
    return new DefaultFallbackDecisionService();
  }

  public static FallbackArgumentsTransformer fallbackArgumentsTransformer() {
    return new DefaultFallbackArgumentsTransformer();
  }
}
