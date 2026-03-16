package org.janus.sidecar.validation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.janus.sidecar.configuration.properties.SidecarProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class ActualDegradationIdsValidator {

  private final SidecarProperties properties;

  public void validate(List<@Nullable String> degradationIds) {
    if (degradationIds.size() > properties.maxSyncDegradationIds()) {
      throw new IllegalArgumentException("Too many degradation ids");
    }

    for (var degradationId : degradationIds) {
      if (degradationId == null || degradationId.isBlank()) {
        throw new IllegalArgumentException("Degradation id must not be blank");
      }
    }
  }
}
