package org.janus.evaluator.model;

import jakarta.validation.constraints.Null;
import java.time.Instant;

@Null
public record EvaluationResult(String degradationId, Instant nextEvaluationAt) {}
