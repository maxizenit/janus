package org.janus.sidecar.model.handler;

import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SyncActualDegradationsResult(
    Set<String> addedIds, Set<String> removedIds, Set<String> retainedIds) {}
