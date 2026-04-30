package org.janus.sidecar.model;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record StateRefreshResult(String degradationId, Instant nextRefreshAt) {}
