package org.janus.sidecar.model.handler;

import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SyncActualDegradationsCommand(Set<String> degradationIds) {}
