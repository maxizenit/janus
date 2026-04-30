package org.janus.evaluator.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.evaluator")
@Validated
public record EvaluatorProperties(
    @NotBlank String instanceId,
    @NotNull @DurationMin(millis = 1) Duration policyRefreshInterval,
    @NotNull @DurationMin(millis = 1) Duration leadershipLeaseDuration,
    @NotNull @DurationMin(millis = 1) Duration leadershipRetryBackoff,
    @NotNull @DurationMin(millis = 1) Duration evaluationFailureBackoff,
    @Positive int evaluationThreads,
    @Positive int evaluationQueueCapacity) {}
