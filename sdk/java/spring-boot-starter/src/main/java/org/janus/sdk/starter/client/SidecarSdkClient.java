package org.janus.sdk.starter.client;

import java.util.List;
import java.util.Set;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SidecarSdkClient {

  void syncActualDegradations(Set<String> degradationIds);

  List<DegradationRuntimeState> getDegradations();
}
