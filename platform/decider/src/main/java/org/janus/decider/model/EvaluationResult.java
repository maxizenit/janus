package org.janus.decider.model;

import jakarta.validation.constraints.Null;
import java.time.Instant;

@Null
public record EvaluationResult(String degradationId, Instant nextEvaluationAt) {}
