package org.janus.adminui.model;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record StateView(
    String degradationId,
    @Nullable Double effectiveValue,
    @Nullable String effectiveSource,
    List<SourceStateView> sourceStates,
    Instant refreshedAt) {}
