package org.janus.adminui.model;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record StateView(
    String degradationId,
    double effectiveValue,
    String effectiveSource,
    List<SourceStateView> sourceStates,
    Instant refreshedAt) {}
